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

package sorcer.co.tuple;

import java.io.Serializable;


public class ExecPath extends Path<Object> implements Serializable {

    public ExecPath() { }

    public ExecPath(String path) {
        this._1 = path;
    }

    public ExecPath(String path, Object executor) {
        this._1 = path;
        this._2 = executor;
    }

    public String path() {
        return _1;
    }
}