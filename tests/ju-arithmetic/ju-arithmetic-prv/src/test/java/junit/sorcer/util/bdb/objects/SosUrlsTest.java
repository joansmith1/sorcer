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

import org.junit.Assert;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import static sorcer.eo.operator.dbURL;
import static sorcer.eo.operator.value;

/**
 * @author Mike Sobolewski
 */
public class SosUrlsTest {

	private final static Logger logger = Logger.getLogger(SosUrlsTest.class
			.getName());

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
}
