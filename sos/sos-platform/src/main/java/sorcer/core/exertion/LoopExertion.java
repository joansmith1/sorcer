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
package sorcer.core.exertion;

import java.rmi.RemoteException;

import net.jini.core.transaction.Transaction;
import sorcer.service.Condition;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.SignatureException;
import sorcer.service.Task;

/**
 * The loop Exertion executes its target exertion while its condition is true.
 * Other types of looping types depend on parameters provided as described for
 * each LoopExertion constructor.
 * 
 * @author Mike Sobolewski
 * 
 * @param <V>
 */
public class LoopExertion extends Task {

	private static final long serialVersionUID = 8538804142085766935L;
	
	private int min = 0;

	private int max = 0;

	protected Condition condition;

	protected Exertion target;

	/**
	 * Loop: while(true) { operand }
	 * 
	 * @param name
	 * @param var
	 */
	public LoopExertion(String name, Exertion exertion) {
		super(name);
		condition = new Condition(true);
		target = exertion;
	}

	/**
	 * Iteration: for i = n to m { operand }
	 * 
	 * @param name
	 * @param min
	 * @param max
	 * @param var
	 */
	public LoopExertion(String name, int min, int max, Exertion exertion) {
		super(name);
		this.min = min;
		this.max = max;
		target = exertion;
	}

	/**
	 * Loop: while (condition) { operand }
	 * 
	 * @param name
	 * @param condition
	 * @param var
	 */
	public LoopExertion(String name, Condition condition, Exertion exertion) {
		super(name);
		this.condition = condition;
		target = exertion;
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
	public LoopExertion(String name, int min, int max, Condition condition,
			Exertion invoker) {
		super(name);
		this.min = min;
		this.max = max;
		this.condition = condition;
		target = invoker;
	}

	@Override
	public Task doTask(Transaction txn) throws ExertionException,
			SignatureException, RemoteException {
		try {
			if (condition == null) {
				for (int i = 0; i < max - min; i++) {
					target = target.exert(txn);
				}
				return this;
			} else if (condition != null && max - min == 0) {
				while (condition.isTrue()) {
					target = target.exert(txn);
				}
			} else if (condition != null && max - min > 0) {
				// execute min times
				for (int i = 0; i < min; i++) {
					target = target.exert(txn);
				}
				for (int i = 0; i < max - min; i++) {
					target = target.exert(txn);
					if (condition.isTrue())
						target = target.exert(txn);
					else
						return this;
				}
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
		return this;
	}
	
	public boolean isConditional() {
		return true;
	}
}
