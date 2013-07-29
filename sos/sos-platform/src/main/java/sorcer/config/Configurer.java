package sorcer.config;
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


import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Rafał Krupiński
 */
public class Configurer {
    final private static Logger log = LoggerFactory.getLogger(Configurer.class);

    public void process(Object object, Configuration config) {
        Class<?> targetClass = object.getClass();
        Configurable configurable = targetClass.getAnnotation(Configurable.class);
        if (configurable == null) return;

        String component = configurable.component();

        for (Field field : targetClass.getDeclaredFields()) {
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            if (configEntry != null) {
                updateField(object, field, config, component, configEntry);
            }
        }

        for (Method method : targetClass.getDeclaredMethods()) {
            ConfigEntry configEntry = method.getAnnotation(ConfigEntry.class);
            if (configEntry != null) {
                updateProperty(object, method, config, component, configEntry);
            }
        }
    }

    private void updateProperty(Object object, Method method, Configuration config, String component, ConfigEntry configEntry) {
        Class<?>[] ptypes = method.getParameterTypes();
        if (ptypes.length != 1)return;
        Class type = ptypes[0];

        Object defaultValue = null;
        if (!ConfigEntry.NONE.equals(configEntry.defaultValue())) {
            defaultValue = configEntry.defaultValue();
        }
        Object value;
        try {
            value = config.getEntry(component, getEntryKey(getPropertyName(method), configEntry),type,defaultValue);
        } catch (ConfigurationException e) {
            return;
        }

        if (!ptypes[0].isAssignableFrom(type)){
            return;
        }
            try {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
            } catch (SecurityException ignored) {
                log.warn("Could not set value of {} because of access restriction", method);
                return;
            }

        try {
            method.invoke(object, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String getPropertyName(Method m) {
        String name = m.getName();
        if (name.length() > 3 && name.startsWith("set") && Character.isUpperCase(name.charAt(4))) {
            return "" + Character.toLowerCase(name.charAt(4)) + name.substring(5);
        }
        return name;
    }

    private void updateField(Object target, Field field, Configuration config, String component, ConfigEntry configEntry) {
        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
        } catch (SecurityException x) {
            log.warn("Could not set value of {} because of access restriction", field);
            return;
        }

        Object defaultValue = null;
        if (!ConfigEntry.NONE.equals(configEntry.defaultValue()) && field.getType().isAssignableFrom(String.class)) {
            defaultValue = configEntry.defaultValue();
        }
        try {
            Object value = config.getEntry(component, getEntryKey(component, configEntry), field.getType(), defaultValue);
            field.set(target, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String getEntryKey(String propertyName, ConfigEntry entry) {
        String key;
        if (ConfigEntry.DEFAULT_KEY.equals(entry.key())) {
            return propertyName;
        } else {
            return entry.key();
        }
    }
}
