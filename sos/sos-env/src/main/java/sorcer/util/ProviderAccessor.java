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
package sorcer.util;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceItemFilter;
import sorcer.core.Cataloger;
import sorcer.core.Provider;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.river.Filters;
import sorcer.service.Accessor;
import sorcer.service.DynamicAccessor;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class that provides access to SORCER services and some
 * infrastructure services. It extends the <code>ServiceAccessor</code>
 * functionality.
 * 
 * The <code>getService</code> methods directly use ServiceAccessor calls while
 * the <code>getService</code> methods use a SORCER Cataloger's cached services
 * with round robin load balancing.
 * 
 * The individual SORCER services should be accessed using this utility since it
 * uses local cached proxies for frequently used SORCER infrastructure services,
 * for example: Cataloger, JavaSpace, Jobber. ProviderAccessor normally uses
 * Cataloger if available, otherwise it uses Jini lookup services as implemented
 * by <code>ServiceAccessor</code>.
 * 
 * @see ServiceAccessor
 */

public class ProviderAccessor extends ServiceAccessor implements
		DynamicAccessor {

	static Logger logger = Logger.getLogger(ProviderAccessor.class.getName());

    /**
	 * Used for local caching to speed up getting frequently needed service
	 * providers. Calls to discover JavaSpace takes a lot of time.
	 */
	protected static Cataloger cataloger;

    protected static ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

    protected static Map<Class, Object> cache = new HashMap<Class, Object>();

	public ProviderAccessor() {
		// Nothing to do, uses the singleton design pattern
	}

    /**
	 * Returns a SORCER service provider registered with the most significant
	 * and the least significant bits.
	 *
	 * @param mostSig
	 *            most significant bits
	 * @param leastSig
	 *            least significant bits
	 * @return a SORCER provider service
	 */
	public Object getService(long mostSig, long leastSig) {
		ServiceID serviceID = new ServiceID(mostSig, leastSig);
		return getService(serviceID, null, null, SorcerEnv.getLookupGroups());
	}

	/**
	 * Returns a SORCER service provider with the specified name and service
	 * type, using a Cataloger if available, otherwise using Jini lookup
	 * services.
	 *
	 * @param providerName
	 *            the name of service provider
	 * @param serviceType
	 *            a provider service type (interface)
	 * @return a SORCER provider service
	 */
	public <T> T getProvider(String providerName, Class<T> serviceType) {
		Provider servicer = null;
		if (providerName != null) {
            if (providerName.equals(SorcerConstants.ANY))
                providerName = null;
            if(SorcerConstants.NAME_DEFAULT.equals(providerName)){
                providerName = providerNameUtil.getName(serviceType);
            }
        }

		try {
			//servicer = (Service)ProviderLookup.getService(providerName, serviceType);
			cataloger = getCataloger();
			if (cataloger != null) {
				int tryNo = 0;
				while (tryNo < ServiceAccessor.LUS_REAPEAT) {
					servicer = cataloger.lookup(providerName, serviceType);
					//servicer = (Service)cataloger.lookupItem(providerName, serviceType).service;
					if (servicer != null)
						break;

					Thread.sleep(ServiceAccessor.WAIT_FOR);
					tryNo++;
				}
			}
			// fall back on Jini LUS
			if (servicer == null) {
				servicer = (Provider) super.getService(providerName, serviceType);
			}
		} catch (Throwable ex) {
			logger.throwing(ProviderAccessor.class.getName(), "getService", ex);
			ex.printStackTrace();
		}
		return (T) servicer;
	}


    /**
	 * Returns a SORCER service provider with the specified service ID using a
	 * Cataloger if available, otherwise using Jini lookup services.
	 *
	 * @param serviceID
	 *            serviceID of the desired service
	 * @return a SORCER provider service
	 */
	public Object getService(ServiceID serviceID) {
		try {
			cataloger = getCataloger();
			if (cataloger != null)
				return cataloger.lookup(serviceID);
			else
				return super.getService(serviceID);
		} catch (Exception ex) {
			logger.throwing(ProviderAccessor.class.getName(), "getService", ex);
			return null;
		}
	}

	/**
	 * Returns a SORCER service provider matching a given attributes.
	 *
	 * @param attributes
	 *            attribute set to match
	 * @return a SORCER provider
	 */
	public Provider getProvider(Entry[] attributes) {
		return (Provider) getService(null, null, attributes, SorcerEnv.getLookupGroups());
	}

	/**
	 * Returns a SORCER service provider matching a given list of implemented
	 * service types (interfaces).
	 *
	 * @param serviceTypes
	 *            a set of service types to match
	 * @return a SORCER provider
	 */
	public Provider getProvider(Class[] serviceTypes) {
		return (Provider) getService(null, serviceTypes, null, SorcerEnv.getLookupGroups());
	}

	/**
	 * Returns a SORCER Cataloger Service.
	 *
	 * This method searches for either a JINI or a RMI Cataloger service.
	 *
	 * @return a Cataloger service proxy
     * @see sorcer.core.Cataloger
	 */
	protected Cataloger getCataloger() {
        return getCataloger(providerNameUtil.getName(Cataloger.class)) ;
	}

	/**
	 * Returns a SORCER Cataloger service provider using JINI discovery.
	 *
	 * @return a SORCER Cataloger
	 */
    protected Cataloger getCataloger(String serviceName) {
        boolean catIsOk;
        try {
            catIsOk = Providers.isAlive((Provider) cataloger);
        } catch (Exception ignored) {
			catIsOk = false;
		}
		try {
			if (catIsOk) {
				return cataloger;
			} else {
                ServiceItem[] serviceItems = getServiceItems(Accessor.getServiceTemplate(null, serviceName, new Class[]{Cataloger.class}, null), 1, 1, null, SorcerEnv.getLookupGroups());
                return cataloger = serviceItems.length == 0 ? null : (Cataloger) serviceItems[0].service;
            }
		} catch (Exception e) {
			logger.throwing(ProviderAccessor.class.getName(), "getService", e);
			return null;
		}
	}

    /**
	 * Returns a SORCER service using a cached Cataloger instance by this
	 * ProviderAccessor. However if it not possible uses a ServiceAccessor to
	 * get a requested service form Jini lookup services directly. This approach
	 * allows for SORCER requestors and providers to avoid continuous usage of
	 * lookup discovery for each needed service that is delegated to a SORCER
	 * Cataloger service.
	 *
	 * @param providerName
	 *            - a name of requested service
	 * @param primaryInterface
	 *            - service type of requested provider
	 * @return a requested service or null if a Cataloger is not available
	 */
	protected Provider lookup(String providerName, Class<Provider> primaryInterface) {
		try {
			// check if the cataloger is alive then return a reqested service
			// provider
			if (Providers.isAlive((Provider) cataloger))
				return cataloger.lookup(providerName, primaryInterface);
			else {
				// try to get a new cataloger and lookup again
				cataloger = getService(providerNameUtil.getName(Cataloger.class), Cataloger.class);
				if (cataloger != null) {
					logger.info("Got service provider from Cataloger");
					return cataloger.lookup(providerName, primaryInterface);
				} else {
					// just get a provider without a Cataloger, use directly
					// LUSs
					logger.severe("No SORCER cataloger available");
					return getService(providerName, primaryInterface);
				}
			}
		} catch (RemoteException ex) {
			logger.throwing(ProviderAccessor.class.getName(), "lookup", ex);
			return null;
		}
	}

    @Override
    public ServiceItem[] getServiceItems(ServiceTemplate template, int minMatches, int maxMatches, ServiceItemFilter filter, String[] groups) {
        if(!Arrays.asList(template.serviceTypes).contains(Cataloger.class)){
            Cataloger cataloger = getCataloger();
            if (cataloger != null) {
                try {
                    ServiceMatches matches = cataloger.lookup(template, maxMatches);
                    ServiceItem[] matching = Filters.matching(matches.items, filter);
                    if (matching.length > 0) return matching;
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Could not contact Cataloger, falling back", e);
                }
            }
        }
        return super.getServiceItems(template, minMatches, maxMatches, filter, groups);
    }
}
