/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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
package sorcer.core.provider;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import net.jini.config.Configuration;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.transaction.CannotAbortException;
import net.jini.core.transaction.CannotCommitException;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionFactory;
import net.jini.core.transaction.UnknownTransactionException;
import net.jini.core.transaction.server.TransactionManager;
import net.jini.lease.LeaseListener;
import net.jini.lease.LeaseRenewalEvent;
import net.jini.lease.LeaseRenewalManager;
import net.jini.space.JavaSpace;
import net.jini.space.JavaSpace05;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.exertion.ExertionEnvelop;
import sorcer.core.loki.exertion.KPEntry;
import sorcer.core.loki.member.LokiMemberUtil;
import sorcer.service.Exec;
import sorcer.service.Exertion;
import sorcer.service.ServiceExertion;
import sorcer.service.Task;
import sorcer.service.space.SpaceAccessor;
import sorcer.service.txmgr.TransactionManagerAccessor;

/**
 * This is a class creates a JavaSpace taker that extends the {@link Thread}
 * class and implements the interface {@link LeaseListener}
 *
 * @see Thread
 * @see LeaseListener
 */
public class SpaceTaker implements Runnable, LeaseListener {
	static Logger logger = LoggerFactory.getLogger(SpaceTaker.class);

	protected boolean isTransactional;

	protected static long TRANSACTION_LEASE_TIME = 1000 * 60 * 1; // 1 minute

	protected long transactionLeaseTimeout = TRANSACTION_LEASE_TIME;

	public final static long SPACE_TIMEOUT = 1000 * 3; // 3 seconds

	protected long spaceTimeout = SPACE_TIMEOUT;

	protected JavaSpace05 space;

	protected SpaceTakerData data;

	protected ExecutorService pool;

	protected static LeaseRenewalManager leaseManager;

	// controls the loop of this space worker
	protected volatile boolean keepGoing = true;

	public static void doLog(String msg, String threadId, Transaction.Created txn) {
		String newMsg = "\nspace taker log; thread id = " + threadId + "\n"
				+ msg;

		if (txn != null) {
			long expTime = txn.lease.getExpiration();
			long expDuration = expTime - System.currentTimeMillis();
			newMsg = newMsg + "\n\ttxn = " + txn;
			newMsg = newMsg + "\n\tlease = " + txn.lease;
			newMsg = newMsg + "\n\texpires in [s] = " + expDuration / 1000;
		}
		logger.info(newMsg);

	}

	public static class SpaceTakerData {
		public ExertionEnvelop entry;
		public LokiMemberUtil myMemberUtil;
		public Provider provider;
		public String spaceName;
		public String spaceGroup;
		public boolean workerTransactional;
		public boolean noQueue;

		public SpaceTakerData() {
		}

		public SpaceTakerData(ExertionEnvelop entry, LokiMemberUtil member,
				Provider provider, String spaceName, String spaceGroup,
				boolean workerIsTransactional, boolean noQueue) {
			this.provider = provider;
			this.entry = entry;
			this.myMemberUtil = member;
			this.spaceName = spaceName;
			this.spaceGroup = spaceGroup;
			this.workerTransactional = workerIsTransactional;
			this.noQueue = noQueue;
		}

		public String toString() {
			return entry.describe();
		}
	}

	/**
	 * Default constructor. Set the worker thread as a Daemon thread
	 */
	public SpaceTaker() {
	}

	/**
	 * This is a Constructor. It executes the default constructor plus set the
	 * provider worker data and executor service pool. The transaction lease
	 * time is set and space time out time is established.
	 * 
	 * @param data
	 *            SpaceDispatcher data
	 * @param pool
	 *            Executor service provides methods to manage termination and
	 *            tracking progress of one or more asynchronous tasks
	 */
	public SpaceTaker(SpaceTakerData data, ExecutorService pool) {
		this();
		this.data = data;
		this.pool = pool;
		this.transactionLeaseTimeout = getTransactionLeaseTime();
		this.spaceTimeout = getTimeOut();
		this.isTransactional = data.workerTransactional;
	}

	protected long getTransactionLeaseTime() {
		long lt = TRANSACTION_LEASE_TIME;
		Configuration config = null;
		try {
			config = data.provider.getProviderConfiguration();
			lt = (Long) config.getEntry(ServiceProvider.PROVIDER,
					ProviderDelegate.WORKER_TRANSACTION_LEASE_TIME, long.class);
		} catch (Exception e) {
			lt = TRANSACTION_LEASE_TIME;
		}
		return lt;
	}

	protected long getTimeOut() {
		long st = SPACE_TIMEOUT;
		Configuration config = null;
		try {
			config = data.provider.getProviderConfiguration();
			st = (Long) config.getEntry(ServiceProvider.PROVIDER,
					ProviderDelegate.SPACE_TIMEOUT, long.class);
		} catch (Exception e) {
			st = SPACE_TIMEOUT;
		}
		return st;
	}

	
	// fields for taker thread metrics
	//
	private int numThreadsTaker = 0;
	private ArrayList<String> threadIdsTaker = new ArrayList<String>();
	private int numCallsTaker = 0;

