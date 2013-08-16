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
package sorcer.core.provider;

import com.sun.jini.config.Config;
import groovy.lang.GroovyShell;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.entry.Entry;
import net.jini.core.event.RemoteEvent;
import net.jini.core.lease.Lease;
import net.jini.core.lookup.ServiceID;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.export.Exporter;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.entry.Location;
import net.jini.lookup.entry.Name;
import net.jini.security.TrustVerifier;
import net.jini.space.JavaSpace05;
import sorcer.core.AccessDeniedException;
import sorcer.core.Provider;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.core.SorcerNotifierProtocol;
import sorcer.core.UEID;
import sorcer.core.UnknownExertionException;
import sorcer.core.context.ContextManagement;
import sorcer.core.context.Contexts;
import sorcer.core.context.ServiceContext;
import sorcer.core.dispatch.JobThread;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.exertion.NetTask;
import sorcer.core.exertion.ObjectJob;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.core.misc.MsgRef;
import sorcer.core.provider.ServiceProvider.ProxyVerifier;
import sorcer.core.provider.jobber.ServiceJobber;
import sorcer.core.provider.logger.RemoteHandler;
import sorcer.core.provider.proxy.Partnership;
import sorcer.core.provider.proxy.ProviderProxy;
import sorcer.core.signature.NetSignature;
import sorcer.jini.jeri.SorcerILFactory;
import sorcer.jini.lookup.entry.SorcerServiceInfo;
import sorcer.security.sign.SignedServiceTask;
import sorcer.security.sign.SignedTaskInterface;
import sorcer.security.sign.TaskAuditor;
import sorcer.service.*;
import sorcer.service.space.SpaceAccessor;
import sorcer.service.txmgr.TransactionManagerAccessor;
import sorcer.util.*;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.ExportException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static sorcer.service.SignatureFactory.sig;
import static sorcer.core.SorcerConstants.*;

