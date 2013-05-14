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
package sorcer.ex4.runner;

import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.requestor.ServiceRequestor;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.Task;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

public class MasterSlaveParallelWorkerRunner extends ServiceRequestor {

	/* (non-Javadoc)
	 * @see sorcer.core.requestor.ExertionRunner#getExertion(java.lang.String[])
	 */
	@Override
	public Exertion getExertion(String... args) 
		throws ExertionException, ContextException, SignatureException {
		String requestorName = getProperty("requestor.name");
		// define requestors data
		Context context1 = new ServiceContext("work1");	
		context1.putValue("requestor/name", requestorName);
		context1.putValue("requestor/operand/1", 1);
		context1.putValue("requestor/operand/2", 1);
		context1.putOutValue("provider/result", 0);
		
		Context context2 = new ServiceContext("work2");
		context2.putValue("requestor/name", requestorName);
		context2.putValue("requestor/operand/1", 2);
		context2.putValue("requestor/operand/2", 2);
		context2.putOutValue("provider/result", 0);

		Context context3 = new ServiceContext("work3");
		context3.putValue("requestor/name", requestorName);
		context3.putInValue("requestor/operand/1", 0);
		context3.putInValue("requestor/operand/2", 0);
		
		// pass the parameters from one dataContext to the next dataContext
		//mapped parameter should be marked via in, out, or inout paths
	    context1.map("provider/result", "requestor/operand/1", context3);
	    context2.map("provider/result", "requestor/operand/2", context3);
	
	    // define required services
	    NetSignature signature1 = new NetSignature("doIt",
				sorcer.ex2.provider.Worker.class);
		NetSignature signature2 = new NetSignature("doIt",
				sorcer.ex2.provider.Worker.class);
		NetSignature signature3 = new NetSignature("doIt",
				sorcer.ex2.provider.Worker.class);

		// define tasks
		Task task1 = new NetTask("work1", signature1, context1);
		Task task2 = new NetTask("work2", signature2, context2);
		Task task3 = new NetTask("work3", signature3, context3);
		
		// define a job
		Job job = new NetJob();
		job.addExertion(task1);
		job.addExertion(task2);
		job.addExertion(task3);
		
		// define a job control strategy
		// use the catalog to delegate the tasks
		job.setAccessType(Access.CATALOG); 
		// either parallel or sequential
		job.setFlowType(Flow.SEQ);
	     // time the job execution
		job.setExecTimeRequested(true); 
		job.setMasterExertion(task3);
	 
		return job;
	}

}