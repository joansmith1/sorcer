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

import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.lookup.ServiceItemFilter;
import net.jini.lookup.entry.Name;
import sorcer.core.Provider;
import sorcer.core.SorcerConstants;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.service.SignatureException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * A service discovery and management utility allowing to access services by
 * matching service templates and passing filters provided by clients. The
 * optional usage of a service lookup cache is provided for clients that require
 * lookup for multiple services frequently.
 * <p>
 * A similar service discovery and management functionality is provided by a
 * SORCER Cataloger service provider with round robin load balancing.
 * <p>
 * The continuous discovery of SORCER services is usually delegated to
 * Catalogers while other SORCER service providers can query effectively
 * Cataloger's cached services with round robin load balancing.
 * <p>
 * The individual SORCER services should be accessed using the
 * {@link sorcer.util.ProviderAccessor} subclass, that uses local cached proxies
 * for frequently used SORCER infrastructure services. ProviderAccessor normally
 * uses Cataloger if available, otherwise uses Jini lookup services as
 * implemented by the ServiceAccessor.
 * 
 * @see sorcer.util.ProviderAccessor
 * 
 * @author Mike Sobolewski
 */
public class ServiceAccessor {

	static Logger logger = Log.getProviderLog();

	static private boolean cacheEnabled = Sorcer.isLookupCacheEnabled();

	static long WAIT_FOR = Sorcer.getLookupWaitTime();

	final static int LUS_REAPEAT = 3;

	private static DiscoveryManagement ldManager = null;

	private static ServiceDiscoveryManager sdManager = null;

	private static LookupCache lookupCache = null;

	protected static String[] lookupGroups = Sorcer.getLookupGroups();
	
	private static int MIN_MATCHES = Sorcer.getLookupMinMatches();

	private static int MAX_MATCHES = Sorcer.getLookupMaxMatches();

    protected Map<String, Object> cache = new HashMap<String, Object>();

    protected ProviderNameUtil providerNameUtil = new SorcerProviderNameUtil();

	/**
	 * lookup cache parameters, should be set by clients of this utility class.
	 * lookup cache used at the cache creation time
	 */
	private static ServiceItemFilter cacheFilter;

	/** lookup cache template used at the cache creation time. */
	private static ServiceTemplate cacheTemplate;

	/** can used at the cache creation time or added any time later. */
	private static ServiceDiscoveryListener cacheListener;

	protected ServiceAccessor() {
		// do nothing
	}