/**
 * The provider delegate implements most of the intialization and configuration
 * of service providers by dependency injection.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProviderDelegate {

	static ThreadGroup threadGroup = new ThreadGroup(PROVIDER_THREAD_GROUP);

	static final int TRY_NUMBER = 5;
	
	// service class loader
	private ClassLoader implClassLoader;

	// visited exertion for forwardExertion to check for potential looping
	private static Set visited;

	private static final Logger logger = Log.getProviderLog();

	private Logger remoteLogger;

	/** Provider logger used in custom provider methods */
	private Logger providerLogger;

	/** Context logger used in custom provider methods */
	private Logger contextLogger;

	/** Provider deployment configuration. */
	protected DeploymentConfiguration config = new DeploymentConfiguration();

	/** The unique ID for this server proxy verification. */
	private Uuid serverUuid;

	protected String[] groupsToDiscover;

	protected JavaSpace05 space;

	protected TransactionManager tManager;

	protected boolean workerTransactional = false;

	protected String spaceGroup;

	protected String spaceName;

	protected Class[] publishedServiceTypes;

	/** provider service type entry used to be included in the provider's proxy. */
	protected SorcerServiceInfo serviceType;

	protected boolean idPersistent = false;

	/** if true then we match all entries with interface names only. */
	protected boolean matchInterfaceOnly = false;

	/** if true then its provider can be monitored for its exerting behavior. */
	protected boolean monitorable = false;

	/** if true then its provider can produce notification. */
	protected boolean notifying = false;

	/* use Spacer workers, when false no space computing support. */
	protected boolean spaceEnabled = false;

	protected boolean spaceReadiness = false;

	protected boolean spaceSecurityEnabled = false;

	private ThreadGroup namedGroup, interfaceGroup;

	private int workerCount = 10;

    private int queueSize = 0;

	private int maximumPoolSize = 20;

	private List<ExecutorService> spaceHandlingPools;

    protected Provider provider;

	protected boolean mutualExclusion = true;

	// all exported services with corresponding exporter
	// <Remote, Exporter> or <service bean, service provider>
	private static Map exports = new HashMap();

	protected Remote providerProxy;

	private long eventID = 0, seqNum = 0;

	private List<Entry> extraLookupAttributes = new Vector<Entry>();

	/** Map of exertion ID's and state of execution */
	private static final Map exertionStateTable = Collections
			.synchronizedMap(new HashMap(11));
	/**
	 * A smart proxy instance
	 */
	private Object smartProxy = null;

	/**
	 * A {@link java.rmi.Remote} partner object expending functionality of this provider.
	 * The provider's inner proxy can be used by the outer proxy of this
	 * provider to make remote redirectional calls on this partner.
	 */
	private Remote partner = null;

	/**
	 * A remote inner proxy implements Remote interface. Usually outer proxy
	 * complements its functionality by invoking remote calls on the inner proxy
	 * server. Thus, inner proxy can make remote calls on another service
	 * provider, for example {@link sorcer.service.Service#service), while the
	 * outer proxy still can call directly on the originating service provider.
	 */
	private Remote innerProxy = null;

	/**
	 * An outer service proxy, by default the proxy of this provider, is used
	 * from by service requestors if provider's smart proxy is absent. At least
	 * two generic Remote interface: {@link sorcer.service.Service} and {@link sorcer.core.Provider} are
	 * implemented by outer proxies of all SORCER service providers. Each SORCER
	 * provider uses outer proxy to actually call directly its provider and make
	 * redirected calls using its inner proxy (redirected remote invocations).
	 * Any method of not Remote interface implemented by a SORCER service
	 * provider can be invoked via the Service remote interface,
	 * {@link sorcer.service.Service#service} - recommended approach. That
	 * provider's direct invocation method is embedded into a service method of
	 * the provided exertion.
	 */
	private Remote outerProxy = null;

	/** The exporter for exporting and unexporting outer proxy */
	private Exporter outerExporter;

    /** The exporter for exporting and unexporting inner proxy */
	private Exporter partnerExporter;

	/**
	 * The admin proxy handles the standard Jini Admin interface.
	 */
	private Remote adminProxy;

	/**
	 * SORCER service beans instantiated by this delegate
	 */
	private Object[] serviceBeans;

	/**
	 * Exposed service type components. A key is an interface and a value its
	 * implementing service-object.
	 */
	private Map serviceComponents;

	private String hostName, hostAddress;

	private ContextManagement contextManager;

	/*
	 * A nested class to hold the state information of the executing thread for
	 * a served exertion.
	 */
	public static class ExertionSessionInfo {

		static LeaseRenewalManager lrm = new LeaseRenewalManager();

		private static class ExertionSessionBundle {
			public Uuid exertionID;
			public MonitoringSession session;
		}

		private static final ThreadLocal<ExertionSessionBundle> tl = new ThreadLocal<ExertionSessionBundle>() {
			@Override
			protected ExertionSessionBundle initialValue() {
				return new ExertionSessionBundle();
			}
		};

		public static void add(ServiceExertion ex) {
			ExertionSessionBundle esb = tl.get();
			esb.exertionID = ex.getId();
			esb.session = ex.getMonitorSession();
			if (ex.getMonitorSession() != null)
				lrm.renewUntil(
						ex.getMonitorSession().getLease(),
						Lease.ANY, null);
		}

		public static MonitoringSession getSession() {
			ExertionSessionBundle esb = tl.get();
			return (esb != null) ? esb.session : null;
		}

        public static void removeLease() {
			ExertionSessionBundle esb = tl.get();
			try {
				lrm.remove(esb.session.getLease());
			} catch (Exception e) {
			}
		}
	}

	public ProviderDelegate() {
	}

	public void init(Provider provider) throws RemoteException,
			ConfigurationException {
		init(provider, null);
	}

	public void init(Provider provider, String configFilename)
			throws RemoteException, ConfigurationException {
		this.provider = provider;
		// This allows us to specify different properties for different hosts
		// using a shared mounted filesystem
		restore();
		// set provider's ID persistance flag if defined in provider's
		// properties
		idPersistent = SorcerEnv.getProperty(P_SERVICE_ID_PERSISTENT, "false")
				.equals("true");
		// set provider join groups if defined in provider's properties
		groupsToDiscover = SorcerEnv.getLookupGroups();
		logger.info("ServiceProvider:groups to discover="
                + StringUtils.arrayToString(groupsToDiscover));
		// set provider space group if defined in provider's properties
		spaceGroup = config.getProperty(J_SPACE_GROUP, SorcerEnv.getSpaceGroup());
		// set provider space name if defined in provider's properties
		spaceName = config.getProperty(J_SPACE_NAME,
                SorcerEnv.getActualSpaceName());

		Class[] serviceTypes = new Class[0];
		try {
			serviceTypes = (Class[]) config.jiniConfig.getEntry(
					ServiceProvider.PROVIDER, J_INTERFACES, Class[].class);
		} catch (ConfigurationException e) {
			// do nothing, used the default value
			// e.printStackTrace();
		}
		if ((serviceTypes != null) && (serviceTypes.length > 0)) {
			publishedServiceTypes = serviceTypes;
			logger.info("*** published services: "
					+ Arrays.toString(publishedServiceTypes));
		}
	}

	void initSpaceSupport() throws ConfigurationException, RemoteException {
		try {
			hostName = SorcerEnv.getLocalHost().getHostName();
			hostAddress = SorcerEnv.getHostAddress();
		} catch (UnknownHostException e) {
			// ignore it
		}
		if (!spaceEnabled){
			return;
		}
		space = SpaceAccessor.getSpace(spaceName, spaceGroup);
		if (space == null) {
			int ctr = 0;
			while (space == null && ctr++ < TRY_NUMBER) {
				logger.warning("could not get space, trying again... try number = "
						+ ctr);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				space = SpaceAccessor.getSpace(spaceName, spaceGroup);
			}
			if (space != null) {
				logger.info("got space = " + space);
			} else {
				logger.warning("***warn: could not get space...moving on.");
			}
		}
		if (workerTransactional)
			tManager = TransactionManagerAccessor.getTransactionManager();

		try {
			startSpaceTakers();
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Provider HALTED: Couldn't start Workers");
			provider.destroy();
		}
	}

	protected void configure(Configuration jconfig) throws ExportException {
		final Thread currentThread = Thread.currentThread();
		implClassLoader = currentThread.getContextClassLoader();
		Class partnerType;
		String partnerName;
		boolean remoteContextLogging = false;

		try {
			remoteContextLogging = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, REMOTE_CONTEXT_LOGGING,
					boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}
		if (remoteContextLogging) {
			initContextLogger();
		}

		boolean remoteProviderLogging = false;
		try {
			remoteProviderLogging = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, REMOTE_PROVIDER_LOGGING,
					boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}
		if (remoteProviderLogging) {
			initProviderLogger();
		}

		boolean remoteLogging = false;
		try {
			remoteLogging = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, REMOTE_LOGGING, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}
		if (remoteLogging) {
			String managerName, loggerName;
			try {
				managerName = (String) jconfig.getEntry(
						ServiceProvider.COMPONENT, REMOTE_LOGGER_MANAGER_NAME,
						String.class, "*");
				Level level = (Level) jconfig.getEntry(
						ServiceProvider.COMPONENT, REMOTE_LOGGER_LEVEL,
						Level.class, Level.ALL);
				loggerName = (String) jconfig.getEntry(
						ServiceProvider.COMPONENT, REMOTE_LOGGER_NAME,
						String.class,
						"remote.sorcer.provider-" + provider.getProviderName());

				initRemoteLogger(level, managerName, loggerName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			monitorable = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT,
					PROVIDER_MONITORING, boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			notifying = (Boolean) jconfig.getEntry(ServiceProvider.COMPONENT,
					PROVIDER_NOTIFYING, boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			mutualExclusion = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, MUTUAL_EXCLUSION, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			matchInterfaceOnly = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, INTERFACE_ONLY, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			spaceEnabled = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, SPACE_ENABLED, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}
		try {
			workerCount = (Integer) jconfig.getEntry(ServiceProvider.PROVIDER,
					WORKER_COUNT, int.class, 10);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			queueSize = (Integer) jconfig.getEntry(ServiceProvider.PROVIDER,
					SPACE_WORKER_QUEUE_SIZE, int.class, 0);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			maximumPoolSize = (Integer) jconfig.getEntry(
					ServiceProvider.PROVIDER, MAX_WORKER_POOL_SIZE, int.class,
					20);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			spaceReadiness = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, SPACE_READINESS, boolean.class,
					false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			workerTransactional = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, WORKER_TRANSACTIONAL,
					boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			spaceSecurityEnabled = (Boolean) jconfig.getEntry(
					ServiceProvider.COMPONENT, SPACE_SECURITY_ENABLED,
					boolean.class, false);
		} catch (Exception e) {
			// do nothing, default value is used
			e.printStackTrace();
		}

		try {
			contextManager = (ContextManagement) jconfig.getEntry(
					ServiceProvider.COMPONENT, CONTEXT_MANAGER,
					ContextManagement.class, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("*** assigned dataContext manager: " + contextManager);

		try {
			partnerType = (Class) jconfig.getEntry(ServiceProvider.COMPONENT,
					SERVER_TYPE, Class.class, null);
		} catch (Exception e) {
			e.printStackTrace();
			partnerType = null;
		}
		try {
			partnerName = (String) jconfig.getEntry(ServiceProvider.COMPONENT,
					SERVER_NAME, String.class, null);
		} catch (Exception e) {
			e.printStackTrace();
			partnerName = null;
		}
		try {
			partner = (Remote) jconfig.getEntry(ServiceProvider.COMPONENT,
					SERVER, Remote.class, null);
			logger.info("partner=" + partner);
		} catch (Exception e) {
			e.printStackTrace();
			partnerName = null;
		}
		if (partner != null) {
			getPartner(partnerName, partnerType);
			exports.put(partner, partnerExporter);
		}
		// get exporters for outer and inner proxy
		getExporters(jconfig);
		logger.fine("exporting provider: " + provider);
		logger.info("outerExporter = " + outerExporter);
		try {
			if (outerExporter == null) {
					logger.severe("No exporter for provider:" + getProviderName());
					return;
				}
				outerProxy = (Remote) ProviderProxy.wrapServiceProxy(
					outerExporter.export(provider), getServerUuid());
				logger.fine("outerProxy: " + outerProxy);
		} catch (Exception ee) {
			logger.throwing(ProviderDelegate.class.getName(), "deploymnet failed", ee);
		}
		providerProxy = outerProxy;
		adminProxy = outerProxy;
		exports.put(outerProxy, outerExporter);
		logger.fine(">>>>>>>>>>> exported outerProxy: \n" + outerProxy
				+ ", outerExporter: \n" + outerExporter);

		logger.info("PROXIES >>>>> provider: " + providerProxy + "\nsmart: "
				+ smartProxy + "\nouter: " + outerProxy + "\ninner: "
				+ innerProxy);
	}

	private void initThreadGroups() {
		namedGroup = new ThreadGroup("Provider Group: " + getProviderName());
		namedGroup.setDaemon(true);
		namedGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);
		interfaceGroup = new ThreadGroup("Interface Threads");
		interfaceGroup.setDaemon(true);
		interfaceGroup.setMaxPriority(Thread.NORM_PRIORITY - 1);
	}

	public void startSpaceTakers() throws ConfigurationException, RemoteException {
		ExecutorService spaceWorkerPool;
		spaceHandlingPools = new ArrayList<ExecutorService>();
		String msg;
		if (space == null) {
			msg = "ERROR: No space found, spaceName = " + spaceName
					+ ", spaceGroup = " + spaceGroup;
			logger.severe(msg);
		}
		if (workerTransactional && tManager == null) {
			msg = "ERROR: no transactional manager found....";
			logger.severe(msg);
		}
		if (publishedServiceTypes == null || publishedServiceTypes.length == 0) {
			msg = "ERROR: no published interfaces found....";
			logger.severe(msg);
		}

		initThreadGroups();
		ExertionEnvelop envelop;
		LokiMemberUtil memberInfo = null;
		if (spaceSecurityEnabled) {
			memberInfo = new LokiMemberUtil(ProviderDelegate.class.getName());
		}

		logger.finer("*** provider worker count: " + workerCount
				+ ", spaceTransactional: " + workerTransactional);
		logger.info("publishedServiceTypes.length = "
				+ publishedServiceTypes.length);
		logger.info(Arrays.toString(publishedServiceTypes));

		// create a pair of taker threads for each published interface
		SpaceTaker worker;

		// make sure that the number of core threads equals the maximum number
		// of threads
		if (queueSize == 0) {
			if (maximumPoolSize > workerCount)
				workerCount = maximumPoolSize;
		}
		for (int i = 0; i < publishedServiceTypes.length; i++) {
			// spaceWorkerPool = Executors.newFixedThreadPool(workerCount);
			spaceWorkerPool = new ThreadPoolExecutor(workerCount,
					maximumPoolSize > workerCount ? maximumPoolSize
							: workerCount, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>(
							(queueSize == 0 ? workerCount : queueSize)));
			spaceHandlingPools.add(spaceWorkerPool);
			// SORCER.ANY is required for a ProviderWorker
			// to avoid matching to any provider name
			// that is Java null matching everything
			envelop = ExertionEnvelop.getTemplate(publishedServiceTypes[i],
					SorcerConstants.ANY);
			if (spaceReadiness) {
				worker = new SpaceIsReadyTaker(new SpaceTaker.SpaceTakerData(
						envelop, memberInfo, provider, spaceName, spaceGroup,
						workerTransactional, queueSize == 0), spaceWorkerPool);
			} else {
				worker = new SpaceTaker(new SpaceTaker.SpaceTakerData(envelop,
						memberInfo, provider, spaceName, spaceGroup,
						workerTransactional, queueSize == 0), spaceWorkerPool);
			}
			Thread sith = new Thread(interfaceGroup, worker);
			sith.setDaemon(true);
			sith.start();
			logger.info("*** space worker-" + i + " started for: "
					+ publishedServiceTypes[i]);
			// System.out.println("space template: " +
			// envelop.describe());

			if (!matchInterfaceOnly) {
				// spaceWorkerPool = Executors.newFixedThreadPool(workerCount);
				spaceWorkerPool = new ThreadPoolExecutor(workerCount,
						maximumPoolSize > workerCount ? maximumPoolSize
								: workerCount, 0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>(
								(queueSize == 0 ? workerCount : queueSize)));
				spaceHandlingPools.add(spaceWorkerPool);
				envelop = ExertionEnvelop.getTemplate(publishedServiceTypes[i],
						getProviderName());
				if (spaceReadiness) {
					worker = new SpaceIsReadyTaker(
							new SpaceTaker.SpaceTakerData(envelop, memberInfo,
									provider, spaceName, spaceGroup,
									workerTransactional, queueSize == 0),
							spaceWorkerPool);
				} else {
					worker = new SpaceTaker(new SpaceTaker.SpaceTakerData(
							envelop, memberInfo, provider, spaceName,
							spaceGroup, workerTransactional, queueSize == 0),
							spaceWorkerPool);
				}
				Thread snth = new Thread(namedGroup, worker);
				snth.setDaemon(true);
				snth.start();
				logger.info("*** named space worker-" + i + " started for: "
						+ publishedServiceTypes[i] + ":" + getProviderName());
				// System.out.println("space template: " +
				// envelop.describe());
			}
		}
		// interfaceGroup.list();
		// namedGroup.list();
	}

	public Task doTask(Task task, Transaction transaction)
			throws ExertionException, SignatureException, RemoteException,
			ContextException {
        // prepare a default net batch task (has all sigs of SRV type)
        // and make the last signature as master SRV type only.
        List<Signature> alls = task.getSignatures();
        Signature lastSig = alls.get(alls.size() - 1);
        if (alls.size() > 1 && task.isBatch()
                && (lastSig instanceof NetSignature)) {
            for (int i = 0; i < alls.size() - 1; i++) {
                alls.get(i).setType(Signature.PRE);
            }
        }
		task.getControlContext().appendTrace(
				provider.getProviderName() + " execute: "
						+ task.getProcessSignature().getSelector() + ":"
						+ task.getProcessSignature().getServiceType() + ":"
						+ getHostName());

		if (task instanceof SignedTaskInterface) {
			try {
				new TaskAuditor().audit((SignedServiceTask) task);
				task = (Task) ((SignedTaskInterface) task).getObject();
			} catch (Exception e) {
				logger.severe("Exception while retrieving SIGNED TASK" + e);
				e.printStackTrace();
			}
		}

		String providerId = task.getProcessSignature().getProviderName();
		/*
		 * String actions = task.method.action(); GuardedObject go = new
		 * GuardedObject(task.method, new ServiceMethodPermission(task.userID,
		 * actions)); try { Object o = go.getObject(); Util.debug(this, "Got
		 * access to method: " + actions); } catch (AccessControlException ace)
		 * { throw new ExertionMethodException ("Can't access method: " +
		 * actions); }
		 */
		if (isValidTask(task)) {
			try {
				task.startExecTime();
				exertionStateTable.put(task.getId(), ExecState.RUNNING);
				if (((ServiceProvider) provider).isValidTask(task)) {
					// preprocessing
					if (task.getPreprocessSignatures().size() > 0) {
						Context cxt = preprocess(task);
						cxt.setExertion(task);
						task.setContext(cxt);
						task.setServicer(provider);
					}
					// service sig processing
					NetSignature tsig = (NetSignature) task
							.getProcessSignature();
                    // rest path prefix and return path
                    if (tsig.getPrefix() != null)
                        ((ServiceContext)task.getContext()).setPrefix(tsig.getPrefix());
					if (tsig.getReturnPath() != null)
					    ((ServiceContext) task.getContext())
									.setReturnPath(tsig.getReturnPath());

					if (isBeanable(task)) {
						task = useServiceComponents(task);
					} else {
						logger.info("going to execTask(); transaction = "
								+ transaction);
						task = execTask(task);
						logger.info("DONE going to execTask(); transaction = "
								+ transaction);
					}
					// postprocessing
					logger.info("postprocessing task...transaction = "
							+ transaction);
					if (task.getPostprocessSignatures().size() > 0) {
						Context cxt = postprocess(task);
						cxt.setExertion(task);
						task.setContext(cxt);
						task.setServicer(provider);
					}
					confirmExec(task);
					task.stopExecTime();
					logger.info("provider name = " + provider.getDescription()
							+ "\nreturing task; transaction = " + transaction);
					return task;
				} else {
					provider.fireEvent();
					task.stopExecTime();
					ExertionException ex = new ExertionException(
							"Unacceptable task received, requested provider: "
									+ providerId + " Name:" + task.getName());
					task.reportException(ex);
					task.setStatus(ExecState.FAILED);
					return (Task) forwardTask(task, provider);
				}
			} finally {
				exertionStateTable.remove(exertionStateTable.remove(task
						.getId()));
			}
		}
		return (Task) forwardTask(task, provider);
	}

	private Context preprocess(Task task) throws ExertionException,
			SignatureException {
		return processContinousely(task, task.getPreprocessSignatures());
	}

	private Context postprocess(Task task) throws ExertionException,
			SignatureException {
		return processContinousely(task, task.getPostprocessSignatures());
	}

	private Context processContinousely(Task task, List<Signature> signatures)
			throws ExertionException, SignatureException {
		Signature.Type st = signatures.get(0).getType();

		ObjectJob job = new ObjectJob(signatures.get(0).getType() + "-"
				+ task.getName(), sig("execute", ServiceJobber.class));
		Task t;
		Signature ss;
		for (int i = 0; i < signatures.size(); i++) {
			ss = signatures.get(i);
			if (ss instanceof NetSignature)
				((NetSignature) ss).setServicer(provider);
			try {
				t = Task.newTask(task.getName() + "-" + i, ss,
						task.getContext());
				ss.setType(Signature.SRV);
				((ServiceContext) task.getContext()).setCurrentSelector(ss
						.getSelector());
                if (ss.getPrefix() != null)
                    ((ServiceContext)task.getContext()).setPrefix(ss.getPrefix());
                if (ss.getReturnPath() != null)
                    ((ServiceContext)task.getContext()).setReturnPath(ss.getReturnPath());
				t.setContinous(true);
			} catch (Exception e) {
				e.printStackTrace();
				resetSigantures(signatures, st);
				throw new ExertionException(e);
			}
			job.addExertion(t);
		}
		Exertion result;
		try {
			// result = sj.exert();
			JobThread jobThread = new JobThread(job, provider);
			jobThread.start();
			jobThread.join();
			result = jobThread.getResult();
			// logger.info("<==== JobThread result: " + result);
		} catch (Exception e) {
			e.printStackTrace();
			resetSigantures(signatures, st);
			throw new ExertionException(e);
		}
		// append accumulated exceptions and trace
		task.getExceptions().addAll(result.getExceptions());
		task.getTrace().addAll(result.getTrace());
		if (result.getStatus() <= ExecState.FAILED) {
			task.setStatus(ExecState.FAILED);
			ExertionException ne = new ExertionException(
					"Batch signatures failed: " + signatures);
			task.reportException(ne);
			resetSigantures(signatures, st);
			throw ne;
		}
		// return the service context of the last exertion
		resetSigantures(signatures, st);
        return result.getExertions().get(job.size() - 1).getContext();
	}

	private void resetSigantures(List<Signature> signatures, Signature.Type type) {
		for (int i = 0; i < signatures.size(); i++) {
			signatures.get(i).setType(type);
		}
	}

	private void confirmExec(Task task) {
		String pn;
		try {
			pn = getProviderName();
			if (pn == null || pn.length() == 0)
				pn = getDescription();
			Contexts.putOutValue(task.getContext(), TASK_PROVIDER, pn + "@"
					+ hostName + ":" + hostAddress);
		} catch (ContextException ex) {
			// ignore
		} catch (RemoteException e) {
			// local call
		}
	}

	private boolean isBeanable(Task task) {
		if (serviceComponents == null || serviceComponents.size() == 0)
			return false;
		Class serviceType = task.getProcessSignature().getServiceType();
		Iterator i = serviceComponents.entrySet().iterator();
		Map.Entry next;
        while (i.hasNext()) {
			next = (Map.Entry) i.next();
			logger.fine("mach serviceType: " + serviceType + " against: "
					+ next.getKey());
			// check declared interfaces
			if (next.getKey() == serviceType)
				return true;

			// check implemented interfaces
			Class[] supertypes = ((Class)next.getKey()).getInterfaces();			
			for (Class st : supertypes) {
				logger.fine("mach serviceType: " + serviceType 
						+ " against: " +  st);
				if (st == serviceType)
					return true;
			}
		}
		return false;
	}

	private Task useServiceComponents(Task task)
			throws RemoteException, ContextException {
		String selector = task.getProcessSignature().getSelector();
		Class serviceType = task.getProcessSignature().getServiceType();
		Iterator i = serviceComponents.entrySet().iterator();
		Map.Entry next;
		Object impl = null;
		while (i.hasNext()) {
			next = (Map.Entry) i.next();
			if (next.getKey() == serviceType) {
				impl = next.getValue();
				break;
			}
			Class[] supertypes = ((Class)next.getKey()).getInterfaces();			
			for (Class st : supertypes) {
				if (st == serviceType) {
					impl = next.getValue();
					break;
				}
			}
        }
		if (impl != null) {
			if (task.getProcessSignature().getReturnPath() != null) {
				((ServiceContext) task.getContext()).setReturnPath(task
					.getProcessSignature().getReturnPath());
			}
			// determine args and parameterTpes from the context
			Class[] argTypes = new Class[] { Context.class };
			ServiceContext cxt = (ServiceContext) task.getContext();
			boolean isContextual = true;
			if (cxt.getParameterTypes() != null & cxt.getArgs() != null) {
				argTypes = cxt.getParameterTypes();
				isContextual = false;
			} 
			Method m;
			try {
				// select the proper method for the bean type
				if (selector.equals("invoke") && impl instanceof Exertion) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Context.class, Parameter[].class });
					isContextual = true;
				} else if (selector.equals("exert") && impl instanceof ExertProcessor) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Exertion.class, Parameter[].class });
					isContextual = false;
				} else if (selector.equals("getValue") && impl instanceof Evaluation) {
					m = impl.getClass().getMethod(selector,
							new Class[] { Parameter[].class });
					isContextual = false;
				} else
					m = impl.getClass().getMethod(selector, argTypes);
				logger.info("Executing service bean method: " + m + " by: "
						+ config.getProviderName());
				task.getContext().setExertion(task);
				((ServiceContext) task.getContext())
						.setCurrentSelector(selector);
                String pf = task.getProcessSignature().getPrefix();
                if (pf != null)
			    	((ServiceContext) task.getContext()).setCurrentPrefix(pf);
				
				Context result;
				if (isContextual) 
					result = execContextualBean(m, task, impl);
				else
					result = execParametricBean(m, task, impl);

                ReturnPath rp = result.getReturnPath();
                if (rp != null) {
                    ((ServiceContext)result).setReturnPath(rp.path);
                }
				// clear task in the context
				result.setExertion(null);
				task.setContext(result);
				task.setStatus(ExecState.DONE);
				return task;
			} catch (Exception e) {
				task.reportException(e);
				e.printStackTrace();
			}
		}
		task.setStatus(ExecState.FAILED);
		return task;
	}

	private Context execContextualBean(Method m, Task task,
			Object impl) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, ContextException {
		Context result;
		String selector = task.getProcessSignature().getSelector();
		Object[] args = new Object[] { task.getContext() };
		if(selector.equals("invoke") && impl instanceof Exertion) {
			Exertion xrt = (Exertion) m.invoke(impl,
                    args[0], new Parameter[] {});
			if (xrt.isJob())
				result = ((Job)xrt).getJobContext();
			else
				result = xrt.getContext();
			task.getControlContext().getExceptions().addAll(xrt.getExceptions());
			task.getTrace().addAll(xrt.getTrace());
		} else {
			result = (Context) m.invoke(impl, args);
		}
		return result;
	}
	
	private Context execParametricBean(Method m, Task task,
			Object impl) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException, ContextException {
		Context result = task.getContext();
		String selector = task.getProcessSignature().getSelector();
		Class[] argTypes = ((ServiceContext)result).getParameterTypes();
		Object[] args = (Object[]) ((ServiceContext)result).getArgs();
		if (selector.equals("exert") && impl instanceof ExertProcessor) {
			Exertion xrt;
			if (args.length == 1) {
				xrt = (Exertion) m.invoke(impl, args[0],
                        new Parameter[] {});
			} else {
				xrt = (Exertion) m.invoke(impl, args);
			}
			if (xrt.isJob())
				result = ((Job) xrt).getJobContext();
			else
				result = xrt.getContext();
			task.getControlContext().getExceptions()
					.addAll(xrt.getExceptions());
			task.getTrace().addAll(xrt.getTrace());
			//((ServiceContext) result).setReturnValue(result);

		} else if (selector.equals("getValue") && impl instanceof Evaluation) {
			Object obj;
			if (argTypes == null) {
				obj = m.invoke(impl, new Object[] { new Parameter[] {} });
			} else {
				obj = m.invoke(impl, args);
			}
			result.setReturnValue(obj);
		} else {
			result.setReturnValue(m.invoke(impl, args));
		}
		return result;
	}

	protected ServiceExertion forwardTask(ServiceExertion task,
			Provider requestor) throws ExertionException,
			RemoteException, SignatureException, ContextException {
		// check if we do not look with the same exertion
		Service recipient = null;
		String prvName = task.getProcessSignature().getProviderName();
		NetSignature fm = (NetSignature) task.getProcessSignature();
		ServiceID serviceID = fm.getServiceID();
		Class<Service> prvType = (Class<Service>) fm.getServiceType();
		logger.info("ProviderDelegate#forwardTask \nprvType: " + prvType
				+ "\nprvName = " + prvName);

		if (visited == null)
			visited = new HashSet();

		if (visited.contains(serviceID)) {
			visited.remove(serviceID);
			throw new ExertionException("Not able to get relevant type: "
					+ prvType + ", name: " + prvName);
		}
		visited.add(serviceID);
		if (serviceComponents != null) {
			NetTask result = (NetTask) useServiceComponents((Task) task);
			logger.info("forwardTask executed by a service bean: " + result);
			if (result != null) {
				visited.remove(serviceID);
				return result;
			} else {
				task.setStatus(ExecState.ERROR);
				return task;
			}
		}
		if (serviceID != null)
			recipient = (Service) Accessor.getService(serviceID);
		else if (prvType != null && prvName != null) {
			recipient = Accessor.getService(prvName, prvType);
		} else if (prvType != null) {
			recipient = Accessor.getService(null, prvType);
		}
		if (recipient == null) {
			visited.remove(serviceID);
			ExertionException re = new ExertionException(
					"Not able to get provider type: " + prvType + ", name: "
							+ prvName);
			notifyException(task, "", re);
			throw re;
		} else if (recipient.getClass().getName()
				.startsWith(requestor.getClass().getName())) {
			visited.remove(serviceID);
			ExertionException re = new ExertionException(
					"Invalid task for provider type: " + prvType + ", name: "
							+ prvName + " " + task.toString());
			notifyException(task, "", re);
			throw re;
		} else
			try {
				Task result = (Task) recipient.service(task, null);
				if (result != null) {
					visited.remove(serviceID);
					return result;
				} else {
					visited.remove(serviceID);
					throw new ExertionException(
							"Not able to get relevant type: " + prvType
									+ ", name: " + prvName);
				}
			} catch (TransactionException te) {
				visited.remove(serviceID);
				throw new ExertionException("transaction failure", te);
			}
	}

	public ServiceExertion dropTask(Exertion entryTask)
			throws ExertionException, SignatureException, RemoteException {
		return null;
	}

    public Job dropJob(Job job) throws RemoteException, ExertionException {
		return null;
	}

	public void hangup() throws RemoteException {
		String str = config.getProperty(P_DELAY_TIME);
		if (str != null) {
			try {
				// delay is in seconds
				int delay = Integer.parseInt(str);
				Thread.sleep(delay * 1000);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	public boolean isValidMethod(String name) throws RemoteException {
		// modify name for SORCER providers
		Method[] methods = provider.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(name))
				return true;
		}
		return false;
	}

	public Task execTask(Task task) throws ExertionException,
			SignatureException, RemoteException {
		ServiceContext cxt = (ServiceContext) task.getContext();
		try {
			if (cxt.isValid(task.getProcessSignature())) {
				Signature sig = task.getProcessSignature();
				if (sig.getReturnPath() != null)
					cxt.setReturnPath(sig.getReturnPath());

				cxt.setCurrentSelector(sig.getSelector());
				cxt.setCurrentPrefix(sig.getPrefix());

				cxt.setExertion(task);
				task.setServicer(provider);

				if (sig instanceof NetSignature)
					((NetSignature) sig).setServicer(provider);
				task.setStatus(ExecState.FAILED);
				logger.info("DELEGATE EXECUTING TASK: " + task + " by sig: "
						+ task.getProcessSignature() + " for context: " + cxt);
				cxt = (ServiceContext) invokeMethod(sig.getSelector(), cxt);
				logger.info("doTask: TASK DONE BY DELEGATE OF ="
						+ provider.getProviderName());
				task.setContext(cxt);
				task.setStatus(ExecState.DONE);
				logger.info("CONTEXT GOING OUT: " + cxt);
                if (cxt.getReturnPath() != null)
                    cxt.setReturnValue(cxt.getValue(cxt.getReturnPath().path));
                // clear the exertion and the context
                cxt.setExertion(null);
                task.setServicer(null);
			}
		} catch (ContextException e) {
			throw new ExertionException(e);
		}
		return task;
	}

	public Exertion invokeMethod(String selector, Exertion ex)
			throws ExertionException {
		Class[] argTypes = new Class[] { Exertion.class };
		try {
			Method m = provider.getClass().getMethod(selector, argTypes);
			logger.info("Executing method: " + m + " by: "
					+ config.getProviderName());

            return (Exertion) m.invoke(provider, ex);
		} catch (Exception e) {
			ex.getControlContext().addException(e);
			throw new ExertionException(e);
		}
	}

	public Context invokeMethod(String selector, Context sc)
			throws ExertionException {
		try {
			Class[] argTypes = new Class[] { Context.class };
			Object[] args = new Object[] { sc };
			ServiceContext cxt = (ServiceContext) sc;
			boolean isContextual = true;
			if (cxt.getParameterTypes() != null & cxt.getArgs() != null) {
				argTypes = cxt.getParameterTypes();
				args = (Object[]) cxt.getArgs();
				isContextual = false;
			}
			Method execMethod = provider.getClass().getMethod(selector,
					argTypes);
			Context result;
			if (isContextual) {
				result = (Context) execMethod.invoke(provider, args);

                 // Setting Return Values
                if (result.getReturnPath() != null) {
                    Object resultValue = result.getValue(result.getReturnPath().path);
                    result.setReturnValue(resultValue);

                    // do this only if the result value is null
                    /*if (resultValue==null || (resultValue!=null && (resultValue.equals(Context.Value.NULL)))) {
                        logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!! Setting return value to return path: " + result.getReturnPath() + " from outPaths !!!!!!!!!!!!!");
                        String outPath = null;
                        for (Object path : ((ServiceContext)result).getOutPaths()) {
                            if (path.toString().contains(result.getPrefix())) {
                                outPath = path.toString();
                                break;
                            }
                        }
                        logger.info("!!!!!!!!!!!!!!!!!!!!!! Setting return value from path: " + outPath + " value: " + result.get(outPath).toString());
                        result.setReturnValue(result.get(outPath));
                    } */
                }
			} else {
				sc.setReturnValue(execMethod.invoke(
                        provider, args));
				result = sc;
			}
			return result;
			// }
		} catch (Exception e) {
			throw new ExertionException(e);
		}
	}

    /**
	 * Returns a directory for provider's scratch files containing service
	 * dataContext values.
	 * 
	 * @return a scratch directory
	 */
	public File getScratchDir() {
		return SorcerEnv.getNewScratchDir();
	}

	public File getScratchDir(String scratchDirNamePrefix) {
		return SorcerEnv.getNewScratchDir(scratchDirNamePrefix);
	}

	// adds scratch dir to dataContext
	public File getScratchDir(Context context, String scratchDirPrefix)
			throws ContextException, MalformedURLException {

		File scratchDir = getScratchDir(scratchDirPrefix);

		if (context.containsPath(SCRATCH_DIR_KEY)
				|| context.containsPath(SCRATCH_URL_KEY)) {
			// throw new ContextException(
			// "***error: dataContext already contains scratch dir or scratch url key; "
			// + "do not use this method twice on the same dataContext argument "
			// + "(use getScratchDir() and add scratch dir key and value "
			// + "yourself)");
			logger.warning("***warning: dataContext already contains scratch dir or scratch url key; "
					+ "beware of using this method twice on the same dataContext argument "
					+ "(using getScratchDir() and add scratch dir key and value "
					+ "yourself may be better)."
					+ "\n\tdataContext name = "
					+ context.getName() + "\n\tcontext = " + context);
		}

		Contexts.putOutValue(context, SCRATCH_DIR_KEY,
				scratchDir.getAbsolutePath(),
				SorcerEnv.getProperty("engineering.provider.scratchdir"));

		Contexts.putOutValue(context, SCRATCH_URL_KEY,
				getScratchURL(scratchDir),
                SorcerEnv.getProperty("engineering.provider.scratchurl"));

		return scratchDir;
	}

    /**
	 * Returns the URL of a data HTTP server handling remote scratch files.
	 * 
	 * @param scratchFile
	 * @return a URL of the data HTTP server
	 * 
	 * @throws java.net.MalformedURLException
	 */
	public URL getScratchURL(File scratchFile) throws MalformedURLException {
		return SorcerEnv.getScratchURL(scratchFile);
	}

	/**
	 * Returns a service type of the provider served by this delegate as
	 * registered with lookup services.
	 * 
	 * @return a SorcerServiceType
	 */
	public SorcerServiceInfo getServiceType() {
		return serviceType;
	}

	public Properties getProviderProperties() {
		return config.getProviderProperties();
	}

	public Configuration getProviderConfiguration() {
		return config.getProviderConfiguraion();
	}

	public String getDescription() throws RemoteException {
		return config.getProperty(P_DESCRIPTION);
	}

    public String[] getGroups() throws RemoteException {
		return groupsToDiscover;
	}

    public List<Object> getProperties() {
		List<Object> allAttributes = new ArrayList<Object>();
		Entry[] attributes = getAttributes();
		for (Entry entry : attributes)
			allAttributes.add(entry);
		allAttributes.add(config.getProviderProperties());
		allAttributes.add(SorcerEnv.getProperties());
		return allAttributes;
	}

	/**
	 * Creates the service attributes to be used with Jini lookup services.
	 * <p>
	 * This function will create the following entries:
	 * <ul>
	 * <li>A {@link net.jini.lookup.entry.Name}.
	 * <li>A {@link sorcer.jini.lookup.entry.SorcerServiceInfo}entry with all the information about this
	 * provider.
	 * <li>A main UIDescriptor if the provider overrides
	 * <li>Extra lookup attributes set via #addExtraLookupAttribute(Entry)
	 * {@link ServiceProvider#getMainUIDescriptor()}.
	 * </ul>
	 * 
	 * @return an array of Jini Service Entries.
	 */
	public Entry[] getAttributes() {
		final List<Entry> attrVec = new ArrayList<Entry>();

		try {
			// name of the provider suffixed in loadJiniConfiguration
			attrVec.add(new Name(getProviderName()));
			Entry sst = getSorcerServiceTypeEntry();
			attrVec.add(sst);
			// add additional entries declared in the Jini provider's
			// configuration
			Entry[] miscEntries = (Entry[]) config.jiniConfig.getEntry(
					ServiceProvider.PROVIDER, "entries", Entry[].class,
					new Entry[] {});
			for (int i = 0; i < miscEntries.length; i++) {
				attrVec.add(miscEntries[i]);
				// transfer location from entries if not defined in
				// SorcerServiceInfo
				if (miscEntries[i] instanceof Location
						&& ((SorcerServiceInfo) sst).location == null) {
					((SorcerServiceInfo) sst).location = "" + miscEntries[i];
				}
			}

			// add the service context of this provider to provider attributes
			// AccessControlContext context = AccessController.getContext();
		} catch (Exception ex) {
            logger.warning(StringUtils.stackTraceToString(ex));
		}

		attrVec.addAll(extraLookupAttributes);

		return attrVec.toArray(new Entry[attrVec.size()]);
	}

	/**
	 * Creates an entry that is a {@link sorcer.jini.lookup.entry.SorcerServiceInfo}.
	 * 
	 * @return an entry for the provider.
	 */
	private Entry getSorcerServiceTypeEntry() {
		SorcerServiceInfo serviceType = new SorcerServiceInfo();
		try {
			serviceType.providerName = config.getProviderName();
			serviceType.repository = config.getDataDir();
			serviceType.shortDescription = config.getProperty(P_DESCRIPTION);
			serviceType.location = config.getProperty(P_LOCATION);
            serviceType.groups = StringUtils.arrayToCSV(groupsToDiscover);
			serviceType.spaceGroup = spaceGroup;
			serviceType.spaceName = spaceName;
			serviceType.puller = spaceEnabled;
			serviceType.monitorable = monitorable;
			serviceType.matchInterfaceOnly = matchInterfaceOnly;
			serviceType.startDate = new Date().toString();
			serviceType.userDir = System.getProperty("user.dir");
			// serviceType.iGridHome = System.getProperty("iGrid.home");
			serviceType.userName = System.getProperty("user.name");
			serviceType.iconName = config.getIconName();

			if (publishedServiceTypes == null && spaceEnabled) {
				logger.severe(getProviderName()
						+ " does NOT declare its space interfaces");
				System.exit(1);
			}
			if (publishedServiceTypes != null) {
				String[] typeNames = new String[publishedServiceTypes.length];
				for (int i = 0; i < publishedServiceTypes.length; i++) {
					typeNames[i] = publishedServiceTypes[i].getName();
				}
				serviceType.publishedServices = typeNames;
			}
			serviceType.serviceID = provider.getProviderID();
		} catch (Exception ex) {
			logger.warning("Some problem in accessing attributes");
            logger.warning(StringUtils.stackTraceToString(ex));
		}
		String hostName = null, hostAddress = null;
		hostName = config.getProviderHostName();
		hostAddress = config.getProviderHostAddress();

		if (hostName != null) {
			serviceType.hostName = hostName;
		} else {
			logger.warning("Host is null!");
		}

		if (hostAddress != null) {
			serviceType.hostAddress = hostAddress;
		} else {
			logger.warning("Host address is null!!");
		}
		return serviceType;
	}

	/**
	 * Restores the ServiceID from {@link sorcer.core.SorcerConstants#S_SERVICE_ID_FILENAME}
	 * .
	 * <p>
	 * Please note: There is currently no method to save the ServiceID. So this
	 * method should probably be reworked.
	 */
	public void restore() {
		if (idPersistent) {
			try {
				// ObjectLogger.setResourceClass(this.getClass());
				this.setServerUuid((ServiceID) ObjectLogger.restore(SorcerEnv
						.getProperty(S_SERVICE_ID_FILENAME,
                                SorcerEnv.getServiceIdFilename())));
			} catch (Exception e) { // first time if exception caught
				e.printStackTrace();
            }
		}
	}

	private void ensureServerUuidIsSet() {
		if (serverUuid == null) {
			serverUuid = UuidFactory.generate();
		}
	}

	/**
	 * Retrieves the ServerUUID as an ServiceID.
	 * 
	 * @return a ServiceID representation of the ServerUUID.
	 */
	public ServiceID getServiceID() {
		ensureServerUuidIsSet();
		return new ServiceID(this.serverUuid.getMostSignificantBits(),
				this.serverUuid.getLeastSignificantBits());
	}

	/**
	 * Retrieves the Unique ID of this server.
	 * 
	 * @return the {@link net.jini.id.Uuid} of this server
	 */
	public Uuid getServerUuid() {
		ensureServerUuidIsSet();
		return serverUuid;
	}

	/**
	 * Sets the Uuid of this server from a given ServiceID.
	 * 
	 * @param serviceID
	 *            the ServiceID to use.
	 */
	public void setServerUuid(ServiceID serviceID) {
		logger.info("Setting service ID:" + serviceID);
		serverUuid = UuidFactory.create(serviceID.getMostSignificantBits(),
				serviceID.getLeastSignificantBits());
	}

    public void destroy() throws RemoteException {
		if (spaceEnabled && spaceHandlingPools != null) {
			for (ExecutorService es : spaceHandlingPools)
				shutdownAndAwaitTermination(es);
			if (interfaceGroup != null) {
				Thread[] ifgThreads = new Thread[interfaceGroup.activeCount()];
				Thread[] ngThreads = new Thread[namedGroup.activeCount()];
				interfaceGroup.enumerate(ifgThreads);
				namedGroup.enumerate(ngThreads);
				// System.out.println("ifgThreads.length = " +
				// ifgThreads.length);
				// System.out.println("ngThreads.length = " + ngThreads.length);
				for (int i = 0; i < ifgThreads.length; i++) {
					// System.out.println("ifgThreads[" + i + "] = " +
					// ifgThreads[i]);
					ifgThreads[i].interrupt();
				}
				for (int i = 0; i < ngThreads.length; i++) {
					// System.out.println("ngThreads[" + i + "] = " +
					// ngThreads[i]);
					ngThreads[i].interrupt();
				}
			}
		}
	}

    public boolean isValidTask(Exertion servicetask) throws RemoteException,
			ExertionException {
		if (servicetask.getContext() == null) {
			servicetask.getContext().reportException(
					new ExertionException(getProviderName()
							+ " no service context in task: "
							+ servicetask.getClass().getName()));
			return false;
		}
		Task task = (Task)servicetask;

		// if (task.subject == null)
		// throw new ExertionException("No subject provided with the task '" +
		// task.getName() + "'");
		// else if (!isAuthorized(task))
		// throw new ExertionException("The subject provided with the task '" +
		// task.getName() + "' not authorized to use the service '" +
		// providerName + "'");

		String pn = task.getProcessSignature().getProviderName();
		if (pn != null && !matchInterfaceOnly) {
			if (!(pn.equals(SorcerConstants.ANY) || SorcerConstants.ANY
					.equals(pn.trim()))) {
				if (!pn.equals(getProviderName())) {
					servicetask.getDataContext().reportException(
							new ExertionException(
									"No valid task for service provider: "
											+ config.getProviderName()));
					return false;
				}
			}
		}
		Class st = task.getProcessSignature().getServiceType();

		if (publishedServiceTypes == null) {
			servicetask.getDataContext().reportException(
					new ExertionException(
							"No published interfaces defined by: "
									+ getProviderName()));
			return false;
		} else {
			for (int i = 0; i < publishedServiceTypes.length; i++) {
				if (publishedServiceTypes[i] == st) {
					return true;
				}
			}
		}
		servicetask.getDataContext().reportException(
				new ExertionException(
						"Not a valid task for published service types:\n"
								+ Arrays.toString(publishedServiceTypes)));
		return false;
	}

    protected void notify(Exertion task, int notificationType, String message)
			throws RemoteException {
		if (!notifying)
			return;
		logger.info(getClass().getName() + "::notify() START message:"
				+ message);

        MsgRef mr;
        SorcerNotifierProtocol notifier = (SorcerNotifierProtocol) Accessor.getService(null, SorcerNotifierProtocol.class);

        mr = new MsgRef(task.getId(), notificationType,
                config.getProviderName(), message,
                ((ServiceExertion) task).getSessionId());
        // Util.debug(this, "::notify() RUNTIME SESSION ID:" +
        // task.getRuntimeSessionID());
        RemoteEvent re = new RemoteEvent(mr, eventID++, seqNum++, null);
        logger.info(getClass().getName() + "::notify() END.");
        notifier.notify(re);
    }

	public void notifyException(Exertion task, String message, Exception e,
			boolean fullStackTrace) throws RemoteException {

        if (message == null) {
            if (e == null) {
                message = "NO MESSAGE OR EXCEPTION PASSED";
            } else {
                if (fullStackTrace)
                    message = StringUtils.stackTraceToString(e);
                else
                    message = e.getMessage();
            }
        } else {
			if (fullStackTrace)
                message = StringUtils.stackTraceToString(e);
			else
				message = message + " " + e.getMessage();
		}

		notify(task, NOTIFY_EXCEPTION, message);
	}

	public void notifyException(Exertion task, String message, Exception e)
			throws RemoteException {
		notifyException(task, message, e, false);
	}

	public void notifyExceptionWithStackTrace(Exertion task, Exception e)
			throws RemoteException {
		notifyException(task, null, e, true);
	}

	public void notifyException(Exertion task, Exception e)
			throws RemoteException {
		notifyException(task, null, e, false);
	}

	public void notifyInformation(Exertion task, String message)
			throws RemoteException {
		notify(task, NOTIFY_INFORMATION, message);
	}

	/*
	 * public void notifyFailure(ServiceTask task, Exception e, boolean
	 * fullStackTrace)throws RemoteException{ String message = null;
	 * if(fullStackTrace) message = Debug.stackTraceToString(e); else message =
	 * Debug.stackTraceToArray(e)[0];
	 * 
	 * notify(task, NOTIFY_WARNING, message); }
	 */

	public void notifyFailure(Exertion task, Exception e)
			throws RemoteException {
		notifyFailure(task, e.getMessage());
	}

	public void notifyFailure(Exertion task, String message)
			throws RemoteException {
		notify(task, NOTIFY_FAILURE, message);
	}

	public void notifyWarning(Exertion task, String message)
			throws RemoteException {
		notify(task, NOTIFY_WARNING, message);
	}

	/**
	 * Indicates the change of the monitored service context.
	 * 
	 * @param sc
	 *            the service context
	 * @throws sorcer.service.MonitorException
	 * @throws java.rmi.RemoteException
	 */
	public void changed(Context sc, Object aspect) throws RemoteException,
			MonitorException {
		MonitoringSession session = ExertionSessionInfo.getSession();
		if (session != null)
			session.changed(sc, aspect);
	}

	// task/job monitoring API
	public void stop(UEID ueid, Subject subject)
			throws UnknownExertionException, AccessDeniedException {

		// if (ueid.sid == null || !ueid.sid.equals(serviceID))
		// throw new UnknownExertionException(" ServiceID does not match
		// corresponding to "+ueid.asString());

		synchronized (exertionStateTable) {
			if (exertionStateTable.get(ueid.exertionID) == null)
				throw new UnknownExertionException(
						" No exertion exists corresponding to "
								+ ueid.asString());

			exertionStateTable.put(ueid.exertionID, ExecState.STOPPED);
		}
	}

	public boolean suspend(UEID ueid, Subject subject) throws RemoteException,
			UnknownExertionException {

		// if (ueid.sid == null || !ueid.sid.equals(serviceID))
		// throw new UnknownExertionException(" ServiceID does not match
		// corresponding to "+ueid.asString());

		synchronized (exertionStateTable) {
			if (exertionStateTable.get(ueid.exertionID) == null)
				throw new UnknownExertionException(
						" No exertion exists corresponding to "
								+ ueid.asString());

			exertionStateTable.put(ueid.exertionID, ExecState.SUSPENDED);
		}

		return true;
	}

	/**
	 * @return Returns the provider config.
	 */
	public DeploymentConfiguration getProviderConfig() {
		return config;
	}

	/**
	 * @return Returns the provider Jini configuration instance.
	 */
	public Configuration getDeploymentConfig() {
		return config.jiniConfig;
	}

	/**
	 * Set the Jini Configuration for this provider delegate.
	 */
	public void setJiniConfig(Configuration config) {
		getProviderConfig().jiniConfig = config;
	}

	/**
	 * The configuration class for SORCER providers. This configuration collects
	 * the configuration settings for all SORCER service providers. It uses the
	 * provider properties file and/or Jini configuration file. The global
	 * environment properties are copied from this configuration to the
	 * {@link sorcer.core.SorcerEnv} properties.
	 */
	public class DeploymentConfiguration {

		/** Properties found in provider.properties file */
		protected Properties props = new Properties();

		/** Jini Configuration */
		protected Configuration jiniConfig;

		/** Our data directory */
		protected String dataDir = null;

        /**
		 * initializes this config object (loads all information).
		 * 
		 * @param exitOnEmptyName, propsFilename
		 */
		public void init(boolean exitOnEmptyName, String propsFilename) {
			// load configuration from a provider properties file
			if (propsFilename != null && propsFilename.length() > 0)
				loadConfiguration(propsFilename);
			// load configuration as defined in provider Jini configuration file
			// or as defined in SBP in relevant opstrings
			loadJiniConfiguration(jiniConfig);
			checkProviderName(exitOnEmptyName);
			fillInProviderHost();
			logger.info("*** provider properties from " + propsFilename + ": "
					+ GenericUtil.getPropertiesString(props));
		}

		public Configuration getProviderConfiguraion() {
			return jiniConfig;
		}

		/**
		 * @param exitOnEmptyName
		 */
		private void checkProviderName(boolean exitOnEmptyName) {
			String str;
			String providerName;

			// set provider name if defined in provider's properties
			str = getProperty(P_PROVIDER_NAME);
			if (str != null) {
				providerName = str.trim();
				if (!str.equals(providerName))
					props.setProperty(P_PROVIDER_NAME, providerName);
			} else {
				if (exitOnEmptyName) {
					logger.severe("Provider HALTED: its name not defined in the provider config file");
					System.exit(1);
				}
			}
		}

		/**
		 * 
		 */
		private void fillInProviderHost() {
			String hostname = null, hostaddress = null;
			try {
				hostname = SorcerEnv.getLocalHost().getHostName();
				if (hostname == null) {
					logger.warning("Could not aquire hostname");
					hostname = "[unknown]";
				} else {
					hostaddress = SorcerEnv.getLocalHost().getHostAddress();
				}
			} catch (Throwable t) {
				// Can be ignored.
			}

			props.put(P_PROVIDR_HOST, hostname);
			props.put(P_PROVIDR_ADDRESS, hostaddress);
		}

		private void extractDataDir() {
			try {
				dataDir = new File(".").getCanonicalPath() + File.separatorChar;
			} catch (IOException e) {
				e.printStackTrace();
			}
			String rootDir = SorcerEnv.getProperty(DOC_ROOT_DIR);
			String appDir = SorcerEnv.getProperty(P_DATA_DIR);

			if (rootDir == null || appDir == null)
				return;

			rootDir = rootDir.replace('/', File.separatorChar);
			appDir = appDir.replace('/', File.separatorChar);

			if (!rootDir.endsWith(File.separator)) {
				rootDir += File.separator;
			}

			if (!appDir.endsWith(File.separator)) {
				appDir += File.separator;
			}

			dataDir = rootDir + appDir;
		}

		/**
		 * @return the directory where this provider should store its local
		 *         data.
		 */
		public String getDataDir() {
			if (dataDir == null)
				extractDataDir();

			return dataDir;
		}

        /**
		 * Sets the provider name. Can be called manually if needed.
		 * 
		 * @param name
		 */
		public void setProviderName(String name) {
			props.setProperty(P_PROVIDER_NAME, name);
		}

		/**
		 * Sets a configuration property.
		 * 
		 * @param key
		 *            they key to set (usualy starts with provider.)
		 * @param value
		 *            the value to set to.
		 */
		public void setProperty(String key, String value) {
			props.setProperty(key, value);
		}

		/**
		 * Return a name of the provider. The name may be specified in this
		 * provider's properties file.
		 * 
		 * 
		 * @return the name of the provider
		 */
		public String getProviderName() {
			return getProperty(P_PROVIDER_NAME);
		}

		/**
		 * Return a file name of the provider's icon. The name may be specified
		 * in this provider's properties file.
		 * 
		 * @return the name of the provider
		 */
		public String getIconName() {
			return getProperty(P_ICON_NAME);
		}

		/**
		 * @return the host name for this provider
		 */
		public String getProviderHostName() {
			return getProperty(P_PROVIDR_HOST);
		}

		/**
		 * @return the host address for this provider
		 */
		public String getProviderHostAddress() {
			return getProperty(P_PROVIDR_ADDRESS);
		}

		/**
		 * Loads provider properties from a <code>filename</code> file. By
		 * default a provider loads its properties from
		 * <code>provider.properties</code> file located in the provider's
		 * package. Also, a provider properties file name can be specified as a
		 * variable <code>providerProperties</code> in a Jini configuration file
		 * for a SORCER provider. In this case the provider loads properties
		 * from the specified <code>providerProperties</code> file. Properties
		 * are available from the instance field <code>props</code> field and
		 * accessible calling the <code> getProperty(String)</code> method.
		 * 
		 * @param filename
		 *            the properties file name
		 * @see #getProperty
		 */
		public void loadConfiguration(String filename) {
			try {
				// check the class resource
				InputStream is = provider.getClass().getResourceAsStream(
						filename);
				// next check local resource
				if (is == null) {
					is = new FileInputStream(new File(filename));
				}

                props = SorcerEnv.loadProperties(is);

                // copy loaded provider's properties to global Env
                // properties
                SorcerEnv.updateFromProperties(props);
            } catch (Exception ex) {
				logger.warning("Not able to load provider's file properties"
						+ filename);
			}
		}

		public Properties getProviderProperties() {
			return props;
		}

		/**
		 * Returns a value of a comma separated property as defined in. If the
		 * property value is not defined for the delegate's provider then the
		 * equivalent SORCR environment value value is returned.
		 * {@link sorcer.core.SorcerConstants}.
		 * 
		 * @param key
		 *            a property (attribute)
		 * @return a property value
		 */
		public String getProperty(String key) {
			String val = props.getProperty(key);
			if (val != null)
				return val;
			else
				return SorcerEnv.getProperty(key);
		}

		public String getProperty(String property, String defaultValue) {
			String prop = getProperty(property);
			if (prop == null)
				return defaultValue;
			return prop;
		}

        /**
		 * Load the provider deployment configuration. The properties can be
		 * accessed calling getProperty() methods to obtain properties of this
		 * service provider. Also, the SORCER environment properties are updated
		 * by corresponding properties found in the provider's configuration and
		 * in the JVM system properties.
		 */
		private void loadJiniConfiguration(Configuration config) {
			String val;

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_PROVIDER_NAME, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				setProviderName(val);

			String nameSuffixed;
			boolean globalNameSuffixed = SorcerEnv.nameSuffixed();
			try {
				nameSuffixed = (String) config.getEntry(
						ServiceProvider.PROVIDER, "nameSuffixed", String.class,
						"");
			} catch (ConfigurationException e1) {
				nameSuffixed = "";
			}
			// check for the specified suffix by the user
			String suffix = SorcerEnv.getNameSuffix();

			String suffixedName = null;
			if (nameSuffixed.length() == 0) {
				if (suffix == null)
					suffixedName = SorcerEnv.getSuffixedName(val);
				else
					suffixedName = val + "-" + suffix;
			} else if (!nameSuffixed.equals("true")
					&& !nameSuffixed.equals("false")) {
				suffixedName = val + "-" + nameSuffixed;
				nameSuffixed = "true";
			}
			// add provider name and SorcerServiceType entries
			// nameSuffixed not defined by this provider but in sorcer.env
			if (nameSuffixed.length() == 0 && globalNameSuffixed) {
				setProviderName(suffixedName);
			} else if (nameSuffixed.equals("true")) {
				setProviderName(suffixedName);
			}

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_DESCRIPTION, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_DESCRIPTION, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_LOCATION, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_LOCATION, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_TEMPLATE_MATCH, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_TEMPLATE_MATCH, val);

			try {
				val = ""
						+ jiniConfig.getEntry(
								ServiceProvider.PROVIDER,
								J_SERVICE_ID_PERSISTENT, boolean.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SERVICE_ID_PERSISTENT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_DATA_LIMIT, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_DATA_LIMIT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_PORTAL_HOST, String.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_PORTAL_HOST, val);

			try {
				val = ""
						+ jiniConfig.getEntry(
								ServiceProvider.PROVIDER, J_PORTAL_PORT,
								int.class);
			} catch (ConfigurationException e) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_PORTAL_PORT, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_WEBSTER_INTERFACE, String.class);
			} catch (ConfigurationException e5) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_WEBSTER_INTERFACE, val);

			try {
				val = ""
						+ jiniConfig.getEntry(
								ServiceProvider.PROVIDER, J_WEBSTER_PORT,
								int.class);
			} catch (ConfigurationException e4) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_WEBSTER_PORT, val);

			try {
                val = StringUtils.arrayToCSV(jiniConfig.getEntry(
						ServiceProvider.PROVIDER, J_GROUPS, String[].class));
			} catch (ConfigurationException e3) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_GROUPS, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_SPACE_GROUP, String.class);
			} catch (ConfigurationException e2) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SPACE_GROUP, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_SPACE_NAME, String.class);
			} catch (ConfigurationException e2) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_SPACE_NAME, val);

			try {
                val = StringUtils.arrayToCSV(jiniConfig.getEntry(
						ServiceProvider.PROVIDER, J_LOCATORS, String[].class));
			} catch (ConfigurationException e) {
				val = null;
			}

			// if not defined in provider deployment file use from sorcer.env
			if ((val == null) || (val.length() == 0))
				val = SorcerEnv.getProperty(P_LOCATORS);

			if ((val != null) && (val.length() > 0))
				props.put(P_LOCATORS, val);

			try {
				val = (String) jiniConfig.getEntry(ServiceProvider.PROVIDER,
						J_ICON_NAME, String.class);
			} catch (ConfigurationException e5) {
				val = null;
			}
			if ((val != null) && (val.length() > 0))
				props.put(P_ICON_NAME, val);

			// update and log Sorcer properties
            SorcerEnv.updateFromProperties(props);
            SorcerEnv.updateFromProperties(System.getProperties());
			Properties envProps = SorcerEnv.getEnvProperties();
			logger.finer("All SORCER updated properties: " + envProps);
		}
	}

	public String getProviderName() {
		return config.getProviderName();
	}

	public Provider getProvider() {
		return provider;
	}

	public boolean mutualExlusion() {
		return mutualExclusion;
	}

    public TrustVerifier getProxyVerifier() {
		if (smartProxy == null)
			return new ProxyVerifier(outerProxy, this.getServerUuid());
		else
			return new ProxyVerifier(smartProxy, this.getServerUuid());
	}

	/**
	 * Returns an object that implements whatever administration interfaces are
	 * appropriate for the particular service.
	 * 
	 * @return an object that implements whatever administration interfaces are
	 *         appropriate for the particular service.
	 */
	public Object getAdmin() {
		return adminProxy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see sorcer.core.provider.OuterProxy#setAdmin(java.lang.Object)
	 */
	public void setAdmin(Object proxy) {
		adminProxy = (Remote) proxy;
	}

	/**
	 * Unexports the services of this provider appropriately.
	 * 
	 * @param force
	 *            terminate in progress calls if necessary
	 * @return true if unexport succeeds
	 */
	public boolean unexport(boolean force) throws NoSuchObjectException {
		boolean success = true;
		if (outerExporter != null) {
			exports.remove(outerProxy);
			success = outerExporter.unexport(force);
			outerExporter = null;
			outerProxy = null;
		}

		if (partnerExporter != null) {
			exports.remove(innerProxy);
			success &= partnerExporter.unexport(force);
			outerProxy = null;
			partnerExporter = null;
		}

		if (serviceBeans != null && serviceBeans.length > 0) {
			for (int i = 0; i < serviceBeans.length; i++) {
				exports.remove(serviceBeans[i]);
			}
		}

		return success;
	}

	/**
	 * Returns a proxy object for this provider. If the smart proxy is allocated
	 * then returns a non exported object to be registered with loookup services.
	 * However, if a smart proxy implements {@link sorcer.core.provider.proxy.Outer} then the
	 * provider's proxy is set as its inner proxy. Otherwise the {@link java.rmi.Remote}
	 * outer proxy of this provider is returned.
	 * 
	 * @return a proxy, or null
	 * @see sorcer.core.Provider#getProxy()
	 */
	public Object getProxy() {
		try {
			if (smartProxy == null) {
				if (innerProxy != null && partner == null
						&& outerProxy instanceof Partnership) {
					((Partnership) outerProxy).setInner(innerProxy);
					((Partnership) outerProxy).setAdmin(adminProxy);
				} else if (partner != null && partner instanceof Partnership) {
					((Partnership) partner).setInner(innerProxy);
					((Partnership) partner).setAdmin(adminProxy);
				}
				return outerProxy;
			} else if (smartProxy instanceof Partnership) {
				((Partnership) smartProxy).setInner(outerProxy);
				((Partnership) smartProxy).setAdmin(adminProxy);
			}
			return smartProxy;
		} catch (ProviderException e) {
			return null;
		}
	}

	/** {@inheritDoc} */
	public Remote getInner() {
		return innerProxy;
	}

	/** {@inheritDoc} */
	public void setInner(Object innerProxy) throws ProviderException {
		if (outerProxy instanceof Partnership)
			((Partnership) outerProxy).setInner(innerProxy);
		else
			throw new ProviderException("wrong inner proxy for this provider");
	}

	/**
	 * Returns the exporter to use to export this server.
	 * <p>
	 * Two ways for a client to expose his service:
	 * <ol>
	 * <li>Directly subclass ServiceProvider in which case, configuration should
	 * provide the following: <br>
	 * <code>exporter = xxx //Object exported will be this object</code><br>
	 * By default BasicJeriExporter is used
	 * 
	 * <li>Expose objects as services <br>
	 * <code>beans = new String[] { ..... }<br>
	 *    proxyName = "xxx.xxx"</code><br>
	 * Provide the proxy name and have a constructor with one argument, which
	 * accepts the exported inner proxy.
	 * </ol>
	 * 
	 * @param config
	 *            the configuration to use for supplying the exporter
	 */
	private void getExporters(Configuration config) {
		try {
			String exporterInterface = SorcerEnv.getProperty(P_EXPORTER_INTERFACE);
			try {
				exporterInterface = (String) config.getEntry(
						ServiceProvider.COMPONENT, EXPORTER_INTERFACE,
						String.class, SorcerEnv.getHostAddress());
			} catch (Exception e) {
				// do nothng
			}
			logger.info(">>>>> exporterInterface: " + exporterInterface);

			int exporterPort = 0;
			String port = SorcerEnv.getProperty(P_EXPORTER_PORT);
			if (port != null)
				exporterPort = Integer.parseInt(port);
			try {
				exporterPort = (Integer) config.getEntry(
						ServiceProvider.COMPONENT, EXPORTER_PORT,
						Integer.class, null);
			} catch (Exception e) {
				// do nothng
			}
			logger.info(">>>>> exporterPort: " + exporterPort);

			try {
				// initialize smart proxy
				smartProxy = config.getEntry(ServiceProvider.COMPONENT,
						SMART_PROXY, Object.class, null);
			} catch (Exception e) {
				logger.info(">>>>> NO SMART PROXY specified");
				logger.throwing(this.getClass().getName(), "getExporters", e);
				smartProxy = null;
			}

			List<Object> allBeans = new ArrayList<Object>();
			// find it out if service bean instances are available
			Object[] beans = (Object[]) Config.getNonNullEntry(config,
					ServiceProvider.COMPONENT, BEANS, Object[].class,
					new Object[] {});
			if (beans.length > 0) {
				logger.finer("*** service beans by " + getProviderName()
						+ "\nfor: " + Arrays.toString(beans));
				for (int i = 0; i < beans.length; i++) {
					allBeans.add(beans[i]);
					exports.put(beans[i], this);
				}
			}

			// find it out if data service bean instances are available
			Object[] dataBeans = (Object[]) Config.getNonNullEntry(config,
					ServiceProvider.COMPONENT, DATA_BEANS, Object[].class,
					new Object[] {}, getProviderProperties());
			if (dataBeans.length > 0) {
				logger.finer("*** data service beans by " + getProviderName()
						+ "\nfor: " + Arrays.toString(dataBeans));
				for (int i = 0; i < dataBeans.length; i++) {
					allBeans.add(dataBeans[i]);
					exports.put(dataBeans[i], this);
				}
			}

			// find it out if service classes are available
			Class[] beanClasses = (Class[]) Config.getNonNullEntry(config,
					ServiceProvider.COMPONENT, BEAN_CLASSES, Class[].class,
					new Class[] {});
			if (beanClasses.length > 0) {
				logger.finer("*** service bean classes by " + getProviderName()
						+ " for: \n" + Arrays.toString(beanClasses));
				for (int i = 0; i < beanClasses.length; i++)
					allBeans.add(instantiate(beanClasses[i]));
			}

			// find it out if Groovy scripts are available
			String[] scriptlets = (String[]) Config.getNonNullEntry(config,
					ServiceProvider.COMPONENT, SCRIPTLETS, String[].class,
					new String[] {});
			if (scriptlets.length > 0) {
				logger.finer("*** service scriptlets by " + getProviderName()
						+ " for: \n" + Arrays.toString(scriptlets));
				for (int i = 0; i < scriptlets.length; i++)
					allBeans.add(instantiateScriplet(scriptlets[i]));
			}

			if (allBeans.size() > 0) {
				logger.finer("*** all beans by " + getProviderName()
						+ " for: \n" + allBeans);
				serviceBeans = allBeans.toArray();
				initServiceBeans(serviceBeans);
                SorcerILFactory ilFactory = new SorcerILFactory(serviceComponents,
                        implClassLoader);
				outerExporter = new BasicJeriExporter(
						TcpServerEndpoint.getInstance(exporterInterface,
								exporterPort), ilFactory);
			} else {
				logger.finer("*** NO beans used by " + getProviderName());
				outerExporter = (Exporter) Config.getNonNullEntry(
						config,
						ServiceProvider.COMPONENT,
						EXPORTER,
						Exporter.class,
						new BasicJeriExporter(TcpServerEndpoint.getInstance(
								exporterInterface, exporterPort),
								new BasicILFactory()));
				if (outerExporter == null) {
					logger.warning("*** NO provider exporter defined!!!");
				} else {
					logger.finer("current exporter: "
							+ outerExporter.toString());
				}

				partnerExporter = (Exporter) Config.getNonNullEntry(config,
						ServiceProvider.COMPONENT, SERVER_EXPORTER,
						Exporter.class);
				if (partnerExporter == null) {
					logger.warning("NO provider inner exporter defined!!!");
				} else {
					logger.finer("your partner exporter: " + partnerExporter);
				}
			}
		} catch (Exception ex) {
			// ignore missing exporters and use default configurations for exporters
            logger.log(Level.WARNING, "Error while configuring exporters", ex);
        }
	}

	/**
	 * Initializes the map between all the interfaces and the service object
	 * passed via the configuration file.
	 * 
	 * @param serviceBeans
	 *            service objects exposing their interface types
	 */
	@SuppressWarnings("unchecked")
	private Map initServiceBeans(Object[] serviceBeans) {
        serviceComponents = new Hashtable();
        if (serviceBeans == null) {
            try {
                throw new NullPointerException("No service beans defined by: "
                        + provider.getProviderName());
            } catch (RemoteException e) {
                // ignore it
            }
        } else {
            for (int i = 0; i < serviceBeans.length; i++) {
                Class[] interfaze = serviceBeans[i].getClass()
                        .getInterfaces();
                for (int j = 0; j < interfaze.length; j++) {
                    // if (interfaze[j].getDeclaredMethods().length != 0)
                    // allow marker interfaces to be added
                    serviceComponents.put(interfaze[j], serviceBeans[i]);
                }
            }
        }
		return serviceComponents;
	}

	private Object instantiateScriplet(String scripletFilename)
			throws Exception {
        String[] tokens = StringUtils.tokenize(scripletFilename, "|");
		Object bean;
		Object configurator;
		GroovyShell shell = new GroovyShell();
		bean = shell.evaluate(new File(tokens[0]));
		for (int i = 1; i < tokens.length; i++) {
			configurator = shell.evaluate(new File(tokens[i]));
			if ((configurator instanceof Configurator)
					&& (bean instanceof Configurable)) {
				shell.setVariable("configurable", bean);
				bean = ((Configurator) configurator).configure();
			}
		}
		initBean(bean);
		return bean;
	}

	private Object instantiate(Class beanClass) throws Exception {
		return createBean(beanClass);
	}

    private Object createBean(Class beanClass) throws Exception {
		Object bean = beanClass.newInstance();
		initBean(bean);
		return bean;
	}

	private Object initBean(Object serviceBean) {
		try {
			Method m = serviceBean.getClass().getMethod(
					"init", new Class[] { Provider.class });
			m.invoke(serviceBean, provider);
        } catch (NoSuchMethodException ignored) {
			logger.log(Level.INFO, "No 'init' method for this service bean: "
					+ serviceBean.getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException("Error in init method", e);
		}
		exports.put(serviceBean, this);
		logger.fine(">>>>>>>>>>> exported service bean: \n" + serviceBean
				+ "\n by provider: " + provider);
		return serviceBean;
	}

	/**
	 * Returns a partner service specified in the provider's Jini configuration.
	 * 
	 * @param partnerName
	 *            name of the partner service
	 * @param partnerType
	 *            service type (interface) of the partner service
	 * @throws java.rmi.server.ExportException
	 */
	private Remote getPartner(String partnerName, Class partnerType)
			throws ExportException {
		// get the partner and its proxy
		// if it is exportable, export it, otherwise discover one
		Remote pp = null;
		if (partner == null) {
			if (partnerType != null) {
				try {
					partner = (Remote) partnerType.newInstance();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				// if partner exported use it as the primary proxy
				if (partner != null) {
					pp = partnerExporter.export((Remote) partner);
					if (pp != null) {
						innerProxy = outerProxy;
						outerProxy = pp;
					}
				}
			} else {
				// if partner discovered use it as the inner proxy
				innerProxy = (Remote) Accessor.getService(partnerName,
                        partnerType);
			}
		} else {
			// if partner exported use it as the primary proxy
            if (partnerExporter == null)
                try {
                    partnerExporter = new BasicJeriExporter(
                            TcpServerEndpoint.getInstance(SorcerEnv.getHostAddress(), 0),
                            new BasicILFactory());
                } catch (UnknownHostException e) {
                    throw new ExportException("Could not obtain local address", e);
                }
            pp = partnerExporter.export(partner);
            if (pp != null) {
                innerProxy = outerProxy;
                outerProxy = pp;
            } else
                // use partner as this provider's inner proxy
                innerProxy = partner;
			logger.info(">>>>> got innerProxy: " + innerProxy + "\nfor: "
					+ partner + "\nusing exporter: " + partnerExporter);
		}
		return partner;
	}

	public static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}

	public Object getSmartProxy() {
		return smartProxy;
	}

	public Remote getPartner() {
		return partner;

	}

    public boolean isSpaceSecurityEnabled() {
		return spaceSecurityEnabled;
	}

	public Map getServiceComponents() {
		return serviceComponents;
	}

	public void setServiceComponents(Map serviceComponents) {
		this.serviceComponents = serviceComponents;
	}

	public Logger getContextLogger() {
		return contextLogger;
	}

	public Logger getProviderLogger() {
		return providerLogger;
	}

	public Logger getRemoteLogger() {
		return remoteLogger;
	}

	private void initContextLogger() {
		Handler h;
		try {
			contextLogger = Logger.getLogger(PRIVATE_CONTEXT_LOGGER + "."
					+ getProviderName());

			h = new FileHandler(SorcerEnv.getHomeDir() + "/logs/remote/context-"
					+ getProviderName() + "-" + getHostName() + "-ctx%g.log",
					20000, 8, true);
            h.setFormatter(new SimpleFormatter());
            contextLogger.addHandler(h);
        } catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initProviderLogger() {
		Handler h;
		try {
			providerLogger = Logger.getLogger(PRIVATE_PROVIDER_LOGGER + "."
					+ getProviderName());
			h = new FileHandler(SorcerEnv.getHomeDir() + "/logs/remote/provider-"
					+ getProviderName() + "-" + getHostName() + "-prv%g.log",
					20000, 8, true);
            h.setFormatter(new SimpleFormatter());
            providerLogger.addHandler(h);
        } catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initRemoteLogger(Level level, String managerName,
			String loggerName) {
		Handler rh;
		try {
			remoteLogger = Logger.getLogger(loggerName);
			rh = new RemoteHandler(level, managerName);
			if (remoteLogger != null) {
				rh.setFormatter(new SimpleFormatter());
				remoteLogger.addHandler(rh);
				remoteLogger.setUseParentHandlers(false);
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}
	}

	public String getHostAddress() {
		if (hostAddress == null)
			try {
				hostAddress = SorcerEnv.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		return hostAddress;
	}

	public String getHostName() {
		if (hostName == null) {
			try {
				hostName = SorcerEnv.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return hostName;
	}

	public boolean isMonitorable() {
		return monitorable;
	}

    void shutdownAndAwaitTermination(ExecutorService pool) {
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(3, TimeUnit.SECONDS))
					System.err.println("Pool did not terminate");
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * <p>
	 * Returns a context manager of this delegate as defined in the Jini
	 * configuration file.
	 * </p>
	 * 
	 * @return the contextManager
	 */
	public ContextManagement getContextManager() {
		return contextManager;
	}

	public Class[] getPublishedServiceTypes() {
		return publishedServiceTypes;
	}

	public final static String EXPORTER = "exporter";

	public final static String BEANS = "beans";

	public final static String DATA_BEANS = "dataBeans";

	public final static String SCRIPTLETS = "scriptlets";

	public final static String BEAN_CLASSES = "beanClasses";

	public final static String CONTEXT_MANAGER = "contextManager";

	public final static String SMART_PROXY = "smartProxy";

	public final static String SERVER = "server";

	public final static String SERVER_TYPE = "serverType";

	public final static String REMOTE_LOGGING = "remoteLogging";

	public final static String REMOTE_LOGGER_MANAGER_NAME = "remoteLoggerManagerName";

	public final static String REMOTE_LOGGER_NAME = "remoteLoggerName";

	public final static String REMOTE_LOGGER_LEVEL = "remoteLoggerLevel";

	public final static String REMOTE_CONTEXT_LOGGING = "remoteContextLogging";

	public final static String REMOTE_PROVIDER_LOGGING = "remoteProviderLogging";

	public final static String PROVIDER_MONITORING = "monitorEnabled";

	public final static String PROVIDER_NOTIFYING = "notifierEnabled";

	public final static String SERVER_NAME = "serverName";

	public final static String SERVER_EXPORTER = "serverExporter";

	public final static String EXPORTER_INTERFACE = "exporterInterface";

	public final static String EXPORTER_PORT = "exporterPort";

	public final static int KEEP_ALIVE_TIME = 1000;

	public static final String SPACE_ENABLED = "spaceEnabled";

	public static final String SPACE_READINESS = "spaceReadiness";

	public static final String MUTUAL_EXCLUSION = "mutualExclusion";

	public static final String SPACE_SECURITY_ENABLED = "spaceSecurityEnabled";

	public static final String WORKER_TRANSACTIONAL = "workerTransactional";

	public static final String WORKER_COUNT = "workerCount";

    public static final String SPACE_WORKER_QUEUE_SIZE = "workerQueueSize";

	public static final String MAX_WORKER_POOL_SIZE = "maxWorkerPoolSize";

	public static final String WORKER_TRANSACTION_LEASE_TIME = "workerTransactionLeaseTime";

	public static final String SPACE_TIMEOUT = "workerTimeout";

	public static final String INTERFACE_ONLY = "matchInterfaceOnly";

}
