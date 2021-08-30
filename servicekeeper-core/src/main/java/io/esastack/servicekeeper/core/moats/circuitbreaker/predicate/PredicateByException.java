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
package io.esastack.servicekeeper.core.moats.circuitbreaker.predicate;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.listener.FondConfigListener;
import io.esastack.servicekeeper.core.utils.ClassCastUtils;

import java.util.Arrays;

public class PredicateByException implements PredicateStrategy, PredicateConfigFilling,
        FondConfigListener<Class<? extends Throwable>[]> {

    private static final ResourceId DEFAULT_NOT_NAMED_ID = ResourceId.from("Not Named");

    /**
     * This name is designed for dynamic configuration. You can change the maxSpendTimeMs by:
     * ${name}.ignoreExceptions: eg: com.servicekeeper.demos.DemoClass.method0.ignoreExceptions=
     * java.lang.RuntimeException
     */
    private final ResourceId name;
    private final Class<? extends Throwable>[] originIgnoreExceptions;

    private volatile Class<? extends Throwable>[] ignoreExceptions;

    public PredicateByException() {
        this(DEFAULT_NOT_NAMED_ID);
    }

    public PredicateByException(ResourceId name) {
        this(ClassCastUtils.cast(new Class[0]), ClassCastUtils.cast(new Class[0]), name);
    }

    public PredicateByException(Class<? extends Throwable>[] ignoreExceptions,
                                Class<? extends Throwable>[] originIgnoreExceptions, ResourceId name) {
        this.ignoreExceptions = ignoreExceptions;
        this.originIgnoreExceptions = originIgnoreExceptions;
        this.name = name;
    }

    @Override
    public boolean isSuccess(Context ctx) {
        Throwable bizException = ctx.getBizException();
        if (bizException == null) {
            return true;
        }
        if (ignoreExceptions == null || ignoreExceptions.length == 0) {
            return false;
        }

        for (Class<? extends Throwable> clazz : ignoreExceptions) {
            if (isInstanceOf(bizException, clazz)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Class<? extends Throwable>[] getFond(ExternalConfig config) {
        if (config == null || config.getIgnoreExceptions() == null) {
            return ClassCastUtils.cast(new Class[0]);
        } else {
            return ClassCastUtils.cast(config.getIgnoreExceptions());
        }
    }

    @Override
    public void updateWithNewestConfig(Class<? extends Throwable>[] newestConfig) {
        ignoreExceptions = newestConfig;
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        ignoreExceptions = originIgnoreExceptions;
    }

    @Override
    public boolean isConfigEquals(Class<? extends Throwable>[] newestConfig) {
        return Arrays.equals(ignoreExceptions, newestConfig);
    }

    @Override
    public ResourceId listeningKey() {
        return name;
    }

    @Override
    public void fill(CircuitBreakerConfig config) {
        config.updateIgnoreExceptions(ignoreExceptions);
    }

    private boolean isInstanceOf(final Throwable bizException, Class<? extends Throwable> clazz) {
        return clazz.isAssignableFrom(bizException.getClass());
    }
}
