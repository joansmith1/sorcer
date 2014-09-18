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
package sorcer.arithmetic.requestor;

import sorcer.arithmetic.provider.Adder;
import sorcer.arithmetic.provider.Multiplier;
import sorcer.arithmetic.provider.Subtractor;
import sorcer.core.context.PositionalContext;
import sorcer.core.exertion.NetJob;
import sorcer.core.exertion.NetTask;
import sorcer.core.signature.NetSignature;
import sorcer.service.Context;
import sorcer.service.Job;
import sorcer.service.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Mike Sobolewski
 */
@SuppressWarnings({ "rawtypes" })
public class NetArithmeticReq {

	private final static Logger logger = LoggerFactory
			.getLogger(NetArithmeticReq.class.getName());

	public static Job createArithmeticJob() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Job internal = new NetJob("2tasks");
		internal.addExertion(task2);
		internal.addExertion(task1);
		
		Job job = new NetJob("1job1task");
		job.addExertion(internal);
		job.addExertion(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/value", "arg1/value", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/value", "arg2/value", task3.getContext());
		
		return job;
	}
	
	public static Job createFlatAritheticJob() throws Exception {
		Task task1 = getAddTask();
		Task task2 = getMultiplyTask();
		Task task3 = getSubtractTask();

		Job job = new NetJob("3tasks");
		job.addExertion(task1);
		job.addExertion(task2);
		job.addExertion(task3);
		
		// make the result of second task as the first argument of task
		// three
		task2.getContext().connect("out/value", "arg1/value", task3.getContext());
		// make the result of the first task as the second argument of task
		// three
		task1.getContext().connect("out/value", "arg2/value", task3.getContext());
		
		return job;
	}

	private static Task getAddTask() throws Exception {
		Context context = new PositionalContext("add");
		context.putInValue("arg1/value", 20.0);
		context.putInValue("arg2/value", 80.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0);
		NetSignature method = new NetSignature("add", Adder.class);
		Task task = new NetTask("add", method);
		task.setContext(context);
		return task;
	}

	private static Task getMultiplyTask() throws Exception {
		Context context = new PositionalContext("multiply");
		context.putInValue("arg1/value", 10.0);
		context.putInValue("arg2/value", 50.0);
		// We know that the output is gonna be placed in this path
		context.putOutValue("out/value", 0);
		NetSignature method = new NetSignature("multiply", Multiplier.class);
		Task task = new NetTask("multiply", method);
		task.setContext(context);
		return task;
	}

	private static Task getSubtractTask() throws Exception {
		PositionalContext context = new PositionalContext("subtract");
		// We want to stick in the result of multiply in here
		context.putInValueAt("arg1/value", 0.0, 1);
		// We want to stick in the result of add in here
		context.putInValueAt("arg2/value", 0.0, 2);
		NetSignature method = new NetSignature("subtract", Subtractor.class);
		Task task = new NetTask("subtract",
				"processing results from two previous tasks", method);
		task.setContext(context);
		return task;
	}
}
