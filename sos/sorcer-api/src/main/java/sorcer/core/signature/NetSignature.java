/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.signature;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import net.jini.core.entry.Entry;
import net.jini.lookup.entry.Name;
import sorcer.core.provider.Provider;
import sorcer.service.Accessor;
import sorcer.service.Exertion;
import sorcer.service.ExertionException;
import sorcer.service.Service;
import sorcer.service.SignatureException;

import static sorcer.core.SorcerConstants.ANY;

public class NetSignature extends ObjectSignature {

	/**
	 * URL for a mobile agent: an inserted custom method executed by service
	 * providers
	 */
	private String agentCodebase;

	/** codebase for downloaded classes */
	private String codebase = System.getProperty("java.rmi.server.codebase");

	private String agentClass;

	private String portalURL;

	private boolean isUnicast;

	// provider bound to this signature
	transient private Provider provider;

	protected List<Entry> attributes;

    protected String version;

	public NetSignature() {
		providerName = ANY;
	}

	public NetSignature(ServiceSignature signature) {
		this(signature.getSelector(), signature.getServiceType(), signature
				.getProviderName());
	}
	
	public NetSignature(Class<?> serviceType) {
		this("none", serviceType, ANY);
	}
	
	public NetSignature(String selector, Class<?> serviceType) {
		this(selector, serviceType, ANY);
	}

	public NetSignature(String selector, Class<?> serviceType,
			String providerName) {
		this(selector, serviceType, providerName, (Type) null);
	}

	public NetSignature(String selector, Class<?> serviceType,
			Type methodType) throws SignatureException {
		this(selector, serviceType);
		this.execType = methodType;
	}

	public NetSignature(String selector, Class<?> serviceType,
			List<Entry> attributes, Type methodType)
			throws SignatureException {
		this(selector, serviceType);
		this.execType = methodType;
		this.attributes = attributes;
	}

	public NetSignature(String selector, Class<?> serviceType,
			String providerName, Type methodType) {
		this.serviceType = serviceType;
		if (providerName == null || providerName.length() == 0)
			this.providerName = ANY;
		else
			this.providerName = providerName;
		if (methodType == null)
			execType = Type.SRV;
		else
			execType = methodType;
		
		setSelector(selector);
	}

    /**
    String version of constructor - required i.e. when running from Scilab
    */
    public NetSignature(String selector, String strServiceType) {
        try {
            Class serviceType = Class.forName(strServiceType);
            this.serviceType = serviceType;
            setSelector(selector);
        } catch (ClassNotFoundException e) {
            logger.severe("Problem creating NetSignature: " + e.getMessage());
        }
    }

    public NetSignature(String selector, Class<?> serviceType, String version,
                        String providerName, Type methodType) {
        this(selector, serviceType, providerName, methodType);
        this.version = version;
    }

    public NetSignature(String selector, Class<?> serviceType, String version,
                        String providerName) {
        this(selector, serviceType, version, providerName, null);
    }


    public void setExertion(Exertion exertion) throws ExertionException {
        this.exertion = exertion;
	}

	public Exertion getExertion() {
		return exertion;
	}

	public void setAttributes(List<net.jini.core.entry.Entry> attributes) {
		this.attributes = attributes;
	}

	public List<Entry> getAttributes() {
		if (attributes == null || attributes.size() == 0) {
			Entry[] atts = new Entry[] { new Name(providerName) };
			return Arrays.asList(atts);
		}
		return attributes;
	}

	public void addAttribute(Entry attribute) {
		attributes.add(attribute);
	}

	public void addAllAttributes(List<Entry> attributes) {
		attributes.addAll(attributes);
	}

	public Service getService() {
		if (provider == null) return provider;
		try {
			// ping provider to see if alive
			provider.getProviderName();
		} catch (RemoteException e) {
			// provider is dead; get new one
			//e.printStackTrace();
            provider = (Provider)Accessor.getService(this);
        }
		
		return provider;
	}

	public void setService(Service provider) {
		this.provider = (Provider)provider;
	}

	public String action() {
		String pn = (providerName == null) ? ANY : providerName;
		return serviceType + ", " + selector + ", " + pn;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String name) {
		providerName = name;
	}

	public void setOwnerId(String oid) {
		ownerID = oid;
	}

	public String getOwnerID() {
		return ownerID;
	}

	public String getAgentCodebase() {
		return agentCodebase;
	}

	public void setAgentCodebase(String codebase) {
		agentCodebase = codebase;
	}

	public String getAgentClass() {
		return agentClass;
	}

	public void setAgentClass(String className) {
		agentClass = className;
	}

	public String getPortalURL() {
		return portalURL;
	}

