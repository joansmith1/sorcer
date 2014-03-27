/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.rio.rmi;

import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.Resolver;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import org.rioproject.url.artifact.ArtifactURLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.util.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static sorcer.core.SorcerConstants.CODEBASE_SEPARATOR;

/**
 * SORCER class
 * User: prubach
 * Date: 13.03.14
 */
public class SorcerResolvingLoader extends RMIClassLoaderSpi {
    /**
     * A table of artifacts to derived codebases. This improves performance by resolving the classpath once per
     * artifact.
     */
    private final Map<String, Set<String>> artifactToCodebase = new ConcurrentHashMap<String, Set<String>>();
    /**
     * A table of classes to artifact: codebase. This will ensure that if the annotation is requested for a class that
     * has it's classpath resolved from an artifact, that the artifact URL is passed back instead of the resolved
     * (local) classpath.
     */
    private final Map<String, String> classAnnotationMap = new ConcurrentHashMap<String, String>();
    private static final Resolver resolver;
    private static final Logger logger = LoggerFactory.getLogger(SorcerResolvingLoader.class);

    static {
        try {
            resolver = ResolverHelper.getResolver();
        } catch (ResolverException e) {
            throw new RuntimeException(e);
        }
    }

    private static final RMIClassLoaderSpi loader = RMIClassLoader.getDefaultProviderInstance();

    @Override
    public Class<?> loadClass(final String codebase,
                              final String name,
                              final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {
        if (logger.isTraceEnabled()) {
            logger.trace("codebase: {}, name: {}, defaultLoader: {}",
                    codebase, name, defaultLoader == null ? "NULL" : defaultLoader.getClass().getName());
        }
        String resolvedCodebase = resolveCodebase(codebase);
        if (codebase != null && codebase.startsWith("artifact:") && classAnnotationMap.get(name) == null) {
            classAnnotationMap.put(name, codebase);
            logger.trace("class: {}, codebase: {}, size now {}", name, codebase, classAnnotationMap.size());
        }
        logger.trace("Load class {} using codebase {}, resolved to {}", name, codebase, resolvedCodebase);
        return loader.loadClass(resolvedCodebase, name, defaultLoader);
    }

    @Override
    public Class<?> loadProxyClass(final String codebase,
                                   final String[] interfaces,
                                   final ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException {

        logger.trace("codebase: {}, interfaces: {}, defaultLoader: {}", codebase, interfaces, defaultLoader);
        String resolvedCodebase = resolveCodebase(codebase);
        logger.trace("Load proxy classes {} using codebase {}, resolved to {}, defaultLoader: {}",
                interfaces, codebase, resolvedCodebase, defaultLoader);
        return loader.loadProxyClass(resolvedCodebase, interfaces, defaultLoader);
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException {
        if (logger.isTraceEnabled()) {
            logger.trace("codebase: {}", codebase);
        }
        String resolvedCodebase = resolveCodebase(codebase);
        return loader.getClassLoader(resolvedCodebase);
    }

    @Override
    public String getClassAnnotation(final Class<?> aClass) {
        String annotation = classAnnotationMap.get(aClass.getName());
        if (annotation == null)
            annotation = loader.getClassAnnotation(aClass);
        return annotation;
    }

    private String resolveCodebase(final String codebase) {
        if (codebase != null && codebase.startsWith("artifact:")) {
            String[] artifacts = codebase.split(CODEBASE_SEPARATOR);
            Set<String> jarsSet = new HashSet<String>();
            for (String artf : artifacts) {
                Set<String> adaptedCodebase;
                synchronized (artf.intern()) {
                    adaptedCodebase = artifactToCodebase.get(artf);
                    if (adaptedCodebase == null)
                        try {
                            adaptedCodebase = new HashSet<String>();
                            for (String path : doResolve(artf)) {
                                // ignore pom files
                                if(path.endsWith(".pom"))
                                    continue;
                                adaptedCodebase.add(new File(path).toURI().toURL().toExternalForm());
                            }
                            artifactToCodebase.put(artf, adaptedCodebase);
                    } catch (ResolverException e) {
                        logger.warn("Unable to resolve {}", artf, e);
                    } catch (MalformedURLException e) {
                        logger.warn("The codebase {} is malformed", artf, e);
                    }
                }
                jarsSet.addAll(adaptedCodebase);
            }
            return StringUtils.join(jarsSet, CODEBASE_SEPARATOR);
        } else {
            return codebase;
        }
    }

    private String[] doResolve(String artifact) throws ResolverException {
        logger.debug("Resolve {} ", artifact);
        String path = artifact.substring(artifact.indexOf(":") + 1);
        ArtifactURLConfiguration artifactURLConfiguration = new ArtifactURLConfiguration(path);
        for (RemoteRepository rr : artifactURLConfiguration.getRepositories()) {
            rr.setSnapshotChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
            rr.setReleaseChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
        }
        //TODO Resolver error
        String[] cp;
        try {
            cp = resolver.getClassPathFor(artifactURLConfiguration.getArtifact(),
                    artifactURLConfiguration.getRepositories());
        } catch (Exception e) {
            logger.warn("Trying again to resolve: {}", artifactURLConfiguration.getArtifact());
            /*for (RemoteRepository rr : artifactURLConfiguration.getRepositories()) {
                rr.setSnapshotChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
                rr.setReleaseChecksumPolicy(RemoteRepository.CHECKSUM_POLICY_IGNORE);
            }*/
            cp = resolver.getClassPathFor(artifactURLConfiguration.getArtifact(),
                    artifactURLConfiguration.getRepositories());
        }
        return cp;
    }
}
