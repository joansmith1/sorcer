/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

package sorcer.core.provider.dbp;

import java.io.File;
import java.io.InvalidObjectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.config.Component;
import sorcer.config.ConfigEntry;
import sorcer.core.provider.Provider;
import sorcer.core.provider.StorageManagement;
import sorcer.service.*;
import sorcer.core.provider.DatabaseStorer;
import sorcer.util.ModelTable;
import sorcer.util.bdb.objects.SorcerDatabase;
import sorcer.util.bdb.objects.SorcerDatabaseViews;
import sorcer.util.bdb.objects.UuidKey;
import sorcer.util.bdb.objects.UuidObject;
import sorcer.util.url.sos.SosDbUtil;

import com.sleepycat.collections.StoredMap;
import com.sleepycat.collections.StoredValueSet;
import com.sleepycat.je.DatabaseException;

import static sorcer.util.StringUtils.tName;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Component
public class DatabaseProvider implements DatabaseStorer, IDatabaseProvider {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProvider.class);

	private SorcerDatabase db;

	private SorcerDatabaseViews views;

    @ConfigEntry("name")
    private String providerName;

    public void init(Provider ignored) throws Exception {
        setupDatabase();
    }

    private Set<Uuid> objectsBeingModified = Collections.synchronizedSet(new HashSet<Uuid>());

	public Uuid store(Object object) {
		Object obj = object;
		if (!(object instanceof Identifiable)) {
			obj = new UuidObject(object);
		}
        PersistThread pt = new PersistThread(obj);
		Uuid id = pt.getUuid();
        pt.start();
        return id;
	}
	
	public Uuid update(Uuid uuid, Object object) throws InvalidObjectException {
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(uuid, object);
		}
		UpdateThread ut = new UpdateThread(uuid, uuidObject);
        Uuid id = ut.getUuid();
        //ut.start();
        ut.run();
        return id;
	}
	
	public Uuid update(URL url, Object object) throws InvalidObjectException {
		Object uuidObject = object;
		if (!(object instanceof Identifiable)) {
			uuidObject = new UuidObject(SosDbUtil.getUuid(url), object);
		}
		UpdateThread ut = new UpdateThread(url, uuidObject);
        Uuid id = ut.getUuid();
        ut.start();
		return id;
	}

    private void addToWaitingList(Uuid id) {
        waitWhileObjectIsModified(id);
        objectsBeingModified.add(id);
    }

    private void waitWhileObjectIsModified(Uuid uuid) {
        while (objectsBeingModified.contains(uuid)) {
            try {
                logger.debug("waiting for uuid: " + uuid);
                Thread.sleep(25);
            } catch (InterruptedException ie) {
                logger.error("Interrupted in getObject while waiting for object to be modified: " + uuid);
            }
        }
    }

    private synchronized Set<Uuid> getAllObjectsBeingModified(){
        return new HashSet<Uuid>(objectsBeingModified);
    }

    public void waitWhileObjectsAreModified() {
        Set<Uuid> tmpObjectList = getAllObjectsBeingModified();
        logger.debug("Init tmpObjList size: " + tmpObjectList.size());
        while (!tmpObjectList.isEmpty()) {
            try {
                tmpObjectList.retainAll(objectsBeingModified);
                logger.debug("tmpObjList size: " + tmpObjectList.size());
                Thread.sleep(30);
            } catch (InterruptedException ie) {
                logger.error("Interrupted in getObject while waiting for objects to be modified");
            }
        }
    }

    public Object getObject(Uuid uuid) {
        waitWhileObjectIsModified(uuid);
        logger.debug("Getting object: " + uuid);
        StoredMap<UuidKey, UuidObject> uuidObjectMap = views.getUuidObjectMap();
        int tries = 0;
        while (uuidObjectMap==null && tries<20) {
            logger.debug("Didn't get UuidObjectMap, trying: " + tries);
            try {
                Thread.sleep(25);
            } catch (InterruptedException ie) {
            }
            views.getUuidObjectMap();
            tries++;
        }
		UuidObject uuidObj = uuidObjectMap.get(new UuidKey(uuid));
        /*int tries = 0;
        while (uuidObj==null && tries<20) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException ie) {
            }
            uuidObj = uuidObjectMap.get(new UuidKey(uuid));
            tries++;
        }*/
		return (uuidObj!=null ? uuidObj.getObject() : null);
	}
	
	public Context getContext(Uuid uuid) {
        try {
            addToWaitingList(uuid);
            StoredMap<UuidKey, Context> cxtMap = views.getContextMap();
            return cxtMap.get(new UuidKey(uuid));
        } finally {
            objectsBeingModified.remove(uuid);
        }
	}
	
	public Exertion getExertion(Uuid uuid) {
        try {
            addToWaitingList(uuid);
            StoredMap<UuidKey, Exertion> xrtMap = views.getExertionMap();
		    return xrtMap.get(new UuidKey(uuid));
        } finally {
            objectsBeingModified.remove(uuid);
        }
	}

    public ModelTable getTable(Uuid uuid) {
        try {
            addToWaitingList(uuid);
            StoredMap<UuidKey, ModelTable> xrtMap = views.getTableMap();
            return xrtMap.get(new UuidKey(uuid));
        } finally {
            objectsBeingModified.remove(uuid);
        }
    }

	protected class PersistThread extends Thread {

		Object object;
		Uuid uuid;

		public PersistThread(Object object) {
            super(tName("PersistThread-" + ((Identifiable)object).getId()));
			this.object = object;
			this.uuid = (Uuid)((Identifiable)object).getId();
            addToWaitingList(uuid);
		}

		@SuppressWarnings("unchecked")
		public void run() {
			try {
                StoredValueSet storedSet = null;
                if (object instanceof Context) {
                    storedSet = views.getContextSet();
                    storedSet.add(object);
                } else if (object instanceof Exertion) {
                    storedSet = views.getExertionSet();
                    storedSet.add(object);
                } else if (object instanceof ModelTable) {
                    storedSet = views.getTableSet();
                    storedSet.add(object);
                } else if (object instanceof UuidObject) {
                    storedSet = views.getUuidObjectSet();
                    storedSet.add(object);
                }
            } finally {
                objectsBeingModified.remove(this.uuid);
            }
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}

	protected class UpdateThread extends Thread {

		Object object;
		Uuid uuid;

		public UpdateThread(Uuid uuid, Object object) throws InvalidObjectException {
			this.uuid = uuid;
			this.object = object;
		}

		public UpdateThread(URL url, Object object) throws InvalidObjectException {
            super(tName("UpdateThread-" + url));
			this.object = object;
			this.uuid = SosDbUtil.getUuid(url);
            addToWaitingList(uuid);
		}
		
		public void run() {
            StoredMap storedMap = null;
            UuidKey key = null;
			try {
                key = new UuidKey(uuid);
                if (object instanceof Context) {
                    storedMap = views.getContextMap();
                    storedMap.replace(key, object);
                } else if (object instanceof Exertion) {
                    storedMap = views.getExertionMap();
                    storedMap.replace(key, object);
                } else if (object instanceof ModelTable) {
                    storedMap = views.getTableMap();
                    storedMap.replace(key, object);
                } else if (object instanceof Object) {
                    storedMap = views.getUuidObjectMap();
                    storedMap.replace(key, object);
                }
            } catch (IllegalArgumentException ie) {
                logger.error("Problem updating object with key: " + key.toString() + "\n" + storedMap.get(key).toString());
                objectsBeingModified.remove(this.uuid);
                throw (ie);
            } finally {
                objectsBeingModified.remove(this.uuid);
            }
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}
	
	protected class DeleteThread extends Thread {

		Uuid uuid;
		Store storeType;
        StoredMap storedMap;

        public DeleteThread(Uuid uuid, Store storeType) {
            super(tName("DeleteThread-" + uuid));
            this.uuid = uuid;
			this.storeType = storeType;
            storedMap = getStoredMap(storeType);
            addToWaitingList(uuid);
		}

		public void run() {
            try {
                storedMap.remove(new UuidKey(uuid));
            } finally {
                objectsBeingModified.remove(this.uuid);
            }
		}
		
		public Uuid getUuid() {
			return uuid;
		}
	}
	
	public Context contextStore(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object object = context.getValue(object_stored);		
		Uuid uuid = store(object);
		Store type = getStoreType(object);
		URL sdbUrl = getDatabaseURL(type, uuid);
		if (context.getReturnPath() != null)
			context.putOutValue(context.getReturnPath().path, sdbUrl);

		context.putOutValue(object_url, sdbUrl);
		context.putOutValue(store_size, getStoreSize(type));
		
		return context;
	}

	public URL getDatabaseURL(Store storeType, Uuid uuid) throws MalformedURLException {
		String pn = providerName;
		if (pn == null || pn.length() == 0 || pn.equals("*"))
			pn = "";
		else
			pn = "/" + pn;
		return new URL("sos://" + DatabaseStorer.class.getName() + pn + "#"
				+ storeType + "=" + uuid);
	}
	
	public URL getSdbUrl() throws MalformedURLException, RemoteException {
		String pn = providerName;
		if (pn == null || pn.length() == 0 || pn.equals("*"))
			pn = "";
		else
			pn = "/" + pn;
		return new URL("sos://" + IDatabaseProvider.class.getName() + pn);
	}
	
	public int size(Store storeType) {
		StoredValueSet storedSet = getStoredSet(storeType);
		return storedSet.size();
	}
	
	public Uuid deleteURL(URL url) {
		Store storeType = SosDbUtil.getStoreType(url);
		Uuid id = SosDbUtil.getUuid(url);
		DeleteThread dt = new DeleteThread(id, storeType);
		dt.start();
		id = dt.getUuid();
		return id;
	}

	public Object retrieve(URL url) {
		Store storeType = SosDbUtil.getStoreType(url);
		Uuid uuid = SosDbUtil.getUuid(url);
		return retrieve(uuid, storeType);
	}
	
	public Object retrieve(Uuid uuid, Store storeType) {
		Object obj = null;
		if (storeType == Store.context)
			obj = getContext(uuid);
		else if (storeType == Store.exertion)
			obj = getExertion(uuid);
        else if (storeType == Store.table)
            obj = getTable(uuid);
        else if (storeType == Store.object)
			obj = getObject(uuid);
		
		return obj;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.StorageMangement#rertieve(sorcer.service.Context)
	 */
	@Override
	public Context contextRetrieve(Context context) throws RemoteException,
			ContextException {
		Store storeType = (Store) context.getValue(object_type);
		Uuid uuid = null;
		Object id = context.getValue(object_uuid);
			if (id instanceof String) {
				uuid = UuidFactory.create((String)id);
			} else if (id instanceof Uuid) {
				uuid = (Uuid)id;
			} else {
				throw new ContextException("No valid stored object Uuid: " + id);
			}
				
		Object obj = retrieve(uuid, storeType);
		if (context.getReturnPath() != null)
			context.putOutValue(context.getReturnPath().path, obj);
		
		// default returned path
		context.putOutValue(object_retrieved, obj);
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#update(sorcer.service.Context)
	 */
	@Override
	public Context contextUpdate(Context context) throws RemoteException,
			ContextException, MalformedURLException, InvalidObjectException {
		Object object = context.getValue(object_updated);
		Object id = context.getValue(object_uuid);
		Uuid uuid = null;
		if (id instanceof String) {
			uuid = UuidFactory.create((String)id);
		} else if (id instanceof Uuid) {
			uuid = (Uuid)id;
		} else {
			throw new ContextException("Wrong update object Uuid: " + id);
		}
        try {
            uuid = update(uuid, object);
        } catch (IllegalArgumentException e) {
            logger.error("GOT EXCEPTION in contextUpdate for:" + id + "\nObject to be updated: " + object.toString() +"CTX:\n" + context);
            throw e;
        }
		Store type = getStoreType(object);
		URL sdbUrl = getDatabaseURL(type, uuid);
		if (context.getReturnPath() != null)
			context.putOutValue(context.getReturnPath().path, sdbUrl);

		context.putOutValue(object_url, sdbUrl);
		context.remove(object_updated);
		context.putOutValue(store_size, getStoreSize(type));
		return context;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextList(sorcer.service.Context)
	 */
	@Override
	public Context contextList(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		List<String> content = list((Store) context.getValue(StorageManagement.store_type));
		context.putValue(StorageManagement.store_content_list, content);
		return context;
	}
	
	public List<String> list(Store storeType) {
		StoredValueSet storedSet = getStoredSet(storeType);
        logger.debug("list got storedSet size: " + storedSet.size());
		List<String> contents = new ArrayList<String>(storedSet.size());
		Iterator it = storedSet.iterator();
		while(it.hasNext()) {
			contents.add(it.next().toString());
		}
		return contents;
	}
	
	public List<String> list(URL url) {
		return list(SosDbUtil.getStoreType(url));
	}

	/* (non-Javadoc)
	 * @see sorcer.core.StorageManagement#contextClear(sorcer.service.Context)
	 */
	@Override
	public Context contextClear(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Store type = (Store)context.getValue(StorageManagement.store_type);
		context.putValue(store_size, clear(type));
		return context;
	}
	
	public int clear(Store type) throws RemoteException,
			ContextException, MalformedURLException {
		StoredValueSet storedSet = getStoredSet(type);
		int size = storedSet.size();
		storedSet.clear();
		return size;
	}

    @ConfigEntry("dbHome")
    private String dbHome;

	protected void setupDatabase() throws DatabaseException, RemoteException {
		logger.info("dbHome: " + dbHome);
		if (dbHome == null || dbHome.length() == 0) {
			logger.info("No provider's database created");
			destroy();
			return;
		}

		File dbHomeFile = null;
		dbHomeFile = new File(dbHome);
		if (!dbHomeFile.isDirectory() && !dbHomeFile.exists()) {
			boolean done = dbHomeFile.mkdirs();
			if (!done) {
				logger.error("Not able to create session database home: {}",
                        dbHomeFile.getAbsolutePath());
				destroy();
				return;
			}
		}
		logger.info("Opening provider's BDBJE in: {}"
				, dbHomeFile.getAbsolutePath());
		db = new SorcerDatabase(dbHome);
		views = new SorcerDatabaseViews(db);
	}
	
	/**
	 * Destroy the service, if possible, including its persistent storage.
	 * 
	 * @see sorcer.core.provider.Provider#destroy()
	 */
	public void destroy() throws RemoteException {
		try {
            int tries=0;
            try {
                while (objectsBeingModified.size()>0 && tries<80) {
                    Thread.sleep(50);
                    tries++;
                }
                if (tries==80) logger.error("Interrupted while {} objects where still being modified", objectsBeingModified.size());
            } catch (InterruptedException ie) {}
			if (db != null) {
				db.close();
			}
		} catch (DatabaseException e) {
			logger.error("Failed to close provider's database",
                    e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#delete(sorcer.service.Context)
	 */
	@Override
	public Context contextDelete(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Object deletedObject = context
				.getValue(StorageManagement.object_deleted);
		if (deletedObject instanceof URL) {
			context.putValue(StorageManagement.object_url, deletedObject);
		} else {
			Uuid id = (Uuid) ((Identifiable) deletedObject).getId();
			context.putValue(StorageManagement.object_url,
					getDatabaseURL(getStoreType(deletedObject), id));
		}
		delete(deletedObject);
		return context;
	}
	
	public StoredMap getStoredMap(Store storeType) {
        waitWhileObjectsAreModified();
		StoredMap storedMap = null;
		if (storeType == Store.context) {
			storedMap = views.getContextMap();
		} else if (storeType == Store.exertion) {
			storedMap = views.getExertionMap();
		} else if (storeType == Store.table) {
            storedMap = views.getTableMap();
        } else if (storeType == Store.object) {
			storedMap = views.getUuidObjectMap();
		}
		return storedMap;
	}
	
	public StoredValueSet getStoredSet(Store storeType) {
        waitWhileObjectsAreModified();
		StoredValueSet storedSet = null;
		if (storeType == Store.context) {
			storedSet = views.getContextSet();
		} else if (storeType == Store.exertion) {
			storedSet = views.getExertionSet();
		} else if (storeType == Store.table) {
            storedSet = views.getTableSet();
        } else if (storeType == Store.object) {
			storedSet = views.getUuidObjectSet();
		}
		return storedSet;
	}
	
	public Uuid delete(Object object) {
		if (object instanceof URL) {
			return deleteURL((URL)object);
		} else if (object instanceof Identifiable) 
			return deleteIdentifiable(object);
		return null;
	}
	
	public Uuid deleteIdentifiable(Object object) {
		Uuid id = (Uuid) ((Identifiable) object).getId();
		DeleteThread dt = null;
		if (object instanceof Context) {
			dt = new DeleteThread(id, Store.context);
		} else if (object instanceof Exertion) {
			dt = new DeleteThread(id, Store.exertion);
		} else if (object instanceof ModelTable) {
            dt = new DeleteThread(id, Store.table);
        } else {
			dt = new DeleteThread(id, Store.object);			
		}
		dt.start();
		id = dt.getUuid();
		return id;
	}
	
	private int getStoreSize(Store type) {
        waitWhileObjectsAreModified();
		if (type == Store.context) {
			return views.getContextSet().size();
		} else if (type == Store.exertion) {
			return views.getExertionSet().size();
		} else if (type == Store.table) {
            return views.getTableSet().size();
        } else {
			return views.getUuidObjectSet().size();
		}
	}
	
	private Store getStoreType(Object object) {
		Store type = Store.object;
		if (object instanceof Context) {
			type = Store.context;
		} else if (object instanceof Exertion) {
			type = Store.exertion;
		} else if (object instanceof ModelTable) {
            type = Store.table;
        }
        return type;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#contextSize(sorcer.service.Context)
	 */
	@Override
	public Context contextSize(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		Store type = (Store)context.getValue(StorageManagement.store_type);
		if (context.getReturnPath() != null)
			context.putOutValue(context.getReturnPath().path, getStoreSize(type));
		context.putOutValue(store_size, getStoreSize(type));
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.core.provider.StorageManagement#contextRecords(sorcer.service.Context)
	 */
	@Override
	public Context contextRecords(Context context) throws RemoteException,
			ContextException, MalformedURLException {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public URL storeObject(Object object) {
        Uuid uuid = store(object);
        Store type = getStoreType(object);
        try {
            return getDatabaseURL(type, uuid);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Couldn't parse my own URL", e);
        }
    }

    /**
     * the same as #update() but hide requirement on Uuid class
     */
    @Override
    public void updateObject(URL url, Object object) throws InvalidObjectException {
        update(url, object);
    }

    @Override
    public void deleteObject(URL url) {
        deleteURL(url);
    }
}
