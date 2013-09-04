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
package sorcer.ex1.requestor;

import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.util.logging.Logger;

import sorcer.core.SorcerEnv;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Exertion;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.Task;
import sorcer.service.Strategy.Flow;
import sorcer.util.Log;
import sorcer.util.Sorcer;

public class WhoIsItPushJobRequestor {

	private static Logger logger = Log.getTestLog();

	public static void main(String... args) throws Exception {
		System.setSecurityManager(new RMISecurityManager());
		// initialize system environment from configs/sorcer.env
		Sorcer.getEnvProperties();
        ServiceRequestor.prepareCodebase();
		// get the queried provider name
		String providerName = SorcerEnv.getSuffixedName(args[0]);

        logger.info("Who is \"" + providerName + "\"?");

		Exertion result = new WhoIsItPushJobRequestor().getExertion(providerName)
				.exert(null);
		logger.info("Job exceptions job: \n" + result.getExceptions());
		logger.info("Output job: \n" + result);
		logger.info("Output context1: \n" + result.getContext("Who Is It1?"));
		logger.info("Output context2: \n" + result.getContext("Who Is It2?"));
	}

	public Exertion getExertion(String providerName) throws Exception {
		String hostname, ipAddress;
		InetAddress inetAddress = InetAddress.getLocalHost();
			hostname = inetAddress.getHostName();
			ipAddress = inetAddress.getHostAddress();

			Context context1 = new ServiceContext("Who Is It?");
		context1.putValue("requestor/message",  
				new RequestorMessage(providerName)); 
			context1.putValue("requestor/hostname", hostname);

			Context context2 = new ServiceContext("Who Is It?");
		context2.putValue("requestor/message", 
				new RequestorMessage(providerName)); 
			context2.putValue("requestor/hostname", hostname);
			context2.putValue("requestor/address", ipAddress);

			NetSignature signature1 = new NetSignature("getHostName",
				sorcer.ex1.WhoIsIt.class, providerName);
			NetSignature signature2 = new NetSignature("getHostAddress",
				sorcer.ex1.WhoIsIt.class, providerName);

			Task task1 = new NetTask("Who Is It1?", signature1, context1);
			Task task2 = new NetTask("Who Is It2?", signature2, context2);
		Job job = new NetJob();
			job.addExertion(task1);
			job.addExertion(task2);
		
		ControlContext cc = job.getControlContext();
		// Exertion control flow PAR or SEQ (default)
		cc.setFlowType(Flow.PAR);

		return job;
	}
}
