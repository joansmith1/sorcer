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
package sorcer.service;

import java.rmi.RemoteException;


/**
 * A functionality required by all evaluations in SORCER.
 * 
 * @author Mike Sobolewski
 */
public interface  Evaluation <T> {

	/**
	 * Returns the value of the existing value of this evaluation that might be invalid.
	 * 
	 * @return the value as is
	 * @throws sorcer.service.EvaluationException
	 * @throws java.rmi.RemoteException
	 */
	public T asis() throws EvaluationException, RemoteException;
	
	
	/**
	 * Returns the current value of this evaluation. The current value can be
	 * exiting value with no need to evaluate it if it's still valid.
	 * 
	 * @return the current value of this evaluation
	 * @throws sorcer.service.EvaluationException
	 * @throws java.rmi.RemoteException
	 */
	public T getValue(Parameter... entries) throws EvaluationException, RemoteException;
	
	/**
	 * Realizes the substitution for this evaluation with respect to the provided parameters.
	 * @param entries substitution parameters
	 * @throws sorcer.service.EvaluationException
	 * @throws java.rmi.RemoteException
	 */
	public Evaluation<T> substitute(Parameter... entries) throws EvaluationException, RemoteException;
	
}
