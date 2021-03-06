/**
 *
 * Copyright 2013 the original author or authors.
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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
package sorcer.ex1.bean;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.provider.Provider;
import sorcer.core.SorcerEnv;
import sorcer.ex1.Message;
import sorcer.ex1.WhoIsIt;
import sorcer.ex1.provider.ProviderMessage;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.util.StringUtils;

public class WhoIsItBean2 implements WhoIsIt {

    private String providerName ;
	private Logger logger = LoggerFactory.getLogger(WhoIsItBean2.class);

    public void init(Provider provider) throws RemoteException {
        providerName = provider.getProviderName();
	}
	
	public Context getHostName(Context context) throws RemoteException,
			ContextException {
		String hostname;
		logger.trace("entering WhoIsItBean2.getHostName");
		try {
			hostname = SorcerEnv.getLocalHost().getHostName();
			context.putValue("provider/hostname", hostname);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), providerName, rhn));
			
			Thread.sleep(2000);
			context.reportException(new RuntimeException("Slept for 2 sec"));
			context.appendTrace(getClass().getName() + ":" + providerName);

			logger.info("executed getHostName: {}", context);

		} catch (UnknownHostException e1) {
			throw new ContextException("getHostAddress", e1);
		} catch (InterruptedException e2) {
			throw new ContextException("getHostAddress", e2);
		}
		return context;
	}

	public Context getHostAddress(Context context) throws RemoteException,
			ContextException {
		String ipAddress;
		logger.trace("entering WhoIsItBean2.getHostName");
		try {
			ipAddress = SorcerEnv.getHostAddress();
			context.putValue("provider/address", ipAddress);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), providerName, rhn));
			
			Thread.sleep(2000);
			context.reportException(new RuntimeException("Slept for 2 sec"));
			context.appendTrace(getClass().getName() + ":" + providerName);
			
			logger.info("executed getHostAddress: {}", context);

		} catch (UnknownHostException e1) {
			throw new ContextException("getHostAddress", e1);
		} catch (InterruptedException e2) {
			throw new ContextException("getHostAddress", e2);
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.ex1.provider.WhoIsIt#getCanonicalHostName(sorcer.service.Context)
	 */
	public Context getCanonicalHostName(Context context)
			throws RemoteException, ContextException {
		String fqname;
		try {
			fqname = SorcerEnv.getLocalHost().getCanonicalHostName();
			context.putValue("provider/fqname", fqname);
			String rhn = (String) context.getValue("requestor/hostname");
			Message rmsg = (Message) context.getValue("requestor/message");
			context.putValue("provider/message", new ProviderMessage(rmsg
					.getMessage(), providerName, rhn));
		} catch (UnknownHostException e1) {
			context.reportException(e1);
			e1.printStackTrace();
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see sorcer.ex1.provider.WhoIsIt#getTimestamp(sorcer.service.Context)
	 */
	@Override
	public Context getTimestamp(Context context) throws RemoteException,
			ContextException {
        context.putValue("provider/timestamp", StringUtils.getDateTime());
		return context;
	}
}
