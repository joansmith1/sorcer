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

package sorcer.co.tuple;

import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;

import net.jini.id.Uuid;
import sorcer.service.*;

public class Tuple2<T1, T2> implements Arg, Serializable, Setter, Evaluation<T2>, Identifiable {
	private  static final long serialVersionUID = -6519678282532888568L;
	public T1 _1 = null;
	public T2 _2 = null;
	private Uuid entryUuid;
	public boolean isPersistent = false;
	public URL datastoreURL;

	public Tuple2() {}
	
	public Tuple2(T1 x1, T2 x2) {
		_1 = x1;
		_2 = x2;
	}
	
	public T1 key() {
		return _1;
	}
	
	public String path() {
		return (String)_1;
	}
	
	public T2 value() {
		return _2;
	}
	
	public Strategy strategy() {
		return (Strategy)_2;
	}
	
	/* (non-Javadoc)
	 * @see sorcer.service.Arg#getName()
	 */
	@Override
	public String getName() {
		return ""+_1;
	}
	
	@Override
	public String toString() {
		return "[" + _1 + ":" + _2 + "]";
	}

	/* (non-Javadoc)
 * @see sorcer.service.Evaluation#asis()
 */
	@Override
	public T2 asis() throws EvaluationException, RemoteException {
		return _2;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#getValue(sorcer.service.Arg[])
	 */
	@Override
	public T2 getValue(Arg... entries) throws EvaluationException,
			RemoteException {
		try {
			substitute(entries);
		} catch (SetterException e) {
			throw new EvaluationException(e);
		}
		return this._2;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Setter#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) throws SetterException, RemoteException {
		this._2 = (T2) value;
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Evaluation#substitute(sorcer.service.Arg[])
 	 */
	@Override
	public Evaluation<T2> substitute(Arg... entries) throws SetterException,
			RemoteException {
		if (entries != null) {
			for (Arg a : entries) {
				if (a.getName().equals(getName()) && a instanceof Entry) {
					_2 = ((Entry<T2>) a).value();
				}
			}
		}
		return this;
	}


	@Override
	public boolean equals(Object object) {
		if (object instanceof Tuple2) {
			Tuple2<?,?> pair = (Tuple2<?,?>) object;
			if (_1.equals(pair._1) && _2.equals(pair._2))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
 * @see sorcer.service.Setter#isPersistent()
 */
	@Override
	public boolean isPersistent() {
		return isPersistent;
	}
	
	@Override
	public int hashCode() {
		return 2 * 31 + _1.hashCode() + _2.hashCode();
	}

	/* (non-Javadoc)
	 * @see sorcer.service.Identifiable#getId()
	 */
	@Override
	public Object getId() {
		return entryUuid;
	}

	public void setId(Uuid id) {
		entryUuid = id;
	}

	/**
	 * <p>
	 * Assigns the flag for persistent storage of values of this entry
	 * </p>
	 *
	 * @param isPersistent
	 *            the isPersistent to set
	 * @return nothing
	 */
	public void setPersistent(boolean isPersistent) {
		this.isPersistent = isPersistent;
	}

}