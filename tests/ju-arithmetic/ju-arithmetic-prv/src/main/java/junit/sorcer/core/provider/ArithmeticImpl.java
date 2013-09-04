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
package junit.sorcer.core.provider;

import java.rmi.RemoteException;

import sorcer.service.Context;
import sorcer.service.ContextException;

public class ArithmeticImpl implements Arithmetic {

//public class ArithmeticImpl implements Arithmetic, Adder {
	
	private Arithmometer arithmometer = new Arithmometer();

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.sorcer.core.provider.Adder#add(sorcer.service.Context)
	 */
	@Override
	public Context add(Context context) throws RemoteException,
			ContextException {
		return arithmometer.add(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * junit.sorcer.core.provider.Subtractor#subtract(sorcer.service.Context)
	 */
	@Override
	public Context subtract(Context context) throws RemoteException,
			ContextException {
		return arithmometer.subtract(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * junit.sorcer.core.provider.Multiplier#multiply(sorcer.service.Context)
	 */
	@Override
	public Context multiply(Context context) throws RemoteException,
			ContextException {
		return arithmometer.multiply(context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.sorcer.core.provider.Divider#divide(sorcer.service.Context)
	 */
	@Override
	public Context divide(Context context) throws RemoteException,
			ContextException {
		return arithmometer.divide(context);
	}

}
