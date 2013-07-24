package sorcer.boot.load;
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


import sorcer.core.ServiceActivator;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Find, instantiate and call ServiceActivator implementation in jars
 *
 * @author Rafał Krupiński
 */
public class Activator {

    public void activate(URL[] jars) throws Exception {
        for (URL jar : jars) {
            activate(jar);
        }
    }

    public void activate(URL jarUrl) throws Exception {
        JarFile jar;
        try {
            jar = new JarFile(jarUrl.getFile());

            Attributes mainAttributes = jar.getManifest().getMainAttributes();
            if (mainAttributes.containsKey(ServiceActivator.KEY_ACTIVATOR)) {
                String activatorClassName = (String) mainAttributes.get(ServiceActivator.KEY_ACTIVATOR);
                Class<?> activatorClass = Class.forName(activatorClassName);
                if (!ServiceActivator.class.isAssignableFrom(activatorClass)) {
                    throw new IllegalArgumentException("Activator class " + activatorClassName + " must implement ServiceActivator");
                }
                if (activatorClass.isInterface() || Modifier.isAbstract(activatorClass.getModifiers())) {
                    throw new IllegalArgumentException("Activator class " + activatorClassName + " must be concrete");
                }
                ServiceActivator activator = (ServiceActivator) activatorClass.newInstance();
                activator.activate();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not open jar file " + jarUrl, e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not find Sorcer-Activator class from " + jarUrl, e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not instantiate Sorcer-Activator class from " + jarUrl, e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Could not instantiate Sorcer-Activator class from " + jarUrl, e);
        }
    }
}