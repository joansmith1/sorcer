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

package sorcer.core.dispatch;

import java.rmi.RemoteException;
import java.util.Set;

import sorcer.core.provider.Provider;
import sorcer.core.SorcerConstants;
import sorcer.core.exertion.Jobs;
import sorcer.service.Context;
import sorcer.service.ExertionException;
import sorcer.service.Job;
import sorcer.service.ServiceExertion;
import sorcer.service.SignatureException;
import sorcer.service.Task;

public class CatalogSequentialDispatcher extends CatalogExertDispatcher {

	@SuppressWarnings("rawtypes")
	public CatalogSequentialDispatcher(Job job, 
            Set<Context> sharedContext,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager) throws Throwable {
		super(job, sharedContext, isSpawned, provider, provisionManager);
	}

	public void dispatchExertions() throws ExertionException,
			SignatureException {
		inputXrts = Jobs.getInputExertions(((Job)xrt));
		reconcileInputExertions(xrt);
		collectResults();
	}

	public void collectResults() throws ExertionException, SignatureException {
		try {
			ExertionException fe = null;
			String pn = null;
			if (inputXrts == null || inputXrts.size() == 0) {
				xrt.setStatus(FAILED);
				state = FAILED;
				try {
					pn = provider.getProviderName();
					if (pn == null) 
						pn = provider.getClass().getName();
					fe = new ExertionException(pn
							+ " received invalid job: "  + xrt.getName(), xrt);
				} catch (RemoteException e) {
					// ignore it, local call
				}
				xrt.reportException(fe);
				dispatchers.remove(xrt.getId());
				throw fe;
			}

			ServiceExertion se = null;
			xrt.startExecTime();
			for (int i = 0; i < inputXrts.size(); i++) {
				se = (ServiceExertion) inputXrts.elementAt(i);
				// Provider is expecting exertion to be in dataContext
				se.getDataContext().setExertion(se);
				// support for continuous pre and post execution of task
				// signatures
				if (i > 0 && se.isTask() && ((Task) se).isContinous())
					se.setContext(inputXrts.elementAt(i - 1).getDataContext());
				if (isInterupted(se)) {
					se.stopExecTime();
					dispatchers.remove(xrt.getId());
					return;
				}
				se = (ServiceExertion) execExertion(se);
				if (se.getStatus() <= FAILED) {
					xrt.setStatus(FAILED);
					state = FAILED;
					try {
						pn = provider.getProviderName();
						if (pn == null) 
							pn = provider.getClass().getName();
						fe = new ExertionException(pn
								+ " received failed task: " + se.getName(), se);
					} catch (RemoteException e) {
						// ignore it, local call
					}
					xrt.reportException(fe);
					dispatchers.remove(xrt.getId());
					throw fe;
				} else if (se.getStatus() == SUSPENDED
						|| xrt.getControlContext().isReview(se)) {
					xrt.setStatus(SUSPENDED);
					ExertionException ex = new ExertionException(
							"exertion suspended", se);
					se.reportException(ex);
					dispatchers.remove(xrt.getId());
					throw ex;
				}
			}
			if (isInterupted(masterXrt)) {
				masterXrt.stopExecTime();
				dispatchers.remove(xrt.getId());
				return;
			}

			if (masterXrt != null) {
				masterXrt = (ServiceExertion) execExertion(masterXrt); // executeMasterExertion();
				if (masterXrt.getStatus() <= FAILED) {
					state = FAILED;
					xrt.setStatus(FAILED);
				} else {
					state = DONE;
					xrt.setStatus(DONE);
				}
			} else
				state = DONE;
			dispatchers.remove(xrt.getId());
			xrt.stopExecTime();
		} finally {
			dThread.stop = true;
		}
	}

}
