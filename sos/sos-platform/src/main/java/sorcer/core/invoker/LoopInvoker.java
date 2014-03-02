/*
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.core.invoker;

import java.rmi.RemoteException;

import sorcer.service.Arg;
import sorcer.service.Condition;
import sorcer.service.EvaluationException;

/**
 * The loop Invoker invokes its target while its condition is true. Other types
 * of looping types depend on parameters provided as described for each
 * LoopInvoker constructor.
 * 
 * @author Mike Sobolewski
 * 
 * @param <V>
 */
public class LoopInvoker<V> extends Invoker<V> {

	private int min = 0;

	private int max = 0;

	protected Condition condition;

	protected Invoker<V> target;

	/**
	 * Loop: while(true) { operand }
	 * 
	 * @param name
	 * @param var
	 */
	public LoopInvoker(String name, Invoker<V> invoker) {
		super(name);
		condition = new Condition(true);
		target = invoker;
	}

	/**
	 * Iteration: for i = n to m { operand }
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param var
	 */
	public LoopInvoker(String name, int min, int max, Invoker<V> invoker) {
		super(name);
		this.min = min;
		this.max = max;
		target = invoker;
	}

	/**
	 * Loop: while (condition) { operand }
	 * 
	 * @param name
	 * @param condition
	 * @param var
	 */
	public LoopInvoker(String name, Condition condition, Invoker<V> invoker) {
		super(name);
		this.condition = condition;
		target = invoker;
	}

	/**
	 * The var loop operation is as follows: loop min times, then while
	 * condition is true, loop (max - min) times.
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param condition
	 * @param var
	 */
	public LoopInvoker(String name, int min, int max, Condition condition,
			Invoker<V> invoker) {
		super(name);
		this.min = min;
		this.max = max;
		this.condition = condition;
		target = invoker;
	}

	@Override
	public V getValue(Arg... entries) throws EvaluationException, RemoteException {
		V obj = null;
		try {
			if (condition == null) {
				for (int i = 0; i < max - min; i++) {
					obj = target.getValue(entries);
				}
				return obj;
			} else if (condition != null && max - min == 0) {
				while (condition.isTrue()) {
					obj = target.getValue(entries);
				}
			} else if (condition != null && max - min > 0) {
				// execute min times
				for (int i = 0; i < min; i++) {
					obj = target.getValue(entries);
				}
				for (int i = 0; i < max - min; i++) {
					obj = target.getValue(entries);
					if (condition.isTrue())
						obj = target.getValue(entries);
					else
						return obj;
				}
			}
		} catch (Exception e) {
			throw new EvaluationException(e);
		}
		return obj;
	}
	
}
