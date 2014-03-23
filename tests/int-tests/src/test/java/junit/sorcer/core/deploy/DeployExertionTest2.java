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

import junit.framework.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.core.provider.Provider;
import sorcer.junit.ExportCodebase;
import sorcer.junit.SorcerClient;
import sorcer.junit.SorcerRunner;
import sorcer.service.*;
import sorcer.util.ProviderLookup;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sorcer.eo.operator.*;

/**
 * @author Dennis Reedy
 */
@RunWith(SorcerRunner.class)
@Category(SorcerClient.class)
@ExportCodebase({"org.sorcersoft.sorcer:sorcer-api",
        "org.sorcersoft.sorcer:ju-arithmetic-api"})
//@SorcerServiceConfiguration(":ex6-cfg-all")
public class DeployExertionTest2 extends DeploySetup implements SorcerConstants {
    private final static Logger logger = Logger.getLogger(DeployExertionTest.class.getName());

    static {
        SorcerEnv.debug = true;
        //Sorcer.setCodeBase(new String[]{"ju-arithmetic-dl.jar", "sorcer-prv-dl.jar"});
    }

    @Test
    public void deployAndExec() throws Exception {
    	long t0 = System.currentTimeMillis();
        Provider provider = null;
        while(provider==null)
            provider = (Provider)ProviderLookup.getService(Provider.class);
		Assert.assertNotNull(provider);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for [Sorcer OS] provisioning");
        
        Job f1 = Util.createJob();
        Assert.assertTrue(f1.isProvisionable());
        verifyExertion(f1);
        /* Run it again to make sure that the existing deployment is used */
        verifyExertion(f1);
    }

    private void verifyExertion(Job job) throws ExertionException, ContextException {
    	long t0 = System.currentTimeMillis();
        Exertion out = exert(job);
        System.out.println("Waited "+(System.currentTimeMillis()-t0)+" millis for exerting: " + out.getName());
        assertNotNull(out);
        logger.info("job f1 context: " + jobContext(out));
        logger.info("job f1/f3/result/y3: " + get(out, "f1/f3/result/y3"));
        assertEquals(get(out, "f1/f3/result/y3"), 400.0);
    }

}
