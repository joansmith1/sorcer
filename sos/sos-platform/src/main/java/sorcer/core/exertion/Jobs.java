/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.exertion;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import sorcer.core.SorcerConstants;
import sorcer.core.context.ControlContext;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.NetSignature;
import sorcer.service.*;
import sorcer.service.Strategy.Access;
import sorcer.service.Strategy.Flow;

public class Jobs implements SorcerConstants {

	private Jobs() {
		// Utility class
	}
	
	public static boolean isCatalogSingleton(Job job) {
		ControlContext cc = job.getControlContext();
		return job.size() == 1
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isQosCatalogSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.QOS_PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isQosCatalogParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.QOS_PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PUSH.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isCatalogBlock(Exertion exertion) {
		Context cc = exertion.getControlContext();
		return exertion instanceof Block
				&& Access.PUSH.equals(cc.get(ControlContext.EXERTION_ACCESS));
	}

	public static boolean isSpaceBlock(Exertion exertion) {
		Context cc = exertion.getControlContext();
		return exertion instanceof Block
				&& Access.PULL.equals(cc.get(ControlContext.EXERTION_ACCESS));
	}
	
	public static boolean isSWIFSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.SWIF.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isSpaceSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isQosSpaceSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.QOS_PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isSpaceParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isQosSpaceParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW))
				&& Access.QOS_PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}
	
	public static boolean isSpaceSingleton(Job job) {
		ControlContext cc = job.getControlContext();
		return job.size() == 1
				&& Access.PULL.equals(cc.get(cc.EXERTION_ACCESS));
	}

	public static boolean isParallel(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.PAR.equals(cc.get(cc.EXERTION_FLOW));
	}

	public static boolean isSequential(Job job) {
		ControlContext cc = job.getControlContext();
		return Flow.SEQ.equals(cc.get(cc.EXERTION_FLOW));
	}

	public static boolean isMonitorable(Job job) {
		ControlContext cc = job.getControlContext();
		return cc.isMonitorable();
	}

	public static List<Exertion> getInputExertions(Job job) throws ContextException {
		if (job == null || job.size() == 0)
			return null;
		List<Exertion> exertions = new ArrayList<Exertion>();
		Exertion master = job.getMasterExertion();
		for (int i = 0; i < job.size(); i++)
			if (!(job.get(i).equals(master) || job
					.getControlContext().isSkipped(job.get(i))))
				exertions.add(job.get(i));
		return exertions;
	}

	public static ControlContext getCC(Context sc) {
		ControlContext cc = new ControlContext();
		for (Enumeration e = ((Hashtable) sc).keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			cc.put(key, ((Hashtable) sc).get(key));
		}
		cc.setName(sc.getName());
		cc.setId(sc.getId());
		cc.setParentPath(sc.getParentPath());
		cc.setParentID((sc.getParentID() == null) ? null : sc.getParentID());
		cc.setSubject(sc.getSubjectPath(), sc.getSubjectValue());
		cc.setCreationDate(sc.getCreationDate());
		cc.setLastUpdateDate(sc.getLastUpdateDate());
		cc.setDescription(sc.getDescription());
		cc.setScopeCode(sc.getScope());
		cc.setOwnerID(sc.getOwnerID());
		cc.setSubjectID(sc.getSubjectID());
		cc.setProject(sc.getProject());
		cc.setAccessClass(sc.getAccessClass());
		cc.setExportControl(sc.getExportControl());
		cc.setGoodUntilDate(sc.getGoodUntilDate());
		cc.setDomainID(sc.getDomainID());
		cc.setSubdomainID(sc.getSubdomainID());
		cc.setDomainName(sc.getDomainName());
		cc.setMetacontext(sc.getMetacontext());
		cc.isPersistantTaskAssociated = ((ServiceContext) sc).isPersistantTaskAssociated;
		return cc;
	}

	public static void removeExceptions(Job job) throws ContextException {
		removeExceptions(job.getContext());
		for (int i = 0; i < job.size(); i++) {
			if (job.get(i).isJob())
				removeExceptions((Job) job.get(i));
			else
				removeExceptions(job.get(i)
						.getContext());
		}
	}

	public static void removeExceptions(Context sc) {
		for (Enumeration e = ((ServiceContext) sc).keys(); e.hasMoreElements();) {
			String path = (String) e.nextElement();
			if (path.startsWith(SorcerConstants.EXCEPTIONS))
				try {
					sc.removePath(path);
				} catch (Exception ex) {
					// do nothing
				}
		}
		// sc.removeAttribute(SORCER.EXCEPTIONS);
	}

/*	public static void preserveNodeReferences(Exertion ref, Exertion res)
			throws ContextException {
		if (((ServiceExertion) ref).isJob() && ((ServiceExertion) res).isJob())
			preserveNodeReferences((Job) ref, (Job) res);
		else
			preserveNodeReferences((ServiceExertion) ref, (ServiceExertion) res);
	}

	public static void preserveNodeReferences(Job refJob,
			Job resJob) throws ContextException {
		int size = (refJob.size() < resJob.size()) ? refJob.size() : resJob
				.size();
		for (int i = 0; i < size; i++) {
			if (((ServiceExertion) refJob.exertionAt(i)).isJob()
					&& ((ServiceExertion) resJob.exertionAt(i)).isJob())
				preserveNodeReferences((Job) refJob.exertionAt(i),
						(Job) resJob.exertionAt(i));
			else if (((ServiceExertion) refJob.exertionAt(i)).isTask()
					&& ((ServiceExertion) resJob.exertionAt(i)).isTask())
				preserveNodeReferences(((ServiceExertion) refJob.exertionAt(i))
						.getContext(), ((ServiceExertion) resJob.exertionAt(i))
						.getContext());
		}
	}

	public static void preserveNodeReferences(Context refContext,
			Context resContext) throws ContextException {
		Contexts.copyContextNodesFrom(resContext, refContext);
	}*/

	public static void replaceNullIDs(Exertion ex) throws ContextException {
		if (ex == null)
			return;
		if (ex.isJob()) {
			Job job = (Job) ex;
			if (job.getId() == null)
				job.setId(getId());
			if (job.getContext().getId() == null)
				job.getContext().setId(getId());
			for (int i = 0; i < job.size(); i++)
				replaceNullIDs(job.get(i));
		} else
			replaceNullIDs((ServiceExertion) ex);
	}

	public static void replaceNullIDs(ServiceExertion task) throws ContextException {
		if (task.getId() == null)
			task.setId(getId());
		if (task.getContext() != null) {
			if (task.getContext().getId() == null)
				task.getContext().setId(getId());
		}
	}

	public static synchronized Uuid getId() {
		 return UuidFactory.generate();
	}
	
	public static synchronized String getID() {
		try {
			Thread.sleep(1);
		} catch (InterruptedException ie) {
			// do Nothing
		}
		return Long.toHexString(new Date().getTime());
	}

	public static ExertionEnvelop getEntryEnvelop(Exertion ex)
			throws ExertionException {
		if (ex == null)
			return null;
		else if (ex.getProcessSignature() != null)
			throw new ExertionException("No Method For Exertion e=" + ex);

		ExertionEnvelop eenv = ExertionEnvelop.getTemplate();
		eenv.serviceType = ((NetSignature) ex.getProcessSignature()).getServiceType();
		eenv.providerName = ex.getProcessSignature().getProviderName();
		eenv.exertion = ex;
		eenv.exertionID = ex.getId();
		eenv.isJob = new Boolean(ex.isJob());
		eenv.state = new Integer(Exec.INITIAL);
		return eenv;
	}

	public static List<Context> getTaskContexts(Exertion ex) throws ContextException {
		List<Context> v = new ArrayList<Context>();
		collectTaskContexts(ex, v);
		return v;
	}

	// For Recursion
	private static void collectTaskContexts(Exertion exertion, List<Context> contexts) throws ContextException {
//		if (exertion.isConditional())
//			contexts.add(exertion.getDataContext());
//		else 
			if (exertion instanceof Job) {
			contexts.add(exertion.getDataContext());
			for (int i = 0; i < exertion.getExertions().size(); i++)
				collectTaskContexts(((Job) exertion).get(i),
						contexts);
		}
		else if (exertion instanceof Task) {
			contexts.add(exertion.getDataContext());
		}
	}
}
