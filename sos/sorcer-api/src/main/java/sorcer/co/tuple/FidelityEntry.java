/*
 * Copyright 2009 the original author or authors.
 * Copyright 2009 SorcerSoft.org.
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

import sorcer.core.context.model.var.FidelityInfo;

public class FidelityEntry<T> extends Entry<T> {
	private static final long serialVersionUID = -508307270964254478L;
	
	public FidelityEntry() {}
	
	public FidelityEntry(String x1, T value) {
		_1 = x1;
		_2 = value;
	}

	public FidelityEntry(String x1, FidelityInfo fidelity) {
		_1 = x1;
		this.fidelity = fidelity;
	}
	
	public FidelityInfo fidelity() {
		return fidelity;
	}
	
	public void fidelity(FidelityInfo fidelity) {
		this.fidelity = fidelity;
	}
}