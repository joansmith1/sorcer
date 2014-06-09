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

import java.lang.reflect.Array;
import java.util.*;

import javax.security.auth.Subject;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.co.tuple.Tuple2;
import sorcer.core.DispatchResult;
import sorcer.core.Dispatcher;
import sorcer.core.provider.Provider;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.exertion.Jobs;
import sorcer.service.*;

import static sorcer.service.Exec.*;

@SuppressWarnings("rawtypes")
abstract public class ExertDispatcher implements Dispatcher {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ServiceExertion xrt;

    protected ServiceExertion masterXrt;

    protected List<Exertion> inputXrts;

	protected volatile int state = INITIAL;

    protected boolean isMonitored;

    protected Set<Context> sharedContexts;

    // If it is spawned by another dispatcher.
    protected boolean isSpawned;

    // All dispatchers spawned by this one.
    protected List<Uuid> runningExertionIDs = Collections.synchronizedList(new LinkedList<Uuid>());

    // subject for whom this dispatcher is running.
    // make sure subject is set before and after any object goes out and comes
    // in dispatcher.
    protected Subject subject;

    protected Provider provider;

    protected static Map<Uuid, Dispatcher> dispatchers
            = new HashMap<Uuid, Dispatcher>();

	protected ThreadGroup disatchGroup;
	protected ProviderProvisionManager providerProvisionManager;
    protected ProvisionManager provisionManager;

	public static Map<Uuid, Dispatcher> getDispatchers() {
		return dispatchers;
	}

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

	public ExertDispatcher(Exertion exertion,
                           Set<Context> sharedContexts,
                           boolean isSpawned,
                           Provider provider,
                           ProvisionManager provisionManager,
                           ProviderProvisionManager providerProvisionManager) {
        ServiceExertion sxrt = (ServiceExertion)exertion;
		this.xrt = sxrt;
        this.subject = sxrt.getSubject();
        this.sharedContexts = sharedContexts;
        this.isSpawned = isSpawned;
        this.isMonitored = sxrt.isMonitorable();
        this.provider = provider;
        this.provisionManager = provisionManager;
        this.providerProvisionManager = providerProvisionManager;
    }

    public void exec() {
        dispatchers.put(xrt.getId(), this);
        state = RUNNING;
        if (xrt instanceof Job) {
            masterXrt = (ServiceExertion) ((Job) xrt).getMasterExertion();
        }
        try {
            beforeExec(xrt);
            doExec();
            afterExec(xrt);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.info("Exertion dispatcher thread killed by exception", e);
            xrt.setStatus(FAILED);
            state = FAILED;
            xrt.reportException(e);
        }finally {
            dispatchers.remove(xrt.getId());
        }
    }

    abstract protected void doExec() throws SignatureException, ExertionException;
    abstract protected List<Exertion> getInputExertions() throws ContextException;

    protected void beforeExec(Exertion exertion) throws ExertionException, SignatureException, ContextException {
        logger.info("before exert {}", exertion);
        reconcileInputExertions(exertion);
        updateInputs(exertion);
        checkProvision();
        inputXrts = getInputExertions();
    }

    protected void afterExec(Exertion result) throws ContextException {
        logger.info("After exert {}", result);
    }

    @Override
    public DispatchResult getResult() {
        /**
         * The default implementation - wait for status to be changed by another thread
         */

        try {
            while (!finished())
                Thread.sleep(50);
        } catch (InterruptedException e) {
            logger.warn("Interrupted!", e);
        }
        return new DispatchResult(State.values()[state], xrt);
    }

    private boolean finished(){
        return state == State.DONE.ordinal() || state == State.FAILED.ordinal();
    }

    /**
     * If the {@code Exertion} is provisionable, deploy services.
     *
     * @throws ExertionException if there are issues dispatching the {@code Exertion}
     */
    protected void checkProvision() throws ExertionException {
        if(xrt.isProvisionable() && xrt.getDeployments().size()>0) {
            try {
                getProvisionManager().deployServices();
            } catch (DispatcherException e) {
                throw new ExertionException("Unable to deploy services", e);
            }
        }
    }

