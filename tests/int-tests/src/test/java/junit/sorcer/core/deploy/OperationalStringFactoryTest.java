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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static sorcer.eo.operator.configuration;
import static sorcer.eo.operator.context;
import static sorcer.eo.operator.deploy;
import static sorcer.eo.operator.input;
import static sorcer.eo.operator.job;
import static sorcer.eo.operator.out;
import static sorcer.eo.operator.output;
import static sorcer.eo.operator.pipe;
import static sorcer.eo.operator.sig;
import static sorcer.eo.operator.strategy;
import static sorcer.eo.operator.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.config.Configuration;
import org.rioproject.opstring.OperationalString;
import org.rioproject.opstring.ServiceElement;

import org.rioproject.opstring.UndeployOption;
import sorcer.core.SorcerEnv;
import sorcer.core.deploy.Deployment;
import sorcer.core.deploy.OperationalStringFactory;
import sorcer.service.Job;
import sorcer.service.Service;
import sorcer.service.Strategy;
import sorcer.service.Task;
import sorcer.util.JavaSystemProperties;

/**
 * @author Dennis Reedy
 */
public class OperationalStringFactoryTest {

    @Test
    public void testOperationalStringCreation() throws Exception {
        Job job = Util.createJob();
        Map<Deployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        List<OperationalString> allOperationalStrings = new ArrayList<OperationalString>();
        allOperationalStrings.addAll(deployments.get(Deployment.Unique.YES));
        allOperationalStrings.addAll(deployments.get(Deployment.Unique.NO));
        assertTrue("Expected 2, got " + allOperationalStrings.size(), allOperationalStrings.size() == 2);

        assertTrue(deployments.get(Deployment.Unique.NO).size()==2);

        OperationalString multiply = allOperationalStrings.get(0);
        assertEquals(1, multiply.getServices().length);
        assertEquals("Multiplier", multiply.getServices()[0].getName());
        UndeployOption undeployOption = multiply.getUndeployOption();
        assertNotNull(undeployOption);
        assertTrue(UndeployOption.Type.WHEN_IDLE.equals(multiply.getUndeployOption().getType()));
        assertTrue(1l==undeployOption.getWhen());

        OperationalString federated = allOperationalStrings.get(1);
        String name = job.getDeploymentId();
        assertTrue(name.equals(federated.getName()));
        assertEquals(2, federated.getServices().length);
        assertEquals("Adder", federated.getServices()[0].getName());

        ServiceElement subtract = federated.getServices()[1];
        Assert.assertTrue(subtract.getPlanned()==2);
        Assert.assertTrue(subtract.getMaxPerMachine()==2);
        Assert.assertTrue(subtract.getMachineBoundary() == ServiceElement.MachineBoundary.VIRTUAL);

        assertNotNull(federated.getUndeployOption());
        assertTrue(UndeployOption.Type.WHEN_IDLE.equals(federated.getUndeployOption().getType()));
        assertTrue(1==federated.getUndeployOption().getWhen());

        assertEquals(2, federated.getServices()[1].getPlanned());
    }

    @Test
    public void testServiceProperties() throws Exception {
        Task task = task("f5",
                sig("Foo",
                        Service.class,
                        deploy(configuration("${env.SORCER_HOME}/configs/int-tests/deployment/TestConfig.groovy"))),
                context("foo", input("arg/x3", 20.0d), input("arg/x4", 80.0d),
                        output("result/y2", null)));

        /* totally bogus job definition */
        Job job = job("Some Job", job("f2", task), task, strategy(Strategy.Provision.YES),
                pipe(out(task, "result/y1"), input(task, "arg/x5")),
                pipe(out(task, "result/y2"), input(task, "arg/x6")));
        Map<Deployment.Unique, List<OperationalString>> deployments = OperationalStringFactory.create(job);
        OperationalString operationalString = deployments.get(Deployment.Unique.NO).get(0);
        assertEquals(1, operationalString.getServices().length);
        String name = job.getDeploymentId();
        assertEquals(name, operationalString.getName());
        ServiceElement service = operationalString.getServices()[0];
        assertTrue(service.forkService());
        assertNotNull(service.getExecDescriptor());
        assertEquals("-Xmx4G", service.getExecDescriptor().getInputArgs());
        assertTrue(service.getServiceBeanConfig().getConfigArgs().length==1);
        Configuration configuration = Configuration.getInstance(service.getServiceBeanConfig().getConfigArgs());
        String[] codebaseJars = configuration.getEntry("sorcer.core.exertion.deployment",
                                                       "codebaseJars",
                                                       String[].class);
        assertTrue(codebaseJars.length == 1);
        assertTrue(codebaseJars[0].equals("ju-arithmetic-dl.jar"));
    }

}
