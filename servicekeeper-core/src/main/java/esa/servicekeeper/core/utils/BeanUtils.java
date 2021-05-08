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
package esa.servicekeeper.core.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static esa.commons.ObjectUtils.instantiateBeanIfNecessary;
import static esa.commons.reflect.BeanUtils.getFieldValue;
import static esa.commons.reflect.BeanUtils.setFieldValue;

public final class BeanUtils {

    private BeanUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T newAs(T instance) {
        if (instance == null) {
            return null;
        }
        return (T) newAs(instance, instance.getClass());
    }

    public static <T, M> T newAs(M instance, Class<? extends T> clazz) {
        if (instance == null) {
            return null;
        }

        @SuppressWarnings("unchecked") final T newInstance = (T) instantiateBeanIfNecessary(clazz);
        for (Field field : getAllFields(instance.getClass())) {
            setFieldValue(newInstance, field.getName(), getFieldValue(instance, field.getName()));
        }

        return newInstance;
    }

    public static List<Field> getAllFields(Class<?> clazz) {
        final List<Field> fields = new ArrayList<>(32);
        for (; clazz != null && clazz != Object.class; ) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }

        return fields;
    }
}
