/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.dispatch;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import sorcer.core.context.ServiceContext;
import sorcer.core.provider.Provider;
import sorcer.core.exertion.Jobs;
import sorcer.service.*;
import static sorcer.service.Exec.*;

public class CatalogSequentialDispatcher extends CatalogExertDispatcher {

	@SuppressWarnings("rawtypes")
	public CatalogSequentialDispatcher(Exertion job,
            Set<Context> sharedContext,
            boolean isSpawned, 
            Provider provider,
            ProvisionManager provisionManager,
            ProviderProvisionManager providerProvisionManager) {
		super(job, sharedContext, isSpawned, provider, provisionManager, providerProvisionManager);
	}

    protected void doExec() throws ExertionException,
			SignatureException {

            String pn;
            if (inputXrts == null) {
                xrt.setStatus(FAILED);
                state = FAILED;
                try {
                    pn = provider.getProviderName();
                    if (pn == null)
                        pn = provider.getClass().getName();
                    ExertionException fe = new ExertionException(pn + " received invalid job: "
                            + xrt.getName(), xrt);

                    xrt.reportException(fe);
                    dispatchers.remove(xrt.getId());
                    throw fe;
                } catch (RemoteException e) {
                    logger.warn("Error during local call", e);
                }
            }

            xrt.startExecTime();
            Context previous = null;
            for (Exertion exertion: inputXrts) {

                // Added for Blocks
                if (xrt.isBlock()) {
                    try {
                        ((ServiceContext) exertion.getContext()).setBlockScope(xrt.getContext());
                    } catch (ContextException ce) {
                        throw new ExertionException(ce);
                    }
                }

                ServiceExertion se = (ServiceExertion) exertion;
                // support for continuous pre and post execution of task
                // signatures
                if (previous != null && se.isTask() && ((Task) se).isContinous())
                    se.setContext(previous);

                dispatchExertion(se);
                try {
                    previous = exertion.getContext();
                } catch (ContextException e) {
                    throw new ExertionException(e);
                }
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
			xrt.setStatus(DONE);
	}

    protected void dispatchExertion(ServiceExertion se) throws SignatureException, ExertionException {
        se = (ServiceExertion) execExertion(se);
        if (se.getStatus() <= FAILED) {
            xrt.setStatus(FAILED);
            state = FAILED;
            try {
                String pn = provider.getProviderName();
                if (pn == null)
                    pn = provider.getClass().getName();
                ExertionException fe = new ExertionException(pn
                        + " received failed task: " + se.getName(), se);
                xrt.reportException(fe);
                dispatchers.remove(xrt.getId());
                throw fe;
            } catch (RemoteException e) {
                logger.warn("Exception during local call");
            }
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

    protected List<Exertion> getInputExertions() throws ContextException {
        return Jobs.getInputExertions(((Job) xrt));
    }
}
