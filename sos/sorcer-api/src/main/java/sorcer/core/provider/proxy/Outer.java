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
package sorcer.core.provider.proxy;

import net.jini.admin.Administrable;
import sorcer.service.Service;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Smart proxies and composite proxies extending their functionalty via calls on
 * the inner proxies implement this interface.
 * 
 * @author Mike Sobolewski
 */
public interface Outer extends Administrable, Service {

	/**
	 * Returns the inner proxy of this provider. Inner proxies can be provided
	 * by the registering provider of this proxy or by third party providers.
	 * This proxy extends its local functionality by invoking remote methods on
	 * its inner proxy.
	 * 
	 * @return an inner proxy of thos proxy
	 * @throws RemoteException
	 */
	public Remote getInner() throws RemoteException;

}
