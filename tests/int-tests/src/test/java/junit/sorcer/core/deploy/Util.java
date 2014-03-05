/*
 * Copyright to the original author or authors.
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
package junit.sorcer.core.deploy;

import static sorcer.eo.operator.configuration;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.deploy;
import static sorcer.eo.operator.idle;
import static sorcer.eo.operator.input;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.maintain;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.output;
import static sorcer.eo.operator.perNode;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import junit.sorcer.core.provider.Adder;
import junit.sorcer.core.provider.Multiplier;
import junit.sorcer.core.provider.Subtractor;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryEvent;
import net.jini.discovery.DiscoveryListener;

import org.rioproject.impl.client.JiniClient;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;

import sorcer.core.deploy.Deployment;
import sorcer.core.provider.Jobber;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.SignatureException;
import sorcer.service.Strategy.Provision;
import sorcer.service.Task;
import sorcer.util.Sorcer;

/**
 * @author Dennis Reedy & Mike Sobolewski
 */
public class Util {
    public static long MAX_TIMEOUT = 180000;

    static Job createJob() throws ContextException, SignatureException, ExertionException {
        Task f4 = task("f4",
                       sig("multiply",
                           Multiplier.class,
                           deploy(configuration("tests/int-tests/target/test-classes/multiplier-prv.config"),
                                  idle(1),
                                  Deployment.Type.SELF)),
                       context("multiply", input("arg/x1", 10.0d),
                               input("arg/x2", 50.0d), out("result/y1")));

        Task f5 = task("f5",
                       sig("add",
                           Adder.class,
                           deploy(configuration("tests/int-tests/target/test-classes/AdderProviderConfig.groovy"))),
                       context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                               output("result/y2")));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class,
                           deploy(maintain(2, perNode(2)),
                                  idle(1),
                                  configuration("tests/int-tests/target/test-classes/subtractor-prv.config"))),
                       context("subtract", input("arg/x5"),
                               input("arg/x6"), output("result/y3")));

        return job("f1", sig("service", Jobber.class, deploy(idle(1))),
                   job("f2", f4, f5), f3,
                   strategy(Provision.YES),
                   pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
                   pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
    }

    static Task createTaskt() throws SignatureException, ContextException, ExertionException {
    	return task("f5",
    			sig("add", Adder.class,
    					deploy(configuration("tests/int-tests/target/test-classes/AdderProviderConfig.groovy"))),
    				context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
    							output("result/y2")),
    				strategy(Provision.YES));
    }

	
    static Job createJobNoDeployment() throws ContextException, SignatureException, ExertionException {
        Task f4 = task("f4",
                       sig("multiply", Multiplier.class),
                       context("multiply", input("arg/x1", 10.0d),
                               input("arg/x2", 50.0d), out("result/y1", null)));

        Task f5 = task("f5",
                       sig("add", Adder.class),
                       context("add", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                               output("result/y2", null)));

        Task f3 = task("f3",
                       sig("subtract", Subtractor.class),
                       context("subtract", input("arg/x5", null),
                               input("arg/x6", null), output("result/y3", null)));

        return job("f1", job("f2", f4, f5), f3, 
                   pipe(out(f4, "result/y1"), input(f3, "arg/x5")),
                   pipe(out(f5, "result/y2"), input(f3, "arg/x6")));
    }
    
    static void waitForDeployment(OperationalStringManager mgr) throws RemoteException, OperationalStringException, InterruptedException {
        OperationalString opstring  = mgr.getOperationalString();
        Map<ServiceElement, Integer> deploy = new HashMap<ServiceElement, Integer>();
        int total = 0;
        for (ServiceElement elem: opstring.getServices()) {
            deploy.put(elem, 0);
            total += elem.getPlanned();
        }
        int deployed = 0;
        long sleptFor = 0;
        List<String> deployedServices = new ArrayList<String>();
        while (deployed < total && sleptFor< MAX_TIMEOUT) {
            deployed = 0;
            for (Map.Entry<ServiceElement, Integer> entry: deploy.entrySet()) {
                int numDeployed = entry.getValue();
                ServiceElement elem = entry.getKey();
                if (numDeployed < elem.getPlanned()) {
                    numDeployed = mgr.getServiceBeanInstances(elem).length;
                    deploy.put(elem, numDeployed);
                } else {
                    String name = String.format("%s/%s", elem.getOperationalStringName(), elem.getName());
                    if(!deployedServices.contains(name)) {
                        System.out.println(String.format("Service %s/%-24s is deployed. Planned [%s], deployed [%d]",
                                                         elem.getOperationalStringName(),
                                                         elem.getName(),
                                                         elem.getPlanned(),
                                                         numDeployed));
                        deployedServices.add(name);
                    }
                    deployed += elem.getPlanned();
                }
            }
            if(sleptFor==MAX_TIMEOUT)
                break;
            if (deployed < total) {
                Thread.sleep(1000);
                sleptFor += 1000;
            }
        }

        if(sleptFor>=MAX_TIMEOUT && deployed < total)
            throw new RuntimeException("Timeout waiting for service to be deployed");
    }

    @SuppressWarnings("unchecked")
    static <T> T waitForService(Class<T> serviceType) throws Exception{
        return waitForService(serviceType, 60);
    }

    @SuppressWarnings("unchecked")
    static <T> T waitForService(Class<T> serviceType, long timeout) throws Exception{
        int waited = 0;
        JiniClient client = new JiniClient();
        client.addRegistrarGroups(Sorcer.getLookupGroups());
        Listener listener = new Listener(serviceType);
        client.getDiscoveryManager().addDiscoveryListener(listener);

        while(listener.monitor.get()==null && waited < timeout) {
            listener.lookup();
            Thread.sleep(500);
            waited++;
        }
        return (T) listener.monitor.get();
    }
    
    static class Listener implements DiscoveryListener {
        final AtomicReference<Object> monitor = new AtomicReference<Object>();
        private Class<?> serviceType;
        private final List<ServiceRegistrar> lookups = new ArrayList<ServiceRegistrar>();
        
        public Listener(Class<?> providerInterface) {
        	serviceType = providerInterface;
        }

        void lookup() {
            final ServiceTemplate template = new ServiceTemplate(null, new Class[]{serviceType}, null);
            for(ServiceRegistrar registrar : lookups) {
                try {
                    Object service = registrar.lookup(template);
                    if(service!=null) {
                        monitor.set(service);
                        break;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public void discovered(DiscoveryEvent discoveryEvent) {
            Collections.addAll(lookups, discoveryEvent.getRegistrars());
        }

        public void discarded(DiscoveryEvent discoveryEvent) {
        }
    }
    
}
