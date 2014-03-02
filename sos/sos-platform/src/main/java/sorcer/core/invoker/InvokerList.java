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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import sorcer.co.tuple.Tuple2;
import sorcer.service.EvaluationException;

/**
 * @author Mike Sobolewski
 */

@SuppressWarnings("rawtypes")
public class InvokerList extends ArrayList<Invoker> {
	
	private static final long serialVersionUID = -8243166261682952591L;
	
	public InvokerList() {
		super();
	}

	public InvokerList(int size) {
		super(size);
	}

	public InvokerList(Set<Invoker> invokerSet) {
		addAll(invokerSet);
	}

	public InvokerList(InvokerList... invokerLists) {
		super();
		for (InvokerList sil : invokerLists) {
			addAll(sil);
		}
	}

	public InvokerList(Invoker[] invokerArray) {
		super();
		for (Invoker si : invokerArray) {
			add(si);
		}
	}

	public <T> InvokerList(List<Invoker> invokerList) {
		super();
		for (Invoker si : invokerList) {
			add(si);
		}
	}

	public Invoker getInvoker(String invokerName) throws EvaluationException {
		for (Invoker si : this) {
			if (si.getName().equals(invokerName)) {
				return si;
			}
		}
		return null;
	}

	public void setInvoker(String invokerName, Invoker invoker)
			throws EvaluationException {
		for (int i = 0; i < this.size(); i++) {
			if (get(i).getName().equals(invokerName)) {
				set(i, invoker);
				break;
			}
		}
	}

	public InvokerList selectInvokers(List<String>... invokerNames) {
		List<String> allNames = new ArrayList<String>();
		for (List<String> nl : invokerNames) {
			allNames.addAll(nl);
		}
		InvokerList out = new InvokerList();
		for (Invoker si : this) {
			if (allNames.contains(si.getName())) {
				out.add(si);
			}
		}
		return out;
	}

	public InvokerList selectInvokers(String... invokerNames) {
		List<String> names = Arrays.asList(invokerNames);
		InvokerList out = new InvokerList();
		for (Invoker si : this) {
			if (names.contains(si.getName())) {
				out.add(si);
			}
		}
		return out;
	}

	@Override
	public boolean remove(Object obj) {
		if (obj == null || !(obj instanceof Invoker)) {
			return false;
		} else {
			for (Invoker si : this) {
				if (si.getName().equals(((Invoker) obj).getName())) {
					super.remove(si);
					return true;
				}
			}
		}
		return false;
	}

	public List<String> getNames() {
		List<String> names = new ArrayList<String>(size());
		for (int i = 0; i < size(); i++) {
			names.add(get(i).getName());
		}
		return names;
	}

	public List<Tuple2<String, Object>> getEntries() throws EvaluationException {
		List<Tuple2<String, Object>> entries = new ArrayList<Tuple2<String, Object>>(
				size());
		for (int i = 0; i < size(); i++) {
			try {
				entries.add(new Tuple2<String, Object>(get(i).getName(), get(i)
						.invoke()));
			} catch (RemoteException e) {
				throw new EvaluationException(e);
			}
		}
		return entries;
	}

	public List<Object> getValues() throws EvaluationException, RemoteException {
		List<Object> values = new ArrayList<Object>(size());
		for (int i = 0; i < size(); i++) {
			values.add(get(i).invoke());
		}
		return values;
	}

	public Invoker[] toArray() {
		Invoker[] sia = new Invoker[size()];
		return toArray(sia);
	}

	public static InvokerList asList(Invoker[] array) {
		InvokerList sil = new InvokerList(array.length);
		for (Invoker si : array)
			sil.add(si);
		return sil;
	}

	public String describe() {
		StringBuilder sb = new StringBuilder();
		sb.append(getNames().toString());
		sb.append("\n");
		for (int i = 0; i < size(); i++) {
			sb.append(get(i).getName());
			sb.append("\n");
			sb.append(get(i));
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return getNames().toString();
	}
}