	/**
	 * Returns a service item containing a service matching providerName and
	 * serviceType using Jini lookup service.
	 * 
	 * @param providerName name of the requested provider
	 * @param serviceType type of the requested provider
	 * @return a SORCER provider
	 */
	public static ServiceItem getServiceItem(String providerName,
			Class serviceType) throws SignatureException {
		ServiceItem si = null;
		try {
			Class[] serviceTypes = new Class[] { serviceType };
			Entry[] attrSets = new Entry[] { new Name(providerName) };
			ServiceTemplate st = new ServiceTemplate(null, serviceTypes,
					attrSets);
			si = sdManager.lookup(st, null, WAIT_FOR);
		} catch (IOException ioe) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ioe);
		} catch (InterruptedException ie) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ie);
		}
		return si;
	}
	
	/**
	 * Returns a service item containing a service matching only the
	 * serviceType. It uses Jini lookup service.
	 * 
	 * @param serviceType type of the requested provider
	 * @return a SORCER provider
	 */
	public static ServiceItem getServiceItem(Class serviceType) throws SignatureException {
		ServiceItem si = null;
		try {
			Class[] serviceTypes = new Class[] { serviceType };
			ServiceTemplate st = new ServiceTemplate(null, serviceTypes,
					null);
			si = sdManager.lookup(st, null, WAIT_FOR);
		} catch (IOException ioe) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ioe);
		} catch (InterruptedException ie) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ie);
		}
		return si;
	}



	/**
	 * Returns a service item matching a service template and passing a filter
	 * with Jini registration groups. Note that template matching is a remote
	 * operation - matching is done by lookup services while passing a filter is
	 * done on the client side. Clients should provide a service filter, usually
	 * as an object of an inner class. A filter narrows the template matching by
	 * applying more precise, for example boolean selection as required by the
	 * client. No lookup cache is used by this <code>getServiceItem</code>
	 * method.
	 * 
	 * @param template
	 *            template to match remotely.
	 * @param filter
	 *            filer to use or null.
	 * @param groups
	 *            lookup groups to search in.
	 * @return a ServiceItem that matches the template and filter or null if
	 *         none is found.
	 */
	public static ServiceItem getServiceItem(ServiceTemplate template,
			ServiceItemFilter filter, String[] groups) {
		ServiceItem si = null;
		try {
		openDiscoveryManagement(groups);
			si = sdManager.lookup(template, filter, WAIT_FOR);
		} catch (IOException ioe) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ioe);
		} catch (InterruptedException ie) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ie);
		}
		return si;
	}

	/**
	 * Returns a service item using a lookup cache. The service item matches a
	 * service template and passes a filter associated with the lookup cache at
	 * the cache creation time. Also a returned service item passes the filter
	 * used as the parameter in this method.
	 * <p>
	 * Note that template matching is a remote operation - matching is done by
	 * lookup services while passing a filter is done on the client side.
	 * Clients should provide a service filter, usually as an object of an inner
	 * class. A filter narrows the template matching by applying more precise,
	 * for example boolean selection as required by the client.
	 * 
	 * @param filter
	 *            the filter to apply.
	 * @return a ServiceItem matching the filter.
	 */
	public static ServiceItem getServiceItem(ServiceItemFilter filter) {
		ServiceItem si = null;
		try {
			openDiscoveryManagement(getLookupGroups());
			if (lookupCache != null)
				si = lookupCache.lookup(filter);
			// The item may not be in the cache if the lookup cache was just
			// started
			if (si == null) {
				try {
					si = sdManager.lookup(
							new ServiceTemplate(null, null, null), filter,
							WAIT_FOR);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		} catch (IOException ioe) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItem",
					ioe);
			closeLookupCache();
		}
		closeDiscoveryManagement();
		return si;
	}

	/**
	 * Returns all service items that match a service template and pass a filter
	 * with Jini registration groups. Note that template matching is a remote
	 * operation - matching is done by lookup services while passing a filter is
	 * done on the client side. Clients should provide a service filter, usually
	 * as an object of an inner class. A filter narrows the template matching by
	 * applying more precise, for example boolean selection as required by the
	 * client.
	 * 
	 * 
	 * @param template
	 *            template to match remotely.
	 * @param filter
	 *            filer to use or null.
	 * @param groups
	 *            lookup groups to search in.
	 * @return a ServiceItem[] that matches the template and filter.
	 */
	public static ServiceItem[] getServiceItems(ServiceTemplate template,
			ServiceItemFilter filter, String[] groups) {
		ServiceItem sis[] = null;
		try {
			openDiscoveryManagement(groups);
			sis = sdManager.lookup(template, MIN_MATCHES, MAX_MATCHES, filter,
					WAIT_FOR);
		} catch (IOException ioe) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItems",
					ioe);
		} catch (InterruptedException ie) {
			logger.throwing(ServiceAccessor.class.getName(), "getServiceItems",
					ie);
		}
		closeDiscoveryManagement();
		return sis;
	}

	/**
	 * Returns a collection service items using a lookup cache. The service
	 * items match a service template and pass a filter associated with the
	 * lookup cache at the cache creation time. Also returned service items pass
	 * the filter used as the parameter in this method.
	 * <p>
	 * Note that template matching is a remote operation - matching is done by
	 * lookup services while passing a filter is done on the client side.
	 * Clients should provide a service filter, usually as an object of an inner
	 * class. A filter narrows the template matching by applying more precise,
	 * for example boolean selection as required by the client.
	 *
	 *
	 * @param filter
	 *            the filter to apply.
	 * @return a ServiceItem[] matching the filter.
	 */
	public static ServiceItem[] getServiceItems(ServiceItemFilter filter) {
		ServiceItem sis[] = null;
		if (lookupCache != null)
			sis = lookupCache.lookup(filter, MAX_MATCHES);
		return sis;
	}

	/**
	 * Creates a service lookup and discovery manager with a provided service
	 * template, lookup cache filter, and list of jini groups.
	 * 
	 * @param groups River group names
	 * @throws IOException
	 */
	protected static void openDiscoveryManagement(String[] groups)
			throws IOException {
		if (sdManager == null) {
			LookupLocator[] locators = getLookupLocators();
			try {
				logger.finer("[openDiscoveryManagement]\n"
						+ "\tSORCER Group(s): "
						+ StringUtils.arrayToString(groups) + "\n"
						+ "\tLocators:        "
						+ StringUtils.arrayToString(locators));

				ldManager = new LookupDiscoveryManager(groups, locators, null);
				sdManager = new ServiceDiscoveryManager(ldManager,
						new LeaseRenewalManager());
			} catch (Throwable t) {
				Log.getSorcerLog().throwing(ServiceAccessor.class.getName(),
						"openDiscoveryManagement", t);
			}
		}
		// Opening a lookup cache
		openCache();
	}

	/**
	 * Terminates lookup discovery and service discovery mangers.
	 */
	private static void closeDiscoveryManagement() {
		if (cacheEnabled) {
			return;
		}
		if (ldManager != null) {
			ldManager.terminate();
		}
		if (sdManager != null) {
			sdManager.terminate();
		}
		closeLookupCache();
		ldManager = null;
		sdManager = null;
	}

	/**
	 * Creates a lookup cache for the existing service discovery manager
	 */
	private static void openCache() {
		if (cacheEnabled && lookupCache == null) {
			try {
				lookupCache = sdManager.createLookupCache(cacheTemplate,
						cacheFilter, cacheListener);
			} catch (RemoteException e) {
				closeLookupCache();
			}
		}
	}

	/**
	 * Terminates a lookup cache used by this ServiceAccessor.
	 */
	private static void closeLookupCache() {
		if (lookupCache != null) {
			lookupCache.terminate();
			lookupCache = null;
		}
	}

	/**
	 * Returns a service matching serviceType, service attributes (entries), and
	 * passes a provided filter.
	 * 
	 * @param atributes   attributes of the requested provider
	 * @param serviceType type of the requested provider
	 * @return a SORCER provider
	 */
	public static <T>T getService(Class<T> serviceType,
			Entry[] atributes, ServiceItemFilter filter) {
		return getService(serviceType, atributes, filter, null);
	}

	/**
	 * Returns a service matching serviceType, service attributes (entries),
	 * passes a provided filter, and uses a given codebase for downloadable
	 * classes.
	 *
     * @param atributes   attributes of the requested provider
     * @param serviceType type of the requested provider
	 * @return a SORCER provider
	 */
    @SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> serviceType,
			Entry[] atributes, ServiceItemFilter filter, String codebase) {
		if (serviceType == null) {
			throw new RuntimeException("Missing service type for a ServiceTemplate");
		}
//		logger.info("serviceType: " + serviceType + "\nattributes: "
//				+ SorcerUtil.arrayToString(atributes) + "\nfilter: " + filter
//				+ "\ncodebase: " + codebase);

		ServiceTemplate tmpl = new ServiceTemplate(null, new Class[] { serviceType }, atributes);

		ServiceItem si = getServiceItem(tmpl, filter, Sorcer.getLookupGroups());
		if (si != null)
			return (T)si.service;
		else
			return null;
	}

	/**
	 * Returns a service matching serviceName and serviceType using Jini lookup
	 * service.
	 *
     * @param serviceName name of the requested provider
     * @param serviceType type of the requested provider
	 * @return a service provider
	 */
	public static <T> T getService(String serviceName, Class<T> serviceType) {
		T proxy = null;
		if (serviceName != null && serviceName.equals(SorcerConstants.ANY))
			serviceName = null;
		int tryNo = 0;
		while (tryNo < LUS_REAPEAT) {
			logger.info("trying to get service: " + serviceType + ":" + serviceName + "; attempt: "
					+ tryNo + "...");
			try {
				tryNo++;
				proxy = getService(serviceType, new Entry[] { new Name(
						serviceName) }, null);
				if (proxy != null)
					break;

				Thread.sleep(WAIT_FOR);
			} catch (Exception e) {
				logger.throwing("" + ServiceAccessor.class, "getService", e);
			}
		}
		logger.info("got LUS service [type=" + serviceType + " name=" + serviceName + "]: " + proxy);

		return proxy;
	}

	/**
	 * Returns a service matching the given serviceType, provider Name, and
	 * codebase where to get the interface class from.
	 * 
	 * @param providerName name of the requestod provider
	 * @param serviceType type of the requested provider
	 * @param codebase     codebase to load the provider class from
	 * @return a service provider
	 */
	public static Object getService(String providerName,
			Class serviceType, String codebase) {
		if (providerName != null && providerName.equals(SorcerConstants.ANY))
			providerName = null;
		return getService(serviceType, new Entry[] { new Name(providerName) },
				null, codebase);
	}

	/**
	 * Returns a service matching a given template filter, and Jini lookup
	 * service.
	 * 
	 * @param template service template
	 * @param filter   service filter
	 * @param groups   River groups list
	 * @return a service provider
	 */
	public static Object getService(ServiceTemplate template,
			ServiceItemFilter filter, String[] groups) {
		ServiceItem si = getServiceItem(template, filter, groups);
		if (si != null)
			return si.service;
		else
			return null;
	}

	/**
	 * Returns a list of lookup locators with the URLs defined in the SORCER
	 * environment
	 * 
	 * @see sorcer.util.Sorcer
	 * 
	 * @return a list of locators for unicast lookup discovery
	 */
	private static LookupLocator[] getLookupLocators() {
		List<LookupLocator> locators = new Vector<LookupLocator>();
		String[] locURLs = Sorcer.getLookupLocators();
		logger.finer("ProviderAccessor Locators: " + Arrays.toString(locURLs));

		if (locURLs != null && locURLs.length > 0) {
			for (String locURL : locURLs)
				try {
					locators.add(new LookupLocator(locURL));
				} catch (Throwable t) {
					Log.getSorcerLog().warning(
							"Invalid Lookup URL: " + locURL);
				}
		}
		if (locators.isEmpty())
			return null;
		LookupLocator[] la = new LookupLocator[locators.size()];
		locators.toArray(la);
		return la;
	}

	/**
	 * Returns a list of groups as defined in the SORCER environment.
	 * 
	 * @return a list of group names
	 * @see Sorcer
	 */
	public  static String[] getGroups() {
		if (lookupGroups == null)
			return Sorcer.getLookupGroups();
		else
			return lookupGroups;
	}

	/**
	 * @param filter
	 *            The filter to set.
	 */
	public static void setFilter(ServiceItemFilter filter) {
		cacheFilter = filter;
	}

	/**
	 * @return Returns the listener.
	 */
	public static ServiceDiscoveryListener getCacheListener() {
		return cacheListener;
	}

	/**
	 * @param listener
	 *            The listener to set.
	 */
	public static void setCacheListener(ServiceDiscoveryListener listener) {
		ServiceAccessor.cacheListener = cacheListener;
	}

	/**
	 * @return Returns the template.
	 */
	public static ServiceTemplate getCacheTemplate() {
		return cacheTemplate;
	}

	/**
	 * @param template
	 *            The template to set.
	 */
	public static void setCacheTemplate(ServiceTemplate template) {
		ServiceAccessor.cacheTemplate = template;
	}

	/**
	 * Makes sure the lookup cache is enabled and returns a reference to it.
	 * 
	 * @return the lookup cache.
	 */
	public static LookupCache getAndEnableLookupCache() {
		ServiceAccessor.cacheEnabled = true;
		try {
			openDiscoveryManagement(getLookupGroups());
		} catch (IOException e) {
			logger.warning(e.getMessage());
		}
		return lookupCache;
	}

	public static String[] getLookupGroups() {
		return lookupGroups;
	}

	public static void setLookupGroups(String[] lookupGroups) {
		ServiceAccessor.lookupGroups = lookupGroups;
	}

	public static ServiceTemplate getServiceTemplate(ServiceID serviceID,
			String providerName, Class[] serviceTypes,
			String[] publishedServiceTypes) {
		Class[] types;
		String name;
		Entry[] attributes;

		if (providerName != null && providerName.length() != 0)
			name = providerName;
		else
			name = SorcerConstants.P_UNDEFINED;

		if (serviceTypes == null)
			types = new Class[] { Provider.class };
		else
			types = serviceTypes;

		SorcerServiceInfo st = new SorcerServiceInfo();
		st.providerName = name;

		if (publishedServiceTypes != null)
			st.publishedServices = publishedServiceTypes;

		attributes = new Entry[] { st };

		logger.info("getServiceTemplate >> \n serviceID: " + serviceID
				+ "\nproviderName: " + providerName + "\nserviceTypes: "
				+ StringUtils.arrayToString(serviceTypes)
				+ "\npublishedServiceTypes: "
				+ StringUtils.arrayToString(publishedServiceTypes));

		return new ServiceTemplate(serviceID, types, attributes);
	}
	
	public static void terminateDiscovery() {
		sdManager.terminate();
		sdManager = null;
	}

    /**
     * Test if provider is still replying.
     *
     * @param provider the provider to check
     * @return true if a provider is alive, otherwise false
     * @throws java.rmi.RemoteException
     */
    public static boolean isAlive(Provider provider)
            throws RemoteException {
        if (provider == null)
            return false;
        try {
            provider.getProviderName();
            return true;
        } catch (RemoteException e) {
            ProviderAccessor.logger.throwing(ProviderAccessor.class.getName(), "isAlive", e);
            throw e;
        }
	}
}
