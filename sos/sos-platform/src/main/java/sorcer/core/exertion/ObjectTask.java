/*
 * Copyright 2012 the original author or authors.
 * Copyright 2012 SorcerSoft.org.
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

import java.rmi.RemoteException;

import net.jini.core.transaction.Transaction;
import sorcer.core.context.ServiceContext;
import sorcer.core.signature.ObjectSignature;
import sorcer.service.Context;
import sorcer.service.ContextException;
import sorcer.service.ExertionException;
import sorcer.service.Signature;
import sorcer.service.SignatureException;
import sorcer.service.Task;
//import sorcer.vfe.evaluator.MethodEvaluator;

/**
 * The SORCER object task extending the basic task implementation {@link Task}.
 * 
 * @author Mike Sobolewski
 */
@SuppressWarnings("rawtypes")
public class ObjectTask extends Task {

	static final long serialVersionUID = 1793342047789581449L;

	public ObjectTask() { }
	
	public ObjectTask(String name) {
		super(name);
	}
	
	public ObjectTask(String name, Signature... signatures) {
		super(name);
		for (Signature s : signatures) {
			if (s instanceof ObjectSignature)
				addSignature(s);
		}
	}

	public ObjectTask(String name, String description, Signature signature)
			throws SignatureException {
		super(name);
		if (signature instanceof ObjectSignature)
			addSignature(signature);
		else 
			throw new SignatureException("Object task requires ObjectSignature: "
					+ signature);
        // TODO VFE related

		/*if (((ObjectSignature)signature).getEvaluator() == null)
			try {
				((ObjectSignature)signature).createEvaluator();
			} catch (Exception e) {
				e.printStackTrace();
				throw new SignatureException(e);
			}*/
		this.description = description;
	}
	
	public ObjectTask(String name, Signature signature, Context context)
			throws SignatureException {
		this(name, signature);
		this.dataContext = (ServiceContext) context;
	}

    public Task doTask(Transaction txn) throws ExertionException,
            SignatureException, RemoteException {
        dataContext.setCurrentSelector(getProcessSignature().getSelector());
        dataContext.setCurrentPrefix(getProcessSignature().getPrefix());
        try {
            if (getProcessSignature().getReturnPath() != null)
                dataContext.setReturnPath(getProcessSignature().getReturnPath());

            ObjectSignature os = (ObjectSignature) getProcessSignature();
            Class[] paramTypes = new Class[] { Context.class };
            Object[] parameters = new Object[] { dataContext };
            if (dataContext.getArgsPath() != null) {
                paramTypes = os.getTypes();
                parameters = (Object[]) dataContext.getArgs();
            }
            Object result = ((ObjectSignature) getProcessSignature())
                    .initInstance(parameters, paramTypes);

            if (dataContext.getReturnPath() != null) {
                if (result instanceof Context) {
                    dataContext.setReturnValue(((Context) result).getValue(dataContext
                            .getReturnPath().path));
                } else {
                    dataContext.setReturnValue(result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            dataContext.reportException(e);
        }
        return this;
    }

    // TODO VFE related
    /*public Task doTask(Transaction txn) throws ExertionException, SignatureException, RemoteException {
		ObjectSignature os = (ObjectSignature) getProcessSignature();
		dataContext.setCurrentSelector(os.getSelector());
		dataContext.setCurrentPrefix(os.getPrefix());
		//MethodEvaluator evaluator = ((ObjectSignature) getProcessSignature())
		//		.getEvaluator();
		try {
			if (evaluator == null) {
				if (os.getTarget() != null)
					evaluator = new MethodEvaluator(os.getTarget(), os.getSelector());
				else
					evaluator = new MethodEvaluator(os.newInstance(), os.getSelector());
			}
			if (os.getReturnPath() != null)
				dataContext.setReturnPath(os.getReturnPath());
			if (getArgs() == null  && os.getTypes() == null) {
				// assume this task context is used by the signature's provider
				evaluator.setParameterTypes(new Class[] { Context.class });
				evaluator.setContext(dataContext);
			} 
			else if (dataContext.getArgsPath() != null) {
				evaluator.setArgs(os.getTypes(), (Object[]) getArgs());
			}
			//evaluator.setParameters(context);
			Object result = evaluator.evaluate();
			if (result instanceof Context) {
				if (dataContext.getReturnPath() != null) {
					dataContext.setReturnValue(((Context) result).getValue(dataContext
						.getReturnPath().path));
				} else {
					dataContext.append((Context)result);
				}
			} else {
				dataContext.setReturnValue(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			dataContext.reportException(e);
		}
		dataContext.appendTrace(evaluator.toString());
		return this;
	} */
	
	public Object getArgs() throws ContextException {
		return dataContext.getArgs();
	}

}
