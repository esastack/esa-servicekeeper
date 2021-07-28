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
package esa.servicekeeper.core.factory;

import esa.commons.Checks;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

import java.util.Arrays;
import java.util.Objects;

public class PredicateStrategyConfig {

    private final ResourceId name;
    private final Class<? extends PredicateStrategy> predicate;
    private final Class<? extends Throwable>[] ignoreExceptions;
    private final long maxSpendTimeMs;
    private final long originalMaxSpendTimeMs;
    private final Class<? extends Throwable>[] originIgnoreExceptions;

    private PredicateStrategyConfig(ResourceId name,
                                    Class<? extends PredicateStrategy> predicate,
                                    Class<? extends Throwable>[] ignoreExceptions,
                                    long maxSpendTimeMs, long originalMaxSpendTimeMs,
                                    Class<? extends Throwable>[] originIgnoreExceptions) {
        this.name = name;
        this.predicate = predicate;
        this.ignoreExceptions = ignoreExceptions;
        this.maxSpendTimeMs = maxSpendTimeMs;
        this.originalMaxSpendTimeMs = originalMaxSpendTimeMs;
        this.originIgnoreExceptions = originIgnoreExceptions;
    }

    public static PredicateStrategyConfig from(ResourceId resourceId, CircuitBreakerConfig circuitBreakerConfig,
                                               CircuitBreakerConfig immutableConfig) {
        Checks.checkNotNull(resourceId);
        Checks.checkNotNull(circuitBreakerConfig);

        final long originalMaxSpendTimeMs = immutableConfig == null ? -1L : immutableConfig.getMaxSpendTimeMs();
        @SuppressWarnings("unchecked") final Class<? extends Throwable>[] originIgnoreExceptions =
                immutableConfig == null ? new Class[0] : immutableConfig.getIgnoreExceptions();
        return new PredicateStrategyConfig(resourceId, circuitBreakerConfig.getPredicateStrategy(),
                circuitBreakerConfig.getIgnoreExceptions(), circuitBreakerConfig.getMaxSpendTimeMs(),
                originalMaxSpendTimeMs, originIgnoreExceptions);
    }

    public ResourceId getName() {
        return name;
    }

    public Class<? extends PredicateStrategy> getPredicate() {
        return predicate;
    }

    public Class<? extends Throwable>[] getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public long getMaxSpendTimeMs() {
        return maxSpendTimeMs;
    }

    public long getOriginalMaxSpendTimeMs() {
        return originalMaxSpendTimeMs;
    }

    public Class<? extends Throwable>[] getOriginIgnoreExceptions() {
        return originIgnoreExceptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PredicateStrategyConfig that = (PredicateStrategyConfig) o;
        return maxSpendTimeMs == that.maxSpendTimeMs &&
                originalMaxSpendTimeMs == that.originalMaxSpendTimeMs &&
                Objects.equals(name, that.name) &&
                Objects.equals(predicate, that.predicate) &&
                Arrays.equals(ignoreExceptions, that.ignoreExceptions) &&
                Arrays.equals(originIgnoreExceptions, that.originIgnoreExceptions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, predicate, maxSpendTimeMs, originalMaxSpendTimeMs);
        result = 31 * result + Arrays.hashCode(ignoreExceptions);
        result = 31 * result + Arrays.hashCode(originIgnoreExceptions);
        return result;
    }

    public enum Type {

        /**
         * Type of exception only
         */
        EXCEPTION(1),

        /**
         * Type of spend time only
         */
        SPEND_TIME(2),

        /**
         * Type of exception and type
         */
        EXCEPTION_SPEND_TIME(3),

        /**
         * Type of custom class
         */
        CUSTOM(4);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        /**
         * Get state by code
         *
         * @param code code
         * @return state
         */
        public static PredicateStrategyConfig.Type getTypeByCode(int code) {
            for (PredicateStrategyConfig.Type type : values()) {
                if (code == type.code) {
                    return type;
                }
            }
            return null;
        }
    }

}
