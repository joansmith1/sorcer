/*
 * Copyright 2013 the original author or authors.
 * Copyright 2013 SorcerSoft.org.
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
package sorcer.core.context.model.par;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.Principal;
import java.util.Iterator;
import java.util.logging.Logger;

import sorcer.core.SorcerConstants;
import sorcer.core.context.ApplicationDescription;
import sorcer.core.context.model.Variability;
import sorcer.service.Arg;
import sorcer.service.ArgException;
import sorcer.service.ArgSet;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.Evaluation;
import sorcer.service.EvaluationException;
import sorcer.service.Identifiable;
import sorcer.service.Identity;
import sorcer.service.Invocation;
import sorcer.service.InvocationException;
import sorcer.service.Mappable;
import sorcer.service.Scopable;
import sorcer.service.Setter;

/**
 * In service-based modeling, a parameter (for short a par) is a special kind of
 * variable, used in a service context {@link ParModel} to refer to one of the
 * pieces of data provided as input to the invokers (subroutines of the
 * context). These pieces of data are called arguments.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public abstract class Par<T> extends Identity implements Variability<T>, Arg, Mappable<T>, Evaluation<T>,
	Invocation<T>, Setter, Scopable, Comparable<T>, Serializable {

	private static final long serialVersionUID = 7495489980319169695L;
	 
	private static Logger logger = Logger.getLogger(Par.class.getName());

	protected String name;
	
	private Principal principal;

	protected T value;

	protected Context<T> scope;
	
	private boolean persistent = false;
				
	// data store URL for this par
	protected URL dbURL;

	// A context returning value at the path
	// that is this par name
	// Sorcer Mappable: Context, Exertion, or Var args
	protected Mappable mappable;

	public Par(String parname) {
		name = parname;
		value = null;
	}
	
	public Par(Identifiable identifiable) {
		name = identifiable.getName();
		value = (T)identifiable;
	}
	
	public Par(String parname, T argument) {
		name = parname;
		value = argument;
	}
	
	public Par(String parname, Object argument, Context scope) throws RemoteException {
		this(parname, (T)argument);
		this.scope = scope;
        setClosure(scope);
		if (argument instanceof Scopable)
			((Scopable)argument).setScope(this.scope);
	}


    public abstract void setClosure(Context scope);

	public Par(String name, String path, Mappable map) {
		this(name);
		value =  (T)path;
		mappable = map;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	public abstract void setValue(Object value) throws EvaluationException;

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getAsis()
	 */
	@Override
	public T asis() throws EvaluationException, RemoteException {
		return value;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public abstract T getValue(Arg... entries) throws EvaluationException, RemoteException;

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.co.tuple.Parameter[])
	 */
	@Override
	public Evaluation<T> substitute(Arg... parameters)
			throws EvaluationException, RemoteException {
		if (parameters == null)
			return this;
		for (Arg p : parameters) {
			if (p instanceof Par) {
				if (name.equals(((Par<T>)p).name)) {
					value = ((Par<T>)p).value;
				if (((Par<T>)p).getScope() != null)
					try {
						scope.append(((Par<T>)p).getScope());
					} catch (ContextException e) {
						throw new EvaluationException(e);
					}
				}
			}
		}
		return this;
	}
	
	public Context getScope() {
		return scope;
	}

	public abstract void setScope(Context scope);
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(T o) {
		if (o == null)
			throw new NullPointerException();
		if (o instanceof Par<?>)
			return name.compareTo(((Par<?>) o).getName());
		else
			return -1;
	}
	
	@Override
	public String toString() {
		return "par [" + name + ":" + value + "]";
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbedValue(java.lang.String)
	 */
	@Override
	public T getPerturbedValue(String varName) throws EvaluationException,
			RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Perturbation#getPerturbation()
	 */
	@Override
	public double getPerturbation() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getType()
	 */
	@Override
	public Type getType() {
		return Type.PARAMETER;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getDescription()
	 */
	@Override
	public ApplicationDescription getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getValueType()
	 */
	@Override
	public Class getValueType() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArgs()
	 */
	@Override
	public ArgSet getArgs() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArg(java.lang.String)
	 */
	@Override
	public T getArg(String varName) throws ArgException {
		try {
			return (T) scope.getValue(varName, null);
		} catch (ContextException e) {
			throw new ArgException(e);
		}
	}

	/**
	 * <p>
	 * Returns a Contextable (Context or Exertion) of this Par that by a its
	 * name provides values of this Par.
	 * </p>
	 * 
	 * @return the contextable
	 */
	public Mappable getContextable() {
		return mappable;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#isValueCurrent()
	 */
	@Override
	public boolean isValueCurrent() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged(java.lang.Object)
	 */
	@Override
	public void valueChanged(Object obj) throws EvaluationException,
			RemoteException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#valueChanged()
	 */
	@Override
	public void valueChanged() throws EvaluationException {
		// TODO Auto-generated method stub
		
	}

	public Principal getPrincipal() {
		return principal;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.vfe.Variability#getArgVar(java.lang.String)
	 */
	@Override
	public abstract Variability<T> getVariability(String name) throws ArgException;

	public abstract URL getDbURL() throws MalformedURLException;

	public URL getURL() throws ContextException {
		if (persistent) {
			if (mappable != null)
				return (URL)mappable.asis((String)value);
			else
				return (URL)value;
		}
		return null;
	}
	
	public void setDbURL(URL dbURL) {
		this.dbURL = dbURL;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.vfe.Persister#isPersistable()
	 */
	@Override
	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean state) {
		persistent = state;
	}
	
	public Mappable getMappable() {
		return mappable;
	}

	public void setMappable(Mappable mappable) {
		this.mappable = mappable;
	}
	
	public boolean isMappable() {
		return (mappable != null);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Arg... entries) throws RemoteException, InvocationException {
		try {
			if (value instanceof Invocation)
				return ((Invocation<T>) value).invoke(entries);
			else
				return getValue(entries);
		} catch (EvaluationException e) {
			throw new InvocationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Invocation#invoke(sorcer.service.Context, sorcer.service.Arg[])
	 */
	@Override
	public T invoke(Context context, Arg... entries) throws RemoteException,
			InvocationException {
		try {
			scope.append(context);
			return invoke(entries);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#getValue(java.lang.String, sorcer.service.Arg[])
	 */
	@Override
	public T getValue(String path, Arg... args) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				try {
					return (T)getValue(args);
				} catch (RemoteException e) {
					throw new ContextException(e);
				}
			else if (mappable != null)
				return (T)mappable.getValue(path.substring(name.length()), args);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#asis(java.lang.String)
	 */
	@Override
	public T asis(String path) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				return value;
			else if (mappable != null)
				return (T)mappable.asis(path.substring(name.length()));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Mappable#putValue(java.lang.String, java.lang.Object)
	 */
	@Override
	public T putValue(String path, Object value) throws ContextException {
		String[] attributes = path.split(SorcerConstants.CPS);
		if (attributes[0].equals(name)) {
			if (attributes.length == 1)
				this.value = (T)value;
			else if (mappable != null)
				mappable.putValue(path.substring(name.length()), value);
		}
		return (T)value;	
	}

	/* (non-Javadoc)
	 * @see sorcer.core.context.model.Variability#addArgs(sorcer.core.context.model.par.ParSet)
	 */
	@Override
	public void addArgs(ArgSet set) throws EvaluationException {
		Iterator<Arg> i = set.iterator();
		while (i.hasNext()) {
			Par par = (Par)i.next();
			try {
				putValue(par.getName(), par.asis());
			} catch (Exception e) {
				throw new EvaluationException(e);
			} 
		}
		
	}
	
	@Override
	public int hashCode() {
		int hash = name.length() + 1;
		return hash = hash * 31 + name.hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if (object instanceof Par
				&& ((Par) object).name.equals(name))
			return true;
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Scopable#setScope(java.lang.Object)
	 */
	public void setScope(Object scope) throws RemoteException {
		this.scope = (Context)scope;
		
	}
}