	public void setPortalURL(String url) {
		portalURL = url;
	}

//	public Signature copySignature(Signature m) {
//		ServiceSignature method = (ServiceSignature) m;
//		selector = method.selector;
//		providerName = method.providerName;
//		serviceType = method.serviceType;
//		methodID = method.methodID;
//		portalURL = method.portalURL;
//		codebase = method.codebase;
//		agentCodebase = method.agentCodebase;
//		agentClass = method.agentClass;
//
//		execType = method.execType;
//		selfMode = method.selfMode;
//		isActive = method.isActive;
//		group = method.group;
//		contextTemplateIDs = method.contextTemplateIDs;
//		exertion = method.exertion;
//		order = method.order;
//		// taskID = method.taskID;
//		// contextID = method.contextID;
//
//		return this;
//	}

//	public void setSelector(String opertionName) throws SignatureException {
//		selector = opertionName;
//		isSelectable();
//	}
//
//	public boolean isSelectable() throws SignatureException {
//		if (selector == null && serviceType == null) {
//			return false;
//		}
//		Method[] methods = serviceType.getMethods();
//		for (Method m : methods) {
//			if (m.getName().equals(selector)) {
//				return true;
//			}
//		}
//		throw new SignatureException("No selector:" + selector
//				+ " in service type: " + serviceType.getName());
//	}


//	public String toString() {
//		return providerName + ":" + execType + ":" + isActive + ":"
//				+ serviceType + ":" + selector;
//	}

	public boolean equals(ServiceSignature method) {
		return (method != null) ? toString().equals(method.toString()) : false;
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String getCodebase() {
		return codebase;
	}

	public void setCodebase(String codebase) {
		this.codebase = codebase;
	}

//	/**
//	 * Returns a method provided by the requestor itself to substitute the
//	 * existing provider'smethod. The implementation of this requestor's method
//	 * overrides the existing implementation of the provider if one exists.Thus
//	 * a new functionality is inserted into the executing provider if the
//	 * alternative inserted method is acceptable (valid).
//	 */
//	public Method getSubstituteMethod(Class<?>[] argTypes) {
//		Method m = null;
//		try {
//			Class<?> clazz = getSubstituteClass();
//			if (clazz == null)
//				return null;
//			m = clazz.getMethod(selector, argTypes);
//		} catch (NoSuchMethodException nsme) {
//			nsme.printStackTrace();
//			// logger.info("The method: \"" + selector
//			// + "\" doesn't exist in requestor's signature code");
//		}
//		return m;
//	}

/*
	public Exertion invokeMethod(Exertion ex) throws RemoteException,
			ExertionException {
		// If customized method provided by Mobile Agent
		Method m = getSubstituteMethod(new Class[] { Exertion.class });
		try {
			if (m != null) {
                return (Exertion) m.invoke(this, ex);
            }

			if (((ServiceProvider) provider).isValidMethod(selector)) {
				return (Exertion) ((ServiceProvider) provider).getDelegate()
						.invokeMethod(selector, ex);
			} else {
				ExertionException eme = new ExertionException(
						"Not supported method: " + serviceType + "#" + selector
								+ " by: "
								+ ((Provider) provider).getProviderName());
				((Provider) provider).notifyException(ex, "unsupported method",
						eme);
				throw eme;
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

	public Context invokeMethod(Context context) throws RemoteException,
			ExertionException {
		// If customized method provided by Mobile Agent
		Method m = getSubstituteMethod(new Class[] { Context.class });
		try {
			if (m != null)
				return ((Context) m.invoke(this, new Object[] { context }));

			if (((ServiceProvider) provider).isValidMethod(selector)) {
				Context resultContext = ((ServiceProvider) provider)
						.getDelegate().invokeMethod(selector, context);
				return resultContext;
			} else {
				ExertionException eme = new ExertionException(
						"Not supported method: " + serviceType + "#" + selector
								+ " by: "
								+ provider.getProviderName());
				provider.notifyException(context.getExertion(),
						"unsupported method", eme);
				throw eme;
			}
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}
*/

	public boolean isSelectable() {
		if (selector == null && serviceType == null) {
			return false;
		}
		Method[] methods = serviceType.getMethods();
		for (Method m : methods) {
			if (m.getName().equals(selector)) {
				return true;
			}
		}
		return false;
	}

	public boolean isUnicast() {
		return isUnicast;
	}

	public void setUnicast(boolean isUnicast) {
		this.isUnicast = isUnicast;
	}

    public String getVersion() {
        return version;
	}

	public String toString() {
		return this.getClass() + ":" + providerName + ";" + execType + ";" + isActive + ";"
				+ serviceType + ";" + selector 
					+ (prefix !=null ? "#" + prefix : "") 
					+ (returnPath != null ? ";"  + returnPath : "");
	}
}
