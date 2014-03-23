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

package sorcer.util;

/**
 * @author Rafał Krupiński
 */
public class InjectionHelper {
    public static Injector getInstance() {
        return instance;
    }

    public static void setInstance(Injector instance) {
        InjectionHelper.instance = instance;
    }

    public static interface Injector {
        public void injectMembers(Object target);

        public <T> T create(Class<T> type);
    }

    private static Injector instance;

    public static void injectMembers(Object target) {
        instance.injectMembers(target);
    }

    public static <T> T create(Class<T> type) {
        return instance.create(type);
    }
}