	protected synchronized String doThreadMonitorTaker(String threadIdString) {
		
		String prefix;
		if (threadIdString == null) {
			numCallsTaker++;
			numThreadsTaker++;
			prefix = "adding taker thread";
			threadIdString = this.toString();
			threadIdsTaker.add(threadIdString);
		} else {
			numThreadsTaker--;
			prefix = "subtracting taker thread";
			threadIdsTaker.remove(threadIdString);
		}
		
		logger.info("\n\n***TAKER THREAD: " + prefix + ": total calls = " + numCallsTaker
				+ "\n***" + prefix + ": number of threads running = "
				+ numThreadsTaker + "\n***" + prefix + ": thread ids running = "
				+ threadIdsTaker 
				+ "\nthis = " + this);

		return threadIdString;
	}
	
	protected static void abortTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotAbortException, RemoteException {
		leaseManager.remove(txn.lease);
		txn.transaction.abort();
	}

	protected static void commitTransaction(Transaction.Created txn) throws UnknownLeaseException, UnknownTransactionException, CannotCommitException, RemoteException  {
		leaseManager.remove(txn.lease);
		txn.transaction.commit();
	}
	
	// fields for worker thread metrics
	//
	private int numThreadsWorker = 0;
	private ArrayList<String> threadIdsWorker = new ArrayList<String>();
	private int numCallsWorker = 0;	
	
	protected synchronized String doThreadMonitorWorker(String threadIdString) {
		String prefix;
		if (threadIdString == null) {
			numCallsWorker++;
			numThreadsWorker++;
			prefix = "adding worker thread";
			//threadIdString = new Integer(numCallsWorker).toString();
			threadIdString = this.toString();
			threadIdsWorker.add(threadIdString);
		} else {
			numThreadsWorker--;
			prefix = "subtracting worker thread";
			threadIdsWorker.remove(threadIdString);
		}
		logger.info("\n\n***WORKER THREAD: " + prefix + ": total calls = " + numCallsWorker
				+ "\n***" + prefix + ": number of threads running = "
				+ numThreadsWorker + "\n***" + prefix + ": thread ids running = "
				+ threadIdsWorker
				+ "\nthis = " + this);

		return threadIdString;
	}
	
	public void run() {
		String threadId = doThreadMonitorTaker(null);

		Transaction.Created txnCreated = null;

		while (keepGoing) {
			ExertionEnvelop ee;
			try {
				space = SpaceAccessor.getSpace(data.spaceName,
                        data.spaceGroup);

				if (space == null) {
					Thread.sleep(spaceTimeout / 6);
					continue;
				}

				if (data.noQueue) {
					if (((ThreadPoolExecutor) pool).getActiveCount() != ((ThreadPoolExecutor) pool)
							.getCorePoolSize()) {
						if (isTransactional) {
							txnCreated = createTransaction(threadId);
							if (txnCreated == null) {
                                logger.warn("SpaceTaker did not get TRANSACTION thread: {}", threadId);
								Thread.sleep(spaceTimeout / 6);
								continue;
							}
							ee = (ExertionEnvelop) space.take(data.entry,
									txnCreated.transaction, spaceTimeout);
						} else {
							ee = (ExertionEnvelop) space.take(data.entry, null,
									spaceTimeout);
						}
					} else {
						continue;
					}
				} else {
					if (isTransactional) {
						txnCreated = createTransaction(threadId);
						if (txnCreated == null) {
							doLog("\t***warning: space taker did not get TRANSACTION.",
									threadId, null);
							Thread.sleep(spaceTimeout / 6);
							continue;
						}
						ee = (ExertionEnvelop) space.take(data.entry,
								txnCreated.transaction, spaceTimeout);
					} else {
						ee = (ExertionEnvelop) space.take(data.entry, null,
								spaceTimeout);
					}
				}
				
				// after 'take' timeout abort transaction and sleep for a while
				// before 'taking' the next exertion
				if (ee == null) {
					if (txnCreated != null) {
						abortTransaction(txnCreated);
						try {
                            Thread.sleep(spaceTimeout / 2);
                        } catch (InterruptedException ie) {
                            keepGoing = false;
                            break;
                        }
					}
					
					txnCreated = null;
					continue;
				}

				pool.execute(new SpaceWorker(ee, txnCreated));
			} catch (Exception ex) {
                logger.warn("Problem with SpaceTaker", ex);
			}
		}
		
		// remove thread monitor
		doThreadMonitorTaker(threadId);
	}

	synchronized public Transaction.Created createTransaction() {
		return createTransaction(null);
	}
	
