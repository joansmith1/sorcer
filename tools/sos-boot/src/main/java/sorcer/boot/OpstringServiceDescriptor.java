package sorcer.boot;
/**
 * Copyright 2013, 2014 Sorcersoft.com S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.sun.jini.config.Config;
import com.sun.jini.start.AggregatePolicyProvider;
import com.sun.jini.start.ClassLoaderUtil;
import com.sun.jini.start.LifeCycle;
import com.sun.jini.start.LoaderSplitPolicyProvider;
import com.sun.jini.start.ServiceProxyAccessor;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.export.ProxyAccessor;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyFileProvider;
import net.jini.security.policy.PolicyInitializationException;
import org.rioproject.impl.opstring.OpStringUtil;
import org.rioproject.loader.ClassAnnotator;
import org.rioproject.loader.ServiceClassLoader;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerEnv;
import sorcer.provider.boot.AbstractServiceDescriptor;
import sorcer.util.SorcerResolverHelper;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Policy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rafał Krupiński
 */
public class OpstringServiceDescriptor extends AbstractServiceDescriptor {
    private static final Logger logger = LoggerFactory.getLogger(OpstringServiceDescriptor.class);

    @Inject
    protected Resolver resolver;

    private ServiceElement serviceElement;
    private URL policyFile;

    private static AggregatePolicyProvider globalPolicy = null;
    private static Policy initialGlobalPolicy = null;

    public OpstringServiceDescriptor(ServiceElement serviceElement, URL policyFile) {
        this.serviceElement = serviceElement;
        this.policyFile = policyFile;
    }

    @Override
    protected Service doCreate(Configuration globalConfig) throws Exception {
        Thread currentThread = Thread.currentThread();
        ClassLoader currentClassLoader = currentThread.getContextClassLoader();
        ClassLoader cl = getClassLoader(serviceElement.getComponentBundle(), serviceElement, currentClassLoader);

        security(cl);

        Class<?> implClass = cl.loadClass(serviceElement.getComponentBundle().getClassName());
        if (logger.isDebugEnabled())
            ClassLoaderUtil.displayClassLoaderTree(cl);
        Object impl;
        Object proxy;
        currentThread.setContextClassLoader(cl);
        try {
            Configuration config = ConfigurationProvider.getInstance(serviceElement.getServiceBeanConfig().getConfigArgs(), cl);
            ProxyPreparer servicePreparer = (ProxyPreparer) Config.getNonNullEntry(
                    config, COMPONENT, "servicePreparer", ProxyPreparer.class,
                    new BasicProxyPreparer());

            Injector injector = parentInjector.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String[].class).toInstance(serviceElement.getServiceBeanConfig().getConfigArgs());
                    bind(LifeCycle.class).toInstance(lifeCycle);
                }
            });

            impl = injector.getInstance(implClass);
            logger.trace("Obtained implementation instance: {}", impl.toString());
            if (impl instanceof ServiceProxyAccessor) {
                proxy = ((ServiceProxyAccessor) impl).getServiceProxy();
            } else if (impl instanceof ProxyAccessor) {
                proxy = ((ProxyAccessor) impl).getProxy();
            } else {
                proxy = null; // just for insurance
            }
            if (proxy != null) {
                proxy = servicePreparer.prepareProxy(proxy);
            }
            logger.trace("Proxy:  {}", proxy == null ? "<NULL>" : proxy.toString());
        } finally {
            currentThread.setContextClassLoader(currentClassLoader);
        }
        return new Service(impl, proxy, this);
    }

    private void security(ClassLoader cl) throws PolicyInitializationException {
        synchronized (OpstringServiceDescriptor.class) {
            if (globalPolicy == null) {
                initialGlobalPolicy = Policy.getPolicy();
                globalPolicy = new AggregatePolicyProvider(initialGlobalPolicy);
                Policy.setPolicy(globalPolicy);
                logger.debug("Global policy set: {}",
                        globalPolicy);
            }

            if (this.policyFile != null) {
                String policyFile = this.policyFile.toExternalForm();
                DynamicPolicyProvider service_policy = new DynamicPolicyProvider(
                        new PolicyFileProvider(policyFile));
                LoaderSplitPolicyProvider splitServicePolicy = new LoaderSplitPolicyProvider(
                        cl, service_policy, new DynamicPolicyProvider(
                        initialGlobalPolicy));
                /*
                 * Grant "this" code enough permission to do its work under the
                 * service policy, which takes effect (below) after the context
                 * loader is (re)set.
                 */
                splitServicePolicy.grant(OpstringServiceDescriptor.class, null,
                        new Permission[]{new AllPermission()});
                globalPolicy.setPolicy(cl, splitServicePolicy);
            } else
                logger.warn("No policy file found in deploy jar");
        }
    }

    private URLClassLoader getClassLoader(ClassBundle bundle, ServiceElement serviceElement, ClassLoader parentCL) throws ResolverException, URISyntaxException, IOException {
        URI[] uris = SorcerResolverHelper.toURIs(resolver.getClassPathFor(bundle.getArtifact(), serviceElement.getRemoteRepositories()));

        URL codebaseRoot = SorcerEnv.getCodebaseRoot();
        OpStringUtil.checkCodebase(serviceElement, codebaseRoot.toExternalForm());

        URL[] codebase = getCodebase(serviceElement);
        return new ServiceClassLoader(uris, new ClassAnnotator(codebase), parentCL);
    }

    private static URL[] getCodebase(ServiceElement serviceElement) throws MalformedURLException, ResolverException, URISyntaxException {
        URL[] exportURLs = serviceElement.getExportURLs();
        if (exportURLs.length != 0)
            return exportURLs;

        ClassBundle[] exportBundles = serviceElement.getExportBundles();
        List<URL> exportUrls = new ArrayList<URL>(exportBundles.length);

        for (ClassBundle exportBundle : exportBundles) {
            URL codebaseRoot = new URL(exportBundle.getCodebase());
            String artifact = exportBundle.getArtifact();
            if (artifact != null)
                exportUrls.addAll(artifactToUrl(codebaseRoot, artifact));
        }
        return exportUrls.toArray(new URL[exportUrls.size()]);
    }

    private static List<URL> artifactToUrl(URL codebase, String artifact) throws MalformedURLException, ResolverException, URISyntaxException {
        List<URL> result = new ArrayList<URL>();
/*
        String urlBase = codebase.toExternalForm();
        String mvnRootFileUrl = new File(SorcerEnv.getRepoDir()).toURI().toString();

        for (String file : resolver.getClassPathFor(artifact)) {
            String replace = new File(file).toURI().toString().replace(mvnRootFileUrl, urlBase);
            logger.debug("{} -> {}", file, replace);
            result.add(new URL(replace));
        }
*/
        artifact = artifact.replace(':', '/');
        result.add(new URL("artifact:" + artifact + ";" + codebase.toExternalForm() + "@" + codebase.getHost()));
        return result;
    }

    @Override
    public String toString() {
        return "OpstringServiceDescriptor " + serviceElement.getOperationalStringName() + "/" + serviceElement.getName();
    }
}
