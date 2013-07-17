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
package sorcer.core;

import com.sun.jini.admin.DestroyAdmin;
import net.jini.admin.Administrable;
import net.jini.admin.JoinAdmin;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AdministratableProvider extends Administrable, JoinAdmin,
		DestroyAdmin, Remote {

	/**
	 * Returns an inner proxy of the provider implementing this interface. The
	 * inner proxy can be provided by the registering provider. This proxy
	 * allows smart proxies to invoke remote methods on anoter types of proxies,
	 * e.g., its admin proxy, or to request a new inner proxy to replace the
	 * failing remote one.
	 * 
	 * @return an inner proxy of this proxy
	 * @throws java.rmi.RemoteException
	 */
	public Remote getInner() throws RemoteException;

}
