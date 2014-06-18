/*
 * Copyright 2014 Sorcersoft.com S.A.
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

package sorcer.core.dispatch;

import net.jini.core.lease.Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.monitor.MonitoringSession;
import sorcer.service.*;

import java.rmi.RemoteException;

import static sorcer.service.Exec.FAILED;

/**
 * @author Rafał Krupiński
 */
public class MonitoringExertionListener implements ExertionListener {
    private static final Logger log = LoggerFactory.getLogger(MonitoringExertionListener.class);
    private MonitoringSession monitorSession;

    public MonitoringExertionListener(MonitoringSession monitorSession) {
        this.monitorSession = monitorSession;

    }

    @Override
    public void exertionStatusChanged(Exertion exertion) throws ContextException {
        try {
            if (exertion.getContext().getExertion()==null)
                exertion.getContext().setExertion(exertion);
            monitorSession.changed(exertion.getContext(), exertion.getStatus());
        } catch (ExertionException e) {
            log.warn("Error while updating monitor", e);
        } catch (RemoteException e) {
            log.warn("Error while updating monitor", e);
        } catch (MonitorException e) {
            log.warn("Error while updating monitor", e);
        }
    }
}
