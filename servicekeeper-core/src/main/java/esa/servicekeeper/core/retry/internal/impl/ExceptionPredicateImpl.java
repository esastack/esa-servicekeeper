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
package esa.servicekeeper.core.retry.internal.impl;

import esa.commons.Checks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExceptionPredicateImpl extends ExceptionPredicate {

    final ConcurrentMap<Class<? extends Throwable>, Boolean> exceptions = new ConcurrentHashMap<>(8);

    final Boolean defaultValue;

    ExceptionPredicateImpl(int maxAttempts,
                           Map<Class<? extends Throwable>, Boolean> exceptions,
                           Boolean defaultValue) {
        super(maxAttempts);
        if (exceptions != null) {
            this.exceptions.putAll(exceptions);
        }
        Checks.checkNotNull(defaultValue, "defaultValue");
        this.defaultValue = defaultValue;
    }

    @Override
    protected boolean canRetry0(Throwable th) {
        if (th == null) {
            return defaultValue;
        }

        final Class<? extends Throwable> exClazz = th.getClass();
        if (exceptions.containsKey(exClazz)) {
            return exceptions.get(exClazz);
        }

        // Check from super classes
        Boolean value = null;
        for (Class<?> cls = exClazz; !cls.equals(Object.class) && value == null; cls = cls.getSuperclass()) {
            value = exceptions.get(cls);
        }

        // ConcurrentMap doesn't allow null value
        if (value != null) {
            this.exceptions.put(exClazz, value);
        }

        return value == null ? defaultValue : value;
    }

}
