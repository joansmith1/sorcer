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
package sorcer.core.invoker;

import java.rmi.RemoteException;

import net.jini.core.transaction.Transaction;
import sorcer.co.tuple.Entry;
import sorcer.core.context.model.par.Par;
import sorcer.service.*;
import static sorcer.service.Signature.ReturnPath;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExertInvoker extends Invoker implements ExertionInvoking {
	private static final long serialVersionUID = -8257643691945276788L;
	private Exertion exertion;
	private String path;
	private Exertion evaluatedExertion;
	private Transaction txn;
	private Object updatedValue;

	{
		defaultName = "xrtInvoker-";
	}
	
	public ExertInvoker(String name, Exertion exertion, String path, Par... pars) {
		super(name);
		this.path = path;
		this.exertion = exertion;
		this.pars = new ArgSet(pars);
	}

	public ExertInvoker(Exertion exertion, String path, Par... pars) {
		this(null, exertion, path, pars);
	}
	
	public ExertInvoker(Exertion exertion, Par... pars) {
		this(null, exertion, null, pars);
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.service.Args[])
	 */
	@Override
	public Object getValue(Arg... entries) throws InvocationException,
			RemoteException {
		Context cxt = null;
		try {
			evaluatedExertion = exertion.exert(txn);
			ReturnPath returnPath = evaluatedExertion.getDataContext()
					.getReturnPath();
			if (evaluatedExertion instanceof Job) {
				cxt = ((Job) evaluatedExertion).getJobContext();
			} else {
				cxt = evaluatedExertion.getContext();
			}

			if (returnPath != null) {
				if (returnPath.path == null)
					return cxt;
				else if (returnPath.path.equals("self"))
					return this;
				else
					return cxt.getReturnValue();
			} else {
				if (path != null)
					return cxt.getValue(path);
			}
		} catch (Exception e) {
			throw new InvocationException(e);
		}
		return cxt;
	}
	
	public Exertion getExertion() {
		return exertion;
	}

	public Exertion getEvaluatedExertion() {
		return evaluatedExertion;
	}

	public void substitute(Entry... entries) throws EvaluationException,
			RemoteException {
		((ServiceExertion)exertion).substitute(entries);
	}

	public Object getUpdatedValue() {
		return updatedValue;
	}

	public void setUpdatedValue(Object updatedValue) {
		this.updatedValue = updatedValue;
	}
}
