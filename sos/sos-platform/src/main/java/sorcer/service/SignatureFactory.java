package sorcer.service;
/**
 *
 * Copyright 2013 Rafał Krupiński.
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


import sorcer.core.signature.NetSignature;
import sorcer.core.signature.ObjectSignature;

/**
 * Methods for creating Signature objects; extracted from sorcer.eo.operator.
 * Use it instead of operator in sorcer code.
 *
 * @author Rafał Krupiński
 */
public class SignatureFactory {
    public static Signature sig(String operation, Class<?> serviceType,
                                String providerName, Object... parameters)
            throws SignatureException {
        Signature sig;
        if (serviceType.isInterface()) {
            sig = new NetSignature(operation, serviceType, providerName);
        } else {
            sig = new ObjectSignature(operation, serviceType);
        }
        // default Operation type = SERVICE
        sig.setType(Signature.Type.SRV);
        if (parameters.length > 0) {
            for (Object o : parameters) {
                if (o instanceof Signature.Type) {
                    sig.setType((Signature.Type) o);
                } else if (o instanceof ReturnPath) {
                    sig.setReturnPath((ReturnPath) o);
                }
            }
        }
        return sig;
    }

    public static Signature sig(String operation, Class serviceType) throws SignatureException {
        return sig(operation, serviceType, null, Signature.Type.SRV);
    }

}
