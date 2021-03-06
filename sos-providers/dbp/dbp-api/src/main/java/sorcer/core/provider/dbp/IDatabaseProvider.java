package sorcer.core.provider.dbp;
/**
 *
 * Copyright 2013 Rafał Krupiński.
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


import net.jini.id.Uuid;
import sorcer.core.provider.DatabaseStorer;

import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Rafał Krupiński
 */
public interface IDatabaseProvider extends Remote {
    URL storeObject(Object object) throws RemoteException;

    void updateObject(URL url, Object object) throws RemoteException, InvalidObjectException;

    void deleteObject(URL url) throws RemoteException;

    Object retrieve(URL url) throws RemoteException;

    Uuid store(Object object);

    URL getDatabaseURL(DatabaseStorer.Store storeType, Uuid uuid) throws MalformedURLException;
}
