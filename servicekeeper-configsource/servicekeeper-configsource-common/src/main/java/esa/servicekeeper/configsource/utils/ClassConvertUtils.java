/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package esa.servicekeeper.configsource.utils;

import esa.commons.StringUtils;
import esa.servicekeeper.configsource.constant.Constants;
import esa.servicekeeper.core.utils.ClassCastUtils;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.List;

public final class ClassConvertUtils {

    private static final Logger logger = LogUtils.logger();

    private ClassConvertUtils() {
    }

    /**
     * Get class by class's qualified name.
     * eg: [java.lang.Exception] to Exception.class
     * [java.lang.Exception, java.lang.IllegalStateException, java.lang.RuntimeException] to
     * Class{Exception.class, IllegalStateException, RuntimeException}
     *
     * @param names qualified names
     * @return Class[]
     */
    public static Class<?>[] toClasses(final String names) {
        String value = StringUtils.trim(names);
        if (value == null || value.isEmpty()) {
            return new Class[0];
        }
        // Single
        if (!(value.startsWith(Constants.ARRAY_FORMAT[0]) && value.endsWith(Constants.ARRAY_FORMAT[1]))) {
            try {
                if (StringUtils.isEmpty(value)) {
                    return new Class[0];
                }
                return new Class[]{Class.forName(value)};
            } catch (ClassNotFoundException e) {
                logger.error("Failed to load class by name: " + value, e);
            }
            return new Class[0];
        }
        // Array
        List<Class<?>> classes = new LinkedList<>();
        for (String className : value.substring(1, value.length() - 1).split(Constants.COMMA)) {
            try {
                if (StringUtils.isEmpty(className)) {
                    continue;
                }
                classes.add(ClassCastUtils.cast(Class.forName(className.trim())));
            } catch (ClassNotFoundException e) {
                logger.error("Failed to load class by name: " + value, e);
            }
        }
        return classes.toArray(new Class[0]);
    }

    /**
     * Get class by class's qualified name
     *
     * @param origin origin
     * @return class
     */
    public static Class<?> toSingleClass(final String origin) {
        Class<?>[] classes = toClasses(origin);
        if (classes.length == 0) {
            logger.warn("Failed to convert origin string to class, origin: {}", origin);
            return null;
        }

        return classes[0];
    }

}