	synchronized public Transaction.Created createTransaction(String threadId) {
		if (leaseManager == null) {
			leaseManager = new LeaseRenewalManager();
		}
		try {
			TransactionManager tManager = TransactionManagerAccessor
					.getTransactionManager();
			if (tManager == null) {
				return null;
			}
			Transaction.Created created = TransactionFactory.create(tManager,
					transactionLeaseTimeout);
            logger.info("Transaction created for thread: {} {}", threadId, created);

			leaseManager.renewFor(created.lease, Lease.FOREVER, transactionLeaseTimeout, this);

			return created;
		} catch (RemoteException e) {
            logger.warn("Error while creating transaction", e);
		} catch (LeaseDeniedException e) {
            logger.warn("Lease denied", e);
		}
		return null;
	}

	protected boolean isAbandoned(Exertion exertion) {
		if (space != null) {
			ExertionEnvelop ee = new ExertionEnvelop();
			ee.parentID = exertion.getParentId();
			ee.state = Exec.POISONED;
			try {
				if (space.readIfExists(ee, null, JavaSpace.NO_WAIT) != null) {
					logger.info("...dropped poisoned entry...");
					return true;
				}
			} catch (Exception e) {
				logger.debug("isAbandoned", e);
				// continue on
			}
		}
		return false;
	}

	protected void initDataMember(ExertionEnvelop ee, Transaction txn) {
		try {
			KPEntry ckpeRes = (KPEntry) ee.exertion;
			data.myMemberUtil.setGroupSeqId(ckpeRes.GroupSeqId);
			data.myMemberUtil.takewriteKPExertion(ckpeRes.publicKey,
					data.entry.serviceType);
			data.myMemberUtil.readCCK(data.entry.serviceType);
			ee = data.myMemberUtil.takeEnEE(data.entry, txn);
		} catch (Exception e) {
			logger.debug("run", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.jini.lease.LeaseListener#notify(net.jini.lease.LeaseRenewalEvent)
	 */
	@Override
	public void notify(LeaseRenewalEvent e) {
		// Do nothing. It happens when a space providers is destroyed
		logger.debug("space transaction lost its lease: {}", e.getLease());
	}

    public void destroy() {
        keepGoing = false;
    }


    class SpaceWorker implements Runnable {
		private ExertionEnvelop ee;
		private Transaction.Created txnCreated;
		

		SpaceWorker(ExertionEnvelop envelope,
				Transaction.Created workerTxnCreated)
				throws UnknownLeaseException {
			ee = envelope;
			if (workerTxnCreated != null) {
				txnCreated = workerTxnCreated;
			}
		}

		public void run() {
			String threadId = doThreadMonitorWorker(null);

			Entry result = doEnvelope(ee, (txnCreated == null) ? null
					: txnCreated.transaction, threadId, txnCreated);

			if (result != null) {
				try {
					space.write(result, null, Lease.FOREVER);
				} catch (Exception e) {
                    logger.warn("Error while writing the result", e);
                    try {
						abortTransaction(txnCreated);
					} catch (Exception e1) {
                        logger.warn("Error while aborting transaction", e1);
						doThreadMonitorWorker(threadId);
						return;
					}
					doThreadMonitorWorker(threadId);
					return;
				}
				
				if (txnCreated != null) {
					try {
						commitTransaction(txnCreated);
					} catch (Exception e) {
                        logger.warn("Error while committing transaction", e);
						doThreadMonitorWorker(threadId);
						return;
					}
				}

			} else {
				if (txnCreated != null) {
					try {
						abortTransaction(txnCreated);
					} catch (Exception e) {
                        logger.warn("Error while aborting transaction", e);
						doThreadMonitorWorker(threadId);
						return;
					}
				}
			}
			doThreadMonitorWorker(threadId);
		}

		public Entry doEnvelope(ExertionEnvelop ee, Transaction transaction, String threadId, Transaction.Created txn) {
			ServiceExertion se;
            ServiceExertion out;
            try {
				ee.exertion.getControlContext().appendTrace(
						"taken by: " + data.provider.getProviderName() + ":"
								+ data.provider.getProviderID());
				se = (ServiceExertion) ee.exertion;

				if (se instanceof Task) {
					// task for the worker's provider
					out = ((ServiceProvider) data.provider)
							.getDelegate().doTask((Task) se, transaction);
				} else {
					// delegate it to another collaborating service
					out = (ServiceExertion) data.provider.service(se,
							transaction);
				}
				if (out != null) {
					out.setStatus(Exec.DONE);
					ee.state = Exec.DONE;
					ee.exertion = out;
				} else {
					se.setStatus(Exec.ERROR);
					ee.state = Exec.ERROR;
					ee.exertion = se;
				}
			} catch (Throwable th) {	
				logger.debug("doEnvelope", th);
				if (th instanceof Exception) {
					ee.state = Exec.FAILED;
					((ServiceExertion) ee.exertion).setStatus(Exec.FAILED);
				} else {
					ee.state = Exec.ERROR;
					((ServiceExertion) ee.exertion).setStatus(Exec.ERROR);
				}
				((ServiceExertion) ee.exertion).reportException(th);
			}
			return ee;
		}
	}

}