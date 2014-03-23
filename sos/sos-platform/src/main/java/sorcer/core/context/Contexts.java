/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

package sorcer.core.context;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.jini.id.Uuid;
import sorcer.core.SorcerConstants;
import sorcer.core.context.node.ContextNode;
import sorcer.security.util.SorcerPrincipal;
import sorcer.service.*;
import sorcer.util.StringUtils;

import static sorcer.core.SorcerConstants.*;

/**
 * The Contexts class provides utility services to ServiceContext that are
 * required by requestors such as the the SORCER graphical user interface and
 * and any complex SORCER requestor or service provider.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Contexts {

	private static Logger logger = Logger.getLogger(Contexts.class.getName());
	
	// job broker
	final static String JOBBER_IS_DIRECT = "jobber" + CPS + "is direct";

	// control context name
	final static String CONTROL_CONTEXT = "Control Context";

	// control context attributes
	final static String JOB_STRATEGY = "job" + CPS + "strategy";

	final static String JOB_STRATEGY_ACCESS = "job" + CPS + "strategy" + CPS
			+ "access";

	final static String JOB_NO_WAIT = "job/no wait";

	final static String NODE_REFERENCE_PRESERVED = "job" + CPS + "node" + CPS
			+ "reference" + CPS + "preserved";

	final static String JOBBER_NAME = "jobber" + CPS + "name";

	final static String NOTIFY_EXEC = "notify" + CPS + "execution" + CPS
			+ "to:";

	final static String JOB_COMMENTS = "job" + CPS + "comments";

	final static String JOB_FEEDBACK = "job" + CPS + "feedback";

	final static String GET_EXEC_TIME = "get" + CPS + "exec/time";

	final static String TASK_REVIEW = "task" + CPS + "review";

	final static String EXEC_TIME = "exec" + CPS + "time";

	final static String PRIORITY = "priority";

	final static String TASK = "task" + CPS;

	final static String JOB = "job";

	final static String IO = "io";

	final static String ID = CPS + "id";

	final static String JOB_GET_EXEC_TIME = "job" + CPS + GET_EXEC_TIME;

	final static String JOB_NOTIFY_EXEC = "job" + CPS + NOTIFY_EXEC;

	// control context values
	final static String SPACE = "space";

	final static String CATALOG = "catalog";

	final static String SWIF = "swif";

	final static String DIRECT = "direct";

	final static String PARALLEL = "parallel";

	final static String SEQUENTIAL = "sequential";

	final static String SORCER_VARIABLES_PATH = "supportObjects" + CPS
			+ "sorcerVariables";

	final static String EMPTY_CONTEXT_NODE = "New Context Node";

	/**
	 * Returns list of all values that are referenced by paths that start with
	 * the given <code>subpath</code> string.
	 * <p>
	 * Caution - a match does not indicate the returned results are subpaths of
	 * given path. For instance, consider context that contains paths. It is
	 * recommended to end a matched substring with the context path separator
	 * (SORCER.CPS).
	 * 
	 * <ul>
	 * <li>a/b/c
	 * <li>a/bb/d
	 * </ul>
	 * 
	 * a call to this method with path="a/b" will return both "a/b/c" and
	 * "a/bb/d" and only the first is a subpath.
	 * 
	 * @param context
	 *            ServiceContext to query
	 * @param subpath
	 *            the match string
	 * @return a Vector of context values maching a path
	 * @see #getStartsWithPaths
	 * @see #getSortedStartsWithValues
	 */
	public static List getValuesStartsWith(Context context, String subpath)
			throws ContextException {
		String path;
		List ids = new ArrayList();
		Enumeration e = context.contextPaths();
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			if (path.startsWith(subpath))
				ids.add(context.getValue(path));
		}
		if (ids.size() > 0)
			return ids;
		else
			return null;
	}

	public static List<?> getNamedInValues(Context context) throws ContextException {
		List inpaths = Contexts.getNamedInPaths(context);
		if (inpaths == null) 
			return null;
		List list = new ArrayList(inpaths.size());
		for (Object path : inpaths)
			try {
				list.add(context.getValue((String) path));
			} catch (ContextException e) {
				throw new ContextException(e);
			}

		return list;
	}

	public static List<?> getNamedOutValues(Context context) throws ContextException {
		List outpaths = Contexts.getNamedOutPaths(context);
		if (outpaths == null) 
			return null;
		List list = new ArrayList(outpaths.size());
		for (Object path : outpaths)
			try {
				list.add(context.getValue((String) path));
			} catch (ContextException e) {
				throw new ContextException(e);
			}

		return list;
	}
	
	public static List<?> getPrefixedInValues(Context context) throws ContextException {
		List inpaths = Contexts.getPrefixedInPaths(context);
		if (inpaths == null) 
			return null;
		List list = new ArrayList(inpaths.size());
		for (Object path : inpaths)
			try {
				list.add(context.getValue((String) path));
			} catch (ContextException e) {
				throw new ContextException(e);
			}

		return list;
	}

	public static List<?> getPrefixedOutValues(Context context) throws ContextException {
		List inpaths = Contexts.getPrefixedOutPaths(context);
		if (inpaths == null) 
			return null;
		List list = new ArrayList(inpaths.size());
		for (Object path : inpaths)
			try {
				list.add(context.getValue((String) path));
			} catch (ContextException e) {
				throw new ContextException(e);
			}

		return list;
	}

	/**
	 * 
	 * Returns list of paths that start with the given subpath string.
	 * <p>
	 * Caution - a match does not indicate the returned paths are subpaths of
	 * given path. It is recommended to end a matched substring with the context
	 * path separator (SORCER.CPS).
	 * 
	 * @param context
	 *            ServiceContext to query
	 * @param subpath
	 *            the match string
	 * @return a Vector of matching paths
	 * @throws ContextException
	 */
	public static ArrayList getKeysStartsWith(Context context, String subpath)
			throws ContextException {
		Enumeration e = context.contextPaths();
		String candidate;
		ArrayList result = new ArrayList();
		while (e.hasMoreElements()) {
			candidate = "" + e.nextElement();
			if (candidate.indexOf(subpath) >= 0)
				result.add(candidate);
		}
		return result;
	}

	public static List getSortedValuesStartsWith(Context cxt, String path)
			throws ContextException {
		List ids = getValuesStartsWith(cxt, path);
		if (ids != null && ids.size() > 0) {
			StringUtils.bubbleSort(ids);
			return ids;
		} else
			return null;
	}

	public static void copyNodes(Context fromCntxt, Context toCntxt)
			throws ContextException {
		Enumeration enu = ((Hashtable) fromCntxt).keys();
		String key;
		Object val;
		try {
			while (enu.hasMoreElements()) {
				key = (String) enu.nextElement();
				val = toCntxt.getValue(key);

				if (val instanceof ContextNode) {
					// Util.debug(this, "old DataNode data =
					// "+((DataNode)val).getData());
					// Util.debug(this, "new DataNode data =
					// "+((DataNode)fromCntxt.getValue(key)).getData());
					((ContextNode) val).copy((ContextNode) fromCntxt
							.getValue(key));
					// Util.debug(this, "old DataNode data =
					// "+((DataNode)val).getData());
				} else if (!(key.equals(SORCER_VARIABLES_PATH))) {
					toCntxt.putValue(key, fromCntxt.getValue(key));
				}
			}
		} catch (MalformedURLException me) {
			throw new ContextException("Caught MalformedURLException", me);
		}

		// remove sorcer variables from new context
		// these objects are new objects and collide with old
		// object IDs in original context
		((Hashtable) toCntxt).remove(SORCER_VARIABLES_PATH);
	}

    public static boolean containsContextVariables(Context cntxt) {
		Object h = ((Hashtable) cntxt).get(SORCER_VARIABLES_PATH);
		if ((h == null) || !(h instanceof Hashtable)
				|| (((Hashtable) h).isEmpty()))
			return false;
		else
			return true;
	}

	public static boolean containsContextVariable(Context cntxt, String id) {
		if (containsContextVariables(cntxt)) {
			Hashtable h = (Hashtable) ((Hashtable) cntxt)
					.get(SORCER_VARIABLES_PATH);
			if (h.containsKey(id)) {
				return true;
			}
		}
		return false;
	}

	public static Hashtable getContextVariableMap(Context cntxt)
			throws ContextException {
		if (containsContextVariables(cntxt)) {
			return (Hashtable) cntxt.getValue(SORCER_VARIABLES_PATH);
		} else {
			throw new ContextException("RequestorContext"
					+ ".getContextVariableHashtable(ServiceContext): "
					+ "There are no ContextVariables in " + "this context.");
		}
	}

	public static void deleteContextVariables(Context cntxt) {
		if (containsContextVariables(cntxt)) {
			cntxt.remove(SORCER_VARIABLES_PATH);
		}
	}

	/*
	 * Return boolean result indicating if the value at the designated path is
	 * set as an empty leaf node.
	 */
	public static boolean isEmptyLeafNode(Context cntxt, String path)
			throws ContextException {
		Object obj;
		obj = cntxt.getValue(path);
		if (obj instanceof String) {
			if (obj.equals(Context.EMPTY_LEAF))
				return true;
		}
		return false;
	}

	public static String getTitle(Context cntxt) {
		String domainName = cntxt.getDomainName();
		String subdomainName = cntxt.getSubdomainName();
		return cntxt.getName() + ", "
				+ (domainName == null ? "" : domainName + ", ")
				+ (subdomainName == null ? "" : subdomainName);
	}

	public static void map(String fromPath, Context fromContext, String toPath,
			Context toContext) throws ContextException {
		// add attributes
		// map sorcer types also
		String cp, cp0;
		cp = fromContext.getMetaattributeValue(fromPath,
				Context.CONTEXT_PARAMETER);
		if (cp == null)
			throw new ContextException(
					"no marked attribute as in, out, or inout");
		if (cp.startsWith(Context.DA_IN)) {
			cp0 = Context.DA_INOUT + cp.substring(Context.DA_IN.length());
			fromContext.mark(toPath, Context.CONTEXT_PARAMETER
					+ SorcerConstants.APS + cp0);
		} else if (cp.startsWith(Context.DA_OUT)) {
			// do nothing for now
		} else {
			markOut(fromContext, fromPath);
		}
		toContext.mark(toPath, Context.CONTEXT_PARAMETER
				+ APS + Context.DA_IN + APS
				+ fromPath + APS + fromContext.getId());
	}

	public static String getFormattedOut(Context sc, boolean isHTML) {
		// return context with outpaths
		String inoutAssoc = Context.DIRECTION + SorcerConstants.APS
				+ Context.DA_INOUT + APS;
		String outAssoc = Context.DIRECTION + SorcerConstants.APS
				+ Context.DA_OUT + APS;
		String[] outPaths = null, inoutPaths = null;
		try {
			outPaths = Contexts.getMarkedPaths(sc, outAssoc);
		} catch (ContextException ex) {
			// do nothing
		}
		try {
			inoutPaths = Contexts.getMarkedPaths(sc, inoutAssoc);
		} catch (ContextException ex) {
			// do nothing
		}
		String cr;
		if (isHTML)
			cr = "<br>";
		else
			cr = "\n";
		StringBuffer sb = new StringBuffer();
		if (outPaths != null)
			for (int i = 0; i < outPaths.length; i++) {
				sb.append(outPaths[i]).append(" = ");
				try {
					sb.append(sc.getValue(outPaths[i])).append(cr);
				} catch (ContextException ex) {
					sb.append("Unable to retrieve value").append(cr);
				}
			}
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++) {
				sb.append(inoutPaths[i]).append(" = ");
				try {
					sb.append(sc.getValue(inoutPaths[i])).append(cr);
				} catch (ContextException ex) {
					sb.append("Unable to retrieve value").append(cr);
				}
			}
		return sb.toString();
	}

	/**
	 * Sets context type as input for a path
	 */
	public static Context markIn(Context cntxt, String path)
			throws ContextException {
		return cntxt.mark(path, Context.CONTEXT_PARAMETER
				+ APS + Context.DA_IN + APS + APS);
	}

	/**
	 * Sets context type as out for a path
	 */
	public static Context markOut(Context cntxt, String path)
			throws ContextException {
		return cntxt.mark(path, Context.CONTEXT_PARAMETER
				+ APS + Context.DA_OUT + APS + APS);
	}

	/**
	 * Sets context type as inout for a path
	 */
	public static Context markInout(Context cntxt, String path)
			throws ContextException {
		return cntxt.mark(path, Context.CONTEXT_PARAMETER
				+ APS + Context.DA_INOUT + APS + APS);
	}
	
	public static Context markOutPipe(Context cntxt, String path)
			throws ContextException {
		return cntxt
				.mark(path, Context.PIPE + APS + Context.DA_OUT + APS + APS);
	}

	public static Context markInPipe(Context cntxt, String path)
			throws ContextException {
		return cntxt.mark(path, Context.PIPE + APS
				+ Context.DA_IN + APS + APS);
	}
	
	public static Object getValueAt(Context cxt, int index)
			throws ContextException {
		String tuple = Context.INDEX + APS + index;
		Object[] objs = getMarkedValues(cxt, tuple);
		Object value = null;
		if (objs.length > 0) {
			value = objs[0];
		}
		return value;
	}
	
	public static Object[] getValuesAt(Context cxt, int index)
			throws ContextException {
		String tuple = Context.INDEX + APS + index;
		Object[] objs = getMarkedValues(cxt, tuple);
		return objs;
	}

	public static Object getInValueAt(Context cxt, int index)
			throws ContextException {
		String tuple = Context.OPP + APS + Context.DA_IN + APS + index;
		Object[] objs = getMarkedValues(cxt, tuple);
		Object value = null;
		if (objs.length > 0) {
			value = objs[0];
		}
		return value;
	}
	
	public static Object getOutValueAt(Context cxt, int index)
			throws ContextException {
		String tuple = Context.OPP + APS + Context.DA_OUT + APS + index;
		Object[] objs = getMarkedValues(cxt, tuple);
		Object value = null;
		if (objs.length > 0) {
			value = objs[0];
		}
		return value;
	}
	
	public static Object getInoutValueAt(Context cxt, int index)
			throws ContextException {
		String tuple = Context.OPP + APS + Context.DA_INOUT + APS + index;
		Object[] objs = getMarkedValues(cxt, tuple);
		Object value = null;
		if (objs.length > 0) {
			value = objs[0];
		}
		return value;
	}
	
	public static Object putValueAt(Context cxt, String path, Object value,
			int index) throws ContextException {
		cxt.putValue(path, value);
		cxt.mark(path, Context.INDEX + APS + index);
		return value;
	}

	public static Object putOutValueAt(Context cntxt, String path,
			Object value, int index) throws ContextException {
		cntxt.putValue(path, value);
		markOut(cntxt, path);
		cntxt.mark(path, Context.OPP + APS + Context.DA_OUT + APS + index);
		return value;
	}

	public static Object putInValueAt(Context cntxt, String path, Object value,
			int index) throws ContextException {
		cntxt.putValue(path, value);
		markIn(cntxt, path);
		cntxt.mark(path, Context.OPP + APS + Context.DA_IN + APS + index);
		return value;
	}

	public static Object putInoutValueAt(Context cntxt, String path, Object value,
			int index) throws ContextException {
		cntxt.putValue(path, value);
		markInout(cntxt, path);
		cntxt.mark(path, Context.OPP + APS + Context.DA_INOUT + APS + index
				+ APS);
		return value;
	}
	
	public static ContextNode[] getContextNodes(Context context)
			throws ContextException {
		Enumeration e = context.contextPaths();
		java.util.Set nodes = new HashSet();
		Object obj = null;
		while (e.hasMoreElements()) {
			obj = e.nextElement();
			if (obj != null && obj instanceof ContextNode)
				nodes.add(obj);
            // Look for ContextNodes also in values and set the ContextNode's direction
            else {
                Object val = context.get((String)obj);
                if (val!= null && val instanceof ContextNode) {
                    String dire = Contexts.getDirection(context, (String)obj);

                    ((ContextNode)val).setDA(dire);
                    nodes.add(val);
                }
            }
		}
		ContextNode[] nodeArray = new ContextNode[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}

	/**
	 * Returns all context nodes recursively in this context and all its emebded
	 * contexts, tasks, and jobs.
	 * 
	 * @param context
	 *            a servcie context
	 * @return a list -f {@link ContextNodes}.
	 * @throws ContextException
	 */
	public static ContextNode[] getAllContextNodes(Context context)
			throws ContextException {
		List allNodes = null;
		List additional = null;
		try {
			allNodes = Arrays.asList(getContextNodes(context));
			for (Object obj : allNodes) {
				if (((ContextNode) obj).getData() instanceof Context) {
					additional = Arrays
							.asList(getAllContextNodes((Context) obj));
					if (additional.size() > 0)
						allNodes.addAll(additional);
				} else if (obj instanceof ServiceExertion) {
					additional = Arrays
							.asList(getTaskContextNodes((ServiceExertion) obj));
				} else if (obj instanceof Job) {
					additional = Arrays
							.asList(getTaskContextNodes((ServiceExertion) obj));
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		ContextNode[] result = new ContextNode[allNodes.size()];

		allNodes.toArray(result);
		return result;
	}

	public static ContextNode[] getTaskContextNodes(Exertion task)
			throws ContextException {
		List allNodes = new ArrayList();
		List additional = null;

		additional = Arrays.asList(getAllContextNodes(task.getContext()));
		if (additional.size() > 0)
			allNodes.addAll(additional);
		ContextNode[] result = new ContextNode[allNodes.size()];

		allNodes.toArray(result);
		return result;
	}

	public static ContextNode[] getTaskContextNodes(Job job)
			throws ContextException {
		List allNodes = new ArrayList();
		List additional = null;

		List<Exertion> exertions = ((Job) job).getExertions();
		for (Object exertion : exertions) {
			if (exertion instanceof ServiceExertion) {
				additional = Arrays
						.asList(getTaskContextNodes((ServiceExertion) exertion));
				if (additional.size() > 0)
					allNodes.addAll(additional);
			} else if (exertion instanceof Job) {
				additional = Arrays.asList(getTaskContextNodes((Job) exertion));
				if (additional.size() > 0)
					allNodes.addAll(additional);
			}
		}
		ContextNode[] result = new ContextNode[allNodes.size()];

		allNodes.toArray(result);
		return result;
	}

	public static ContextNode[] getContextNodesWithAttribute(Context sc,
			String attribute) throws ContextException {
		String[] paths = getPathsWithAttribute(sc, attribute);
		java.util.Set nodes = new HashSet();
		Object obj = null;
		for (int i = 0; i < paths.length; i++) {
			obj = sc.getValue(paths[i]);
			if (obj != null && obj instanceof ContextNode)
				nodes.add(obj);
		}
		ContextNode[] nodeArray = new ContextNode[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}

	public static ContextNode[] getMarkedConextNodes(Context sc,
			String association) throws ContextException {
		String[] paths = getMarkedPaths(sc, association);
		java.util.Set nodes = new HashSet();
		Object obj = null;
		for (int i = 0; i < paths.length; i++) {
			obj = sc.getValue(paths[i]);
			if (obj != null && obj instanceof ContextNode)
				nodes.add(obj);
		}
		ContextNode[] nodeArray = new ContextNode[nodes.size()];
		nodes.toArray(nodeArray);
		return nodeArray;
	}

	public static ContextNode getMarkedConextNode(Context sc, String association)
			throws ContextException {
		return getMarkedConextNodes(sc, association)[0];
	}

    public static Object[] getMarkedValues(Context sc, String association)
			throws ContextException {
		String[] paths = getMarkedPaths(sc, association);
		List<Object> values = new ArrayList<Object>();
		for (int i = 0; i < paths.length; i++) {
			values.add(sc.getValue(paths[i]));
		}
		return values.toArray();
	}

	public static boolean hasMarkedValue(Context sc, String association)
			throws ContextException {
		String[] paths = getMarkedPaths(sc, association);
		if (paths.length == 0)
			return false;
		return true;
	}

	public static Object getMarkedValue(Context sc, String association)
			throws ContextException {
		return getMarkedValues(sc, association)[0];
	}

	/**
	 * Returns the list of all context paths matching the given regular
	 * expression.
	 * 
	 * @param regex
	 *            the regular expression to which paths of this context are to
	 *            be matched
	 * @param context
	 *            a service context
	 * @return an enumeration of matches for the given regular expression
	 * @throws ContextException
	 */
	public List getPaths(String regex, Context context)
			throws ContextException {
		Enumeration e = context.contextPaths();
		List list = new ArrayList();
		Pattern p = Pattern.compile(regex);
		String key;
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			if (p.matcher(key).matches())
				list.add(key);
		}
		return list;
	}

	public List getNamedPaths(Context context)
			throws ContextException {
		String regex = "^" + context.getName() + CPS;
		return getPaths(regex, context);
	}
	
	/**
	 * Returns a list of all paths marked as data input.
	 * 
	 * @param cntxt
	 *            a service context
	 * @return list of all paths marked as input
	 * @throws ContextException
	 */
	public static List<String> getInPaths(Context cntxt) throws ContextException {
		// get all the in and inout paths
		String inAssoc = Context.DIRECTION + APS + Context.DA_IN;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] inPaths = getMarkedPaths(cntxt, inAssoc);
		String[] inoutPaths = getMarkedPaths(cntxt, inoutAssoc);
        int cap = (inPaths == null ? 0 : inPaths.length) + (inoutPaths == null ? 0 : inoutPaths.length);
        List<String> list = new ArrayList<String>(cap);

		if (inPaths != null)
            Collections.addAll(list, inPaths);
		if (inoutPaths != null)
            Collections.addAll(list, inoutPaths);
		return list;
	}

	public static List getNamedInPaths(Context cntxt) throws ContextException {
		// get all the in and inout paths
		String cs = ((ServiceContext)cntxt).getCurrentSelector();
		if (cs != null)
			return getPrefixedInPaths(cntxt, cs);
		else
			return null;
	}
	
	public static List getPrefixedInPaths(Context cntxt) throws ContextException {
		// get all the in and inout paths
		String cp = ((ServiceContext)cntxt).getCurrentPrefix();
		if (cp != null)
			return getPrefixedInPaths(cntxt, cp);
		else 
			return null;
	}
	
	public static List getPrefixedInPaths(Context cntxt, String prefixPath) throws ContextException {
		// get all the in and inout paths
		String inAssoc = Context.DIRECTION + APS + Context.DA_IN;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] inPaths = Contexts.getMarkedPaths(cntxt, inAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(cntxt, inoutAssoc);
		List list = new ArrayList();

		if (inPaths != null)
			for (int i = 0; i < inPaths.length; i++) {
				if (inPaths[i].startsWith(prefixPath))
					list.add(inPaths[i]);
			}
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++)
				if (inoutPaths[i].startsWith(prefixPath))
					list.add(inoutPaths[i]);

		return list;
	}
	
	/**
	 * Returns a list of all paths marked as data output.
	 * 
	 * @param cntxt
	 *            a service context
	 * @return list of all paths marked as data output
	 * @throws ContextException
	 */
	public static List<String> getOutPaths(Context cntxt) throws ContextException {
		// get all the in and inout paths
		String outAssoc = Context.DIRECTION + APS + Context.DA_OUT;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] outPaths = getMarkedPaths(cntxt, outAssoc);
		String[] inoutPaths = getMarkedPaths(cntxt, inoutAssoc);
        int cap = (outPaths == null ? 0 : outPaths.length) + (inoutPaths == null ? 0 : inoutPaths.length);
		List<String> list = new ArrayList<String>(cap);

		if (outPaths != null)
            Collections.addAll(list, outPaths);
		if (inoutPaths != null)
            Collections.addAll(list, inoutPaths);
		return list;
	}

	public static List getNamedOutPaths(Context cntxt) throws ContextException {
		// get all the in and out paths
		return getPrefixedOutPaths(cntxt, ((ServiceContext)cntxt).getCurrentSelector());
	}
	
	public static List getPrefixedOutPaths(Context cntxt) throws ContextException {
		// get all the in and out paths
		return getPrefixedOutPaths(cntxt, ((ServiceContext)cntxt).getCurrentPrefix());
	}
	
	public static List getPrefixedOutPaths(Context cntxt, String prefix) throws ContextException {
		// get all the in and out paths
		String outAssoc = Context.DIRECTION + APS + Context.DA_OUT;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] outPaths = Contexts.getMarkedPaths(cntxt, outAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(cntxt, inoutAssoc);
		List list = new ArrayList();

		if (outPaths != null)
			for (int i = 0; i < outPaths.length; i++) {
				if (outPaths[i].startsWith(prefix))
					list.add(outPaths[i]);
			}
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++)
				if (inoutPaths[i].startsWith(prefix))
					list.add(inoutPaths[i]);
		return list;
	}
	/**
	 * Returns a map of all paths marked as data input.
	 * 
	 * @param cntxt
	 *            a service context
	 * @return map of all paths marked as data input
	 * @throws ContextException
	 */
	public static Hashtable getInPathsMap(Context cntxt)
			throws ContextException {
		// get all the in and inout paths
		String inAssoc = Context.DIRECTION + APS + Context.DA_IN;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] inPaths = Contexts.getMarkedPaths(cntxt, inAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(cntxt, inoutAssoc);
		Hashtable inpaths = new Hashtable();

		if (inPaths != null)
			for (int i = 0; i < inPaths.length; i++)
				inpaths.put(inPaths[i], cntxt.getMetaattributeValue(inPaths[i],
						Context.CONTEXT_PARAMETER));
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++)
				inpaths.put(inoutPaths[i], cntxt.getMetaattributeValue(
						inoutPaths[i], Context.CONTEXT_PARAMETER));
		return inpaths;
	}

	/**
	 * Returns a map of all path marked as output with corresponding
	 * associations.
	 * 
	 * @param cntxt
	 *            a service context
	 * @return map of all path marked as output with corresponding associations
	 * @throws ContextException
	 */
	public static Hashtable getOutPathsMap(Context cntxt)
			throws ContextException {
		// get all the out and inout paths
		String outAssoc = Context.DIRECTION + APS + Context.DA_OUT;
		String inoutAssoc = Context.DIRECTION + APS + Context.DA_INOUT;
		String[] outPaths = Contexts.getMarkedPaths(cntxt, outAssoc);
		String[] inoutPaths = Contexts.getMarkedPaths(cntxt, inoutAssoc);
		Hashtable inpaths = new Hashtable();

		if (outPaths != null)
			for (int i = 0; i < outPaths.length; i++)
				inpaths.put(outPaths[i], cntxt.getMetaattributeValue(
						outPaths[i], Context.CONTEXT_PARAMETER));
		if (inoutPaths != null)
			for (int i = 0; i < inoutPaths.length; i++)
				inpaths.put(inoutPaths[i], cntxt.getMetaattributeValue(
						inoutPaths[i], Context.CONTEXT_PARAMETER));
		return inpaths;
	}

	public static void copyContextNodesFrom(Context toContext,
			Context fromContext) throws ContextException {
		// copy all sorcerNodes from fromContext to this context.
		for (Enumeration e = fromContext.contextPaths(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			if (fromContext.getValue(key) instanceof ContextNode)
				toContext.putValue(key, fromContext.getValue(key));
		}
	}

    public static Hashtable getContextParameterMap(Context sc) {
		return (sc != null) ? (Hashtable) sc.getMetacontext().get(
				Context.CONTEXT_PARAMETER) : null;
	}
	
	public static void copyValue(Context fromContext, String fromPath,
			Context toContext, String toPath) throws ContextException {
		toContext.putValue(toPath, fromContext.getValue(fromPath));
	}

	public static Object putOutValue(Context cntxt, String path, Object value)
			throws ContextException {
		cntxt.putValue(path, value);
		markOut(cntxt, path);
		return value;
	}

	public static Object putInValue(Context cntxt, String path, Object value)
			throws ContextException {
		cntxt.putValue(path, value);
		markIn(cntxt, path);
		return value;
	}

	public static Object putOutValue(Context cntxt, String path, Object value,
			String association) throws ContextException {
		cntxt.putValue(path, value);
		markOut(cntxt, path);
		cntxt.mark(path, association);
		return value;
	}

	public static Object putInValue(Context cntxt, String path, Object value,
			String association) throws ContextException {
		cntxt.putValue(path, value);
		markIn(cntxt, path);
		cntxt.mark(path, association);
		return value;
	}

	public static String getContextParameterPath(String contextParameter) {
		return (contextParameter == null) ? null : StringUtils.secondToken(
				contextParameter, SEP);
	}

	public static String getContextParameterID(String contextParameter) {
		return (contextParameter == null) ? null : StringUtils.thirdToken(
				contextParameter, SEP);
	}

	public static String getContextParameterDirection(String contextParameter) {
		return (contextParameter == null) ? null : StringUtils.firstToken(
				contextParameter, SEP);
	}

	public static Object putDirectionalValue(Context context, String path,
			Object node, String attribute, String value)
			throws ContextException {
		Uuid contextID = context.getId();
		if (value == null)
			value = SorcerConstants.NULL;
		StringBuffer sb = new StringBuffer();
		sb
				.append(Context.CONTEXT_PARAMETER)
				.append(SorcerConstants.APS)
				.append(attribute)
				.append(SorcerConstants.APS)
				.append(value)
				.append(SorcerConstants.APS)
				.append(
						contextID == null ? SorcerConstants.NULL
								: contextID);

		if (node instanceof ContextNode)
			((ContextNode) node).setDA(attribute);

		return context.putValue(path, value, sb.toString());
	}

    public static Enumeration getPathsWithoutLinkedPaths(
            ServiceContext contextTree, Enumeration e, boolean linkStop)
            throws ContextException {
        Vector keys = new Vector();
        String path;
        ContextLink link;
        Context subcntxt = null;
        SorcerPrincipal principal = contextTree.getPrincipal();
        while (e.hasMoreElements()) {
            String key1 = (String) e.nextElement();
            if ((contextTree.getValue(key1) instanceof ContextLink)) {
                link = (ContextLink) contextTree.getValue(key1);
                if (!linkStop) {
                    // get subcontext for recursion
                    try {
                        subcntxt = link.getContext(principal).getContext(
                                link.getOffset().trim());
                    } catch (RemoteException ex) {
                        throw new ContextException(ex);
                    }
                    // getSubcontext cuts above, which is what we want
                    Enumeration el = getPathsWithoutLinkedPaths(
                            (ServiceContext) subcntxt,
                            ((ServiceContext) subcntxt).keys(), true);
                    while (el.hasMoreElements()) {
                        path = (String) el.nextElement();
                        String str = key1 + SorcerConstants.CPS + path;
                        keys.addElement(str);
                    }
                    keys.removeElement(key1);
                } else if (linkStop) {
                    keys.addElement(key1);
                }
            } else {
                keys.addElement(key1);
            }
        }
        StringUtils.bubbleSort(keys);
        return keys.elements();
    }

    public static Enumeration getPathsWithoutLinkedPaths(
            ServiceContext contextTree, Enumeration e) throws ContextException {
        Vector keys = new Vector();
        String path;
        ContextLink link;
        Context subcntxt;
        SorcerPrincipal principal = contextTree.getPrincipal();
        while (e.hasMoreElements()) {
            String key1 = (String) e.nextElement();
            if ((contextTree.getValue(key1) instanceof ContextLink)) {
                link = (ContextLink) contextTree.getValue(key1);
                try {
                    subcntxt = link.getContext(principal).getContext(
                            link.getOffset().trim());
                } catch (RemoteException ex) {
                    throw new ContextException(ex);
                }
                // getSubcontext cuts above, which is what we want
                Enumeration el = getPathsWithoutLinkedPaths(
                        (ServiceContext) subcntxt,
                        ((ServiceContext) subcntxt).keys());
                while (el.hasMoreElements()) {
                    path = (String) el.nextElement();
                    String str = key1 + SorcerConstants.CPS + path;
                    keys.addElement(str);
                }
                keys.removeElement(key1);
            } else {
                keys.addElement(key1);
            }
        }
        StringUtils.bubbleSort(keys);
        return keys.elements();
    }

	public static String[] getPathsWithAttribute(Context cntxt, String attribute)
			throws ContextException {
		Hashtable values;
		if (!cntxt.isAttribute(attribute))
			throw new ContextException("No data attribute defined: "
					+ attribute);

		Vector keys = new Vector();
		if (cntxt.isSingletonAttribute(attribute)) {
			values = (Hashtable) cntxt.getMetacontext().get(attribute);
			if (values != null) { // if no attributes are set, values==null;
				Enumeration e = values.keys();
				while (e.hasMoreElements())
					keys.addElement(e.nextElement());
			}
		} else {
			// it is a metaattribute
			String metapath = cntxt.getLocalMetapath(attribute);
			if (metapath != null) {
				String[] attrs = StringUtils.tokenize(metapath,
						APS);
				String[][] paths = new String[attrs.length][];
				int ii = -1;
				for (int i = 0; i < attrs.length; i++) {
					paths[i] = getPathsWithAttribute(cntxt, attrs[i]);
					if (paths[i] == null) {
						ii = -1;
						break; // i.e. no possible match
					}
					if (ii < 0 || paths[i].length > paths[ii].length) {
						ii = i;
					}
				}

				if (ii >= 0) {
					// The common paths across the paths[][] array are
					// matches. Said another way, the paths[][] array
					// contains all the paths that match attributes in the
					// metapath. paths[0][] are the matches for the first
					// element of the metapath, paths[1][] for the next,
					// etc. Therefore, the matches that are common for
					// each element of the metapath are the ones in which
					// we have interest.
					String candidate;
					int match, thisMatch;
					// go through each element of one with most matches
					for (int i = 0; i < paths[ii].length; i++) {
						candidate = paths[ii][i];
						// now look for paths.length-1 matches...
						match = 0;
						for (int j = 0; j < paths.length; j++) {
							if (j == ii)
								continue;
							thisMatch = 0;
							for (int k = 0; k < paths[j].length; k++)
								if (candidate.equals(paths[j][k])) {
									match++;
									thisMatch++;
									break;
								}
							if (thisMatch == 0)
								break; // no possible match for this candidate
						}
						// System.out.println("candidate="+candidate+"
						// match="+match+" required maches="+(paths.length-1));
						if (match == paths.length - 1)
							keys.addElement(candidate);
					}
				}
			}
		}
		// above we just checked the top-level context; next, check
		// all the top-level LINKED contexts (which in turn will check
		// all their top-level linked contexts, etc.)
		Enumeration e = cntxt.localLinkPaths();
		Link link;
		String keysInLink[], linkPath;
		while (e.hasMoreElements()) {
			linkPath = (String) e.nextElement();
			link = (Link) cntxt.get(linkPath);
			keysInLink = getPathsWithAttribute(((ServiceContext) cntxt)
					.getLinkedContext(link), attribute);
			if (keysInLink != null)
				for (int i = 0; i < keysInLink.length; i++)
					keys.addElement(linkPath + CPS
							+ keysInLink[i]);
		}
		String[] keysArray = new String[keys.size()];
		keys.copyInto(keysArray);
		return keysArray;
	}

	public static String[] getMarkedPaths(Context cntxt, String association)
			throws ContextException {
		String attr, value, key;
		Hashtable values;
		// java 1.4.0 regex
		// Pattern p;
		// Matcher m;
		if (association == null)
			return null;
		int index = association.indexOf(APS);
		if (index < 0)
			return null;

		attr = association.substring(0, index);
		value = association.substring(index + 1);
		if (!cntxt.isAttribute(attr))
			throw new ContextException("No Attribute defined: " + attr);

		Vector keys = new Vector();
		if (cntxt.isSingletonAttribute(attr)) {
			values = (Hashtable) cntxt.getMetacontext().get(attr);
			if (values != null) { // if there are no attributes set,
				// values==null;
				Enumeration e = values.keys();
				while (e.hasMoreElements()) {
					key = (String) e.nextElement();
					/*
					 * java 1.4.0 regex p = Pattern.compile(value); m =
					 * p.matcher((String)values.get(key)); if (m.find())
					 * keys.addElement(key);
					 */
					if (values.get(key).equals(value))
						keys.addElement(key);
				}
			}
		} else {
			// it is a metaattribute
			String metapath = cntxt.getLocalMetapath(attr);
			if (metapath != null) {
				String[] attrs = StringUtils.tokenize(metapath,
						APS);
				String[] vals = StringUtils.tokenize(value, APS);
				if (attrs.length != vals.length)
					throw new ContextException("Invalid association: \""
							+ association + "\"  metaattribute \"" + attr
							+ "\" is defined with metapath =\"" + metapath
							+ "\"");
				String[][] paths = new String[attrs.length][];
				int ii = -1;
				for (int i = 0; i < attrs.length; i++) {
					paths[i] = getMarkedPaths(cntxt, attrs[i]
							+ APS + vals[i]);
					if (paths[i] == null) {
						ii = -1;
						break; // i.e. no possible match
					}
					if (ii < 0 || paths[i].length > paths[ii].length) {
						ii = i;
					}
				}

				if (ii >= 0) {
					// The common paths across the paths[][] array are
					// matches. Said another way, the paths[][] array
					// contains all the paths that match attributes in the
					// metapath. paths[0][] are the matches for the first
					// element of the metapath, paths[1][] for the next,
					// etc. Therefore, the matches that are common for
					// each element of the metapath are the ones in which
					// we have interest.
					String candidate;
					int match, thisMatch;
					// go through each element of one with most matches
					for (int i = 0; i < paths[ii].length; i++) {
						candidate = paths[ii][i];
						// now look for paths.length-1 matches...
						match = 0;
						for (int j = 0; j < paths.length; j++) {
							if (j == ii)
								continue;
							thisMatch = 0;
							for (int k = 0; k < paths[j].length; k++)
								if (candidate.equals(paths[j][k])) {
									match++;
									thisMatch++;
									break;
								}
							if (thisMatch == 0)
								break; // no possible match for this candidate
						}
						// System.out.println("candidate="+candidate+"
						// match="+match+" required maches="+(paths.length-1));
						if (match == paths.length - 1)
							keys.addElement(candidate);
					}
				}
			}
		}
		// above we just checked the top-level context; next, check
		// all the top-level LINKED contexts (which in turn will check
		// all their top-level linked contexts, etc.)
		Enumeration e = cntxt.localLinkPaths();
		Link link;
		String keysInLink[], linkPath;
		while (e.hasMoreElements()) {
			linkPath = (String) e.nextElement();
			link = (Link) cntxt.get(linkPath);
			keysInLink = getMarkedPaths(((ServiceContext) cntxt)
					.getLinkedContext(link), association);
			if (keysInLink != null)
				for (int i = 0; i < keysInLink.length; i++)
					keys.addElement(linkPath + CPS
							+ keysInLink[i]);
		}
		String[] keysArray = new String[keys.size()];
		keys.copyInto(keysArray);
		return keysArray;
	}

	// used by Personal Java UI code
	public static Hashtable getMapStartsWith(Context context, String path) {
		Hashtable map = new Hashtable();
		try {
			List list = Contexts.getKeysStartsWith(context, path);
			Iterator i = list.iterator();
			while (i.hasNext()) {
				Object p = i.next();
				map.put(p, context.getValue((String) p));
			}
		} catch (ContextException e) {
			e.printStackTrace();
		}
		return map;
	}

	public static boolean checkIfPathBeginsWith(Context context, String path)
			throws ContextException {
		List list = getKeysStartsWith(context, path);
		if (list != null && list.size() >= 1)
			return true;
		else
			return false;
	}

	/**
	 * Get all associations (simple and composite) in the provided context.
	 * 
	 * @param context
	 *            a servcie context
	 * @return Enumeration of associations (of type <code>String</code>)
	 * @throws ContextException
	 */
	public static Enumeration getAssociations(Context context)
			throws ContextException {
		Enumeration e = context.localAttributes();
		Enumeration e1;
		String attributeName;
		Object val;
		Vector values = new Vector();
		if (e != null)
			while (e.hasMoreElements()) {
				attributeName = (String) e.nextElement();
				e1 = ((Hashtable) context.getMetacontext().get(attributeName))
						.elements();
				while (e1.hasMoreElements()) {
					val = e1.nextElement();
					if (!values.contains(attributeName + APS + val))
						values.addElement(attributeName + APS + val);
				}
			}
		// we just added all the attribute-value pairs from
		// the top-level context; check first level links,
		// which in turn will check their links, etc., etc.
		Enumeration e2 = context.localLinkPaths();
		Link link;
		String linkPath, assoc;
		if (e2 != null)
			while (e2.hasMoreElements()) {
				linkPath = (String) e2.nextElement();
				link = (Link) context.get(linkPath);
				Enumeration associations = getAssociations(((ServiceContext) context)
						.getLinkedContext(link));
				if (associations != null)
					while (associations.hasMoreElements()) {
						assoc = (String) associations.nextElement();
						if (!values.contains(assoc))
							values.addElement(assoc);
					}
			}
		return values.elements();
	}

	/**
	 * Get all singleton associations (attribute-value pairs) at the specified
	 * context node.
	 * 
	 * @param context
	 *            a service context
	 * @param key
	 *            the location in the context
	 * @return Enumeration of associations (of type <code>String</code>)
	 * @throws ContextException
	 */
	public static Enumeration getAssociations(Context context, String key)
			throws ContextException {
		Enumeration es = getSimpleAssociations(context, key);
		Enumeration em = context.metaassociations(key);
		Vector allAssoc = new Vector();
		while (es.hasMoreElements())
			allAssoc.addElement(es.nextElement());
		while (em.hasMoreElements())
			allAssoc.addElement(em.nextElement());
		return allAssoc.elements();
	}

	public static Enumeration getSimpleAssociations(Context context, String key)
			throws ContextException {
		Object val;
		String attributeName;
		Vector values = new Vector();

		// locate the context and context path for this key
		Object[] map = context.getContextMapping(key);

		Context cntxt = (Context) map[0];
		String mappedKey = (String) map[1];

		Enumeration e = context.localSimpleAttributes();
		if (e != null) {
			while (e.hasMoreElements()) {
				attributeName = (String) e.nextElement();
				val = cntxt
						.getSingletonAttributeValue(mappedKey, attributeName);
				if (val != null)
					values.addElement(attributeName + APS + val);
			}
			return values.elements();
		} else
			return null;
	}

	public static boolean containsContextNodeWithMetaAssoc(Context context,
			String metaAssoc) throws ContextException {
		String attr, value, key;
		Hashtable values;
		attr = metaAssoc.substring(0, metaAssoc.indexOf(APS));
		value = metaAssoc.substring(metaAssoc.indexOf(APS) + 1);
		System.out.println("attr, value" + attr + "," + value);
		if (!context.isMetaattribute(attr))
			return false;
		values = (Hashtable) context.getMetacontext().get(attr);
		System.out.println("values" + values);
		Enumeration e = values.keys();
		while (e.hasMoreElements()) {
			key = (String) e.nextElement();
			if (values.get(key).equals(value))
				if (((ServiceContext) context).get(key) instanceof ContextNode)
					return true;
		}
		return false;
	}

	public static String[] getContextNodePathsWithAssoc(Context context,
			String association) throws ContextException {
		Vector contextNodes = new Vector();
		String[] paths = getMarkedPaths(context, association);
		if (paths == null)
			return (String[]) null;
		for (int i = 0; i < paths.length; i++)
			if (context.getValue(paths[i]) instanceof ContextNode)
				contextNodes.addElement(paths[i]);
		String[] contextNodePaths = new String[contextNodes.size()];
		contextNodes.copyInto(contextNodePaths);
		return contextNodePaths;
	}

	public static String[] getContextNodePaths(Context context)
			throws ContextException {
		String path;
		Vector contextNodes = new Vector();
		Enumeration e = context.contextPaths();
		while (e.hasMoreElements()) {
			path = (String) e.nextElement();
			if (context.getValue(path) instanceof ContextNode)
				contextNodes.addElement(path);
		}
		String[] contextNodePaths = new String[contextNodes.size()];
		contextNodes.copyInto(contextNodePaths);
		return contextNodePaths;
	}

   public static String getDirection(Context context, String path) throws ContextException {
        Enumeration ens = Contexts.getSimpleAssociations(context, path);
        while (ens.hasMoreElements()) {
            String assoc = (String)ens.nextElement();
            if ((assoc).startsWith(Context.DIRECTION)) {
                return assoc.substring(assoc.indexOf(SEP)+1, assoc.length());
            }
        }
        return null;
    }


}
