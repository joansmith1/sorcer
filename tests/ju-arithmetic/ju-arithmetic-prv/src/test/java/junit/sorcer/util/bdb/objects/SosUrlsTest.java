/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 Sorcersoft.com S.A.
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
package junit.sorcer.util.bdb.objects;

import static sorcer.eo.operator.dbURL;
import static sorcer.eo.operator.sosURL;
import static sorcer.eo.operator.value;
import static sorcer.eo.operator.set;

import java.io.IOException;
import java.net.URL;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import org.junit.Assert;

import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.util.bdb.SosURL;
import sorcer.util.bdb.sdb.Handler;

/**
 * @author Mike Sobolewski
 */
public class SosUrlsTest {

	private final static Logger logger = Logger.getLogger(SosUrlsTest.class
			.getName());

	static {
        Handler.register();
		if (System.getProperty("java.security.policy") == null) {
			System.setProperty("java.security.policy", System.getenv("SORCER_HOME") + "/configs/sorcer.policy");
		}
        System.setSecurityManager(new RMISecurityManager());
		System.out.println("CLASSPATH :"
				+ System.getProperty("java.class.path"));
		ServiceExertion.debug = true;
	}

	public void sosUrlsTest() throws SignatureException,
			ExertionException, ContextException, IOException, InterruptedException {
        storedValuesTest();
        updateValuesTest();
    }

	public void storedValuesTest() throws SignatureException,
			ExertionException, ContextException, IOException, InterruptedException {
		URL url1 = dbURL("Test1");
		URL url2 = dbURL(21.0);
		logger.info("object URL: " + url1);
		logger.info("object URL: " + url2);
		Thread.sleep(1000);
		Assert.assertEquals("Test1", value(url1));
		Assert.assertEquals(21.0, value(url2));
	}

	public void updateValuesTest() throws SignatureException,
			ExertionException, ContextException, IOException {
		SosURL url1 = sosURL("Test1");
		SosURL url2 = sosURL(21.0);

		logger.info("url1: " + url1);
		logger.info("url2: " + url2);
		
		logger.info("x1 value: " + value(url1));
		logger.info("x2 value: " + value(url2));

		set(url1, "Test2");
		logger.info("url1 value: " + value(url1));
		Assert.assertEquals("Test2", value(url1));

		set(url2, 25.0);
		logger.info("url2 value: " + value(url2));
		Assert.assertEquals(25.0, value(url2));
	}

	public static void main(String[]args) throws Exception {
		new SosUrlsTest().sosUrlsTest();
	}
}