    public Exertion getExertion() {
        return xrt;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    protected class CollectResultThread implements Runnable {
        public void run() {
            xrt.startExecTime();
            try {
                collectResults();
                xrt.setStatus(DONE);
            } catch (Exception ex) {
                xrt.setStatus(FAILED);
                xrt.reportException(ex);
                ex.printStackTrace();
            }
            if (xrt.isExecTimeRequested())
                xrt.stopExecTime();
            dispatchers.remove(xrt.getId());
        }
    }

    protected void collectResults() throws ExertionException, SignatureException{}
    //protected abstract void dispatchExertions() throws ExertionException, SignatureException;

    protected void collectOutputs(Exertion ex) throws ContextException {
        if (sharedContexts==null) {
            logger.warn("Trying to update sharedContexts but it is null for exertion: " + ex);
            return;
        }
        List<Context> contexts = Jobs.getTaskContexts(ex);
        logger.info("Contexts to check if shared: " + contexts.toString());
        for (Context ctx : contexts) {
            if (((ServiceContext)ctx).isShared()) {
                sharedContexts.add(ctx);
                logger.debug("Added shared context: " + ctx);
            }
        }
//      for (int i = 0; i < contexts.size(); i++) {
//			if (!sharedContexts.contains(contexts.get(i)))
//				sharedContexts.add(contexts.get(i));
//            if (((ServiceContext)contexts.get(i)).isShared())
//                sharedContexts.add(contexts.get(i));
//        }
    }

    protected void updateInputs(Exertion ex) throws ExertionException, ContextException {
        List<Context> inputContexts = Jobs.getTaskContexts(ex);
        for (Context inputContext : inputContexts)
            updateInputs((ServiceContext) inputContext);
    }

	protected void updateInputs(ServiceContext toContext)
			throws ExertionException {
		ServiceContext fromContext;
		String toPath = null, newToPath = null, toPathcp, fromPath = null;
		int argIndex = -1;
		try {
			Map<String, String> toInMap = Contexts.getInPathsMap(toContext);
			logger.info("updating inputs in context toContext = {}", toContext);
			logger.info("updating based on = {}", toInMap);
			for (Map.Entry<String, String> e  : toInMap.entrySet()) {
                toPath = e.getKey();
				// find argument for parametric context
				if (toPath.endsWith("]")) {
					Tuple2<String, Integer> pair = getPathIndex(toPath);
					argIndex = pair._2;
					if	(argIndex >=0) {
						newToPath = pair._1;
					}
				}
				toPathcp = e.getValue();
				logger.info("toPathcp = {}", toPathcp);
				fromPath = Contexts.getContextParameterPath(toPathcp);
				logger.info("context ID = {}", Contexts.getContextParameterID(toPathcp));
				fromContext = getSharedContext(fromPath, Contexts.getContextParameterID(toPathcp));
				logger.info("fromContext = {}", fromContext);
				logger.info("before updating toContext: {}", toContext
						+ "\n>>> TO path: " + toPath + "\nfromContext: "
						+ fromContext + "\n>>> FROM path: " + fromPath);
                if (fromContext != null) {
					logger.info("updating toContext: {}", toContext
							+ "\n>>> TO path: " + toPath + "\nfromContext: "
							+ fromContext + "\n>>> FROM path: " + fromPath);
                    // make parametric substitution if needed
                    if (argIndex >=0 ) {
                        Object args = toContext.getValue(Context.PARAMETER_VALUES);
                        if (args.getClass().isArray()) {
                            if (Array.getLength(args) > 0) {
                                Array.set(args, argIndex, fromContext.getValue(fromPath));
                            } else {
                                // the parameter array is empty
								Object[] newArgs;
                                newArgs = new Object[] { fromContext.getValue(fromPath) };
                                toContext.putValue(newToPath, newArgs);
                            }
                        }
                    } else {
                        // make contextual substitution
                        Contexts.copyValue(fromContext, fromPath, toContext, toPath);
                    }
//					logger.info("updated dataContext:\n" + toContext);
                }
            }
        } catch (Exception ex) {
			throw new ExertionException("Failed to update data dataContext: " + toContext.getName()
                    + " at: " + toPath + " from: " + fromPath, ex);
        }
    }

    private Tuple2<String, Integer> getPathIndex(String path) {
        int index = -1;
        String newPath = null;
        int i1 = path.lastIndexOf('/');
        String lastAttribute = path.substring(i1+1);
        if (lastAttribute.charAt(0) == '[' && lastAttribute.charAt(lastAttribute.length() - 1) == ']') {
            index = Integer.parseInt(lastAttribute.substring(1, lastAttribute.length()-1));
            newPath = path.substring(0, i1+1);
        }
        return new Tuple2<String, Integer>(newPath, index);
    }

    protected ServiceContext getSharedContext(String path, String id) {
		// try to get the dataContext with particular id.
		// If not found, then find a dataContext with particular path.
		if (Context.EMPTY_LEAF.equals(path) || "".equals(path))
            return null;
        if (id != null && id.length() > 0) {
            for (Context hc : sharedContexts) {
                if (UuidFactory.create(id).equals(hc.getId()))
                    return (ServiceContext) hc;
            }
        }
        else {
            for (Context hc : sharedContexts) {
                if (hc.containsPath(path))
                    return (ServiceContext) hc;
            }
        }
        return null;
    }

    public boolean isMonitorable() {
        return isMonitored;
    }

    public ProvisionManager getProvisionManager() {
        return provisionManager;
    }

    protected void reconcileInputExertions(Exertion ex) throws ContextException {
        ServiceExertion ext = (ServiceExertion)ex;
        if (ext.getStatus() == DONE) {
            collectOutputs(ex);
            if (inputXrts != null)
                inputXrts.remove(ex);
        } else {
            ext.setStatus(INITIAL);
            if (ex instanceof CompoundExertion) {
                CompoundExertion ce = (CompoundExertion) ex;
                for (Exertion sub : ce.getExertions())
                    reconcileInputExertions(sub);
            }
        }
    }
}
