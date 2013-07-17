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
package sorcer.core.grid.provider.dispatcher;

import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.LookupDiscovery;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryManager;
import sorcer.core.SorcerEnv;
import sorcer.service.Service;


import java.rmi.RMISecurityManager;

public class TestCaller {

    private static final int MAX_TRIES = 100;
    private static final int SLEEP_TIME = 100;
    ServiceDiscoveryManager sdm;
    LookupCache lCache1;

    public TestCaller() {
        try {
            LookupDiscovery disco = new LookupDiscovery(SorcerEnv.getLookupGroups());
            sdm = new ServiceDiscoveryManager(disco, new LeaseRenewalManager());
            lCache1 = sdm.createLookupCache(new ServiceTemplate(null, new Class[]{Service.class}, null),
                    null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        if (System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());

        System.out.println(">>>>>>>>>Caller = " + (new TestCaller()).getCaller());
    }

    public sorcer.core.Caller getCaller() {
        int tries = 0;
        while (tries < MAX_TRIES) {
            ServiceItem[] items = (lCache1.lookup(null, Integer.MAX_VALUE));
            for (int i = 0; i < items.length; i++)
                if (items[i].service != null &&
                        items[i].service instanceof sorcer.core.Caller) {
                    System.out.println(">>>>>>>>>>ServiceID = " + items[i].serviceID);
                    return (sorcer.core.Caller) items[i].service;
                }
            tries++;
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (Exception e) {
            }
        }
        return null;
    }
}
