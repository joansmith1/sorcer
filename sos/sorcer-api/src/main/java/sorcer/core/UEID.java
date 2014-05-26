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
package sorcer.core;

import net.jini.core.lookup.ServiceID;
import sorcer.util.StringUtils;

import java.io.Serializable;

/**
 * Sometimes it's critical to identify an exertion associated with a provider.
 * For example, a broker or monitor might like to have an identifier associated
 * with the exertion which identifies not only the exertion but also the
 * provider who's the owner.
 * 
 * In other words, this is a kind of cookie which uniquely associates a provider
 * with a particular exertion.
 * 
 * This object is immutable.
 */

public class UEID implements Serializable {

    static final long serialVersionUID = -687991492884005033L;

	public final ServiceID sid;

	public final String exertionID;

	public UEID(ServiceID sid, String exertionID) {

		if (exertionID == null)
			throw new NullPointerException("exertionID cannot be NULL");

		this.sid = sid;
		this.exertionID = exertionID;
	}

	public boolean equals(Object o) {
		if (!(o instanceof UEID))
			return false;

		UEID other = (UEID) o;

		return other.sid.equals(sid) && other.exertionID.equals(exertionID);
	}

	public String asString() {
		String sidString = (sid == null) ? "|" : sid.getLeastSignificantBits()
				+ "|" + sid.getMostSignificantBits();
		return sidString + "|" + exertionID;
	}

	public static UEID fromString(String ueid) {
		String[] str = StringUtils.tokenize(ueid, "|");
		ServiceID sid = null;

		try {
			if (!"".equals(str[0]) && !"".equals(str[0]))
				sid = new ServiceID(Long.parseLong(str[0]), Long
						.parseLong(str[1]));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return new UEID(sid, str[2]);
	}

}
