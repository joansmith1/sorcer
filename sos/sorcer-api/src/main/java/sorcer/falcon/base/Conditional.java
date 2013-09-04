/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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
package sorcer.falcon.base;

import sorcer.service.ContextException;

public interface Conditional {

    /**
     * The isTrue method is responsible for evaluating the condition component of
     * the Conditonal. Thus returning the boolean value true or false.
     *
     * @return boolean true or false depending on the condition
     * @throws ExertionException
     *             if there is any problem within the isTrue method.
     * @throws ContextException
     */
    public boolean isTrue() throws ContextException;

}