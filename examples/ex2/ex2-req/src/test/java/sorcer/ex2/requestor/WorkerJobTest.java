/**
 *
 * Copyright 2013 the original author or authors.
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

package sorcer.ex2.requestor;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerEnv;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.junit.*;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Task;

@RunWith(SorcerRunner.class)
@Category(SorcerClient.class)
@SorcerServiceConfiguration({
        ":ex2-cfg1",
        ":ex2-cfg2",
        ":ex2-cfg3"
})
@ExportCodebase({
        "org.sorcersoft.sorcer:sorcer-api",
        "org.sorcersoft.sorcer:ex2-api",
        "org.sorcersoft.sorcer:ex2-rdl"
})
public class WorkerJobTest {

	private static Logger logger = LoggerFactory.getLogger(WorkerJobTest.class);

    @Test
	public void testWorkerJob() throws Exception {

		// get the queried provider name from the command line
		String pn1 = "Worker1";
		String pn2 = "Worker2";
		String pn3 = "Worker3";
		
		logger.info("Provider name1: " + pn1);
		logger.info("Provider name2: " + pn2);
		logger.info("Provider name3: " + pn3);

		Exertion result = new WorkerJobTest()
			.getExertion(pn1, pn2, pn3).exert(null);
		// get contexts of component exertions - in this case tasks
		logger.info("Output context1: \n" + result.getContext("work1"));
		logger.info("Output context2: \n" + result.getContext("work2"));
		logger.info("Output context3: \n" + result.getContext("work3"));
        ExertionErrors.check(result.getExceptions());
	}

	private Exertion getExertion(String pn1, String pn2, String pn3) throws Exception {
        String hostname = SorcerEnv.getHostName();

        if (pn1!=null) pn1 = SorcerEnv.getSuffixedName(pn1);
        if (pn2!=null) pn2 = SorcerEnv.getSuffixedName(pn2);
        if (pn3!=null) pn3 = SorcerEnv.getSuffixedName(pn3);

        Context context1 = new ServiceContext("work1");
        context1.putValue("requstor/name", hostname);
        context1.putValue("requestor/operand/1", 1);
        context1.putValue("requestor/operand/2", 1);
        context1.putValue("to/provider/name", pn1);
        context1.putValue("requestor/work", Works.work1);

        Context context2 = new ServiceContext("work2");
        context2.putValue("requstor/name", hostname);
        context2.putValue("requestor/operand/1", 2);
        context2.putValue("requestor/operand/2", 2);
        context2.putValue("to/provider/name", pn2);
        context2.putValue("requestor/work", Works.work2);

        Context context3 = new ServiceContext("work3");
        context3.putValue("requstor/name", hostname);
        context3.putValue("requestor/operand/1", 3);
        context3.putValue("requestor/operand/2", 3);
        context3.putValue("to/provider/name", pn3);
        context3.putValue("requestor/work", Works.work3);

		NetSignature signature1 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class, pn1);
		NetSignature signature2 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class, pn2);
		NetSignature signature3 = new NetSignature("doWork",
				sorcer.ex2.provider.Worker.class, pn3);
		
		Task task1 = new NetTask("work1", signature1, context1);
		Task task2 = new NetTask("work2", signature2, context2);
		Task task3 = new NetTask("work3", signature3, context3);
		Job job = new NetJob();
		job.addExertion(task1);
		job.addExertion(task2);
		job.addExertion(task3);
		return job;
	}
}
