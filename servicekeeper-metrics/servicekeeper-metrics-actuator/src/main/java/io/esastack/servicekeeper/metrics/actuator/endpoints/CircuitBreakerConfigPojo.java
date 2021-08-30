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
package io.esastack.servicekeeper.metrics.actuator.endpoints;

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import io.esastack.servicekeeper.core.utils.DurationUtils;

class CircuitBreakerConfigPojo {

    private final float failureRateThreshold;
    private final int ringBufferSizeInHalfOpenState;
    private final int ringBufferSizeInClosedState;
    private final long maxSpendTimeMs;
    private final Class<? extends Throwable>[] ignoreExceptions;
    private final String waitDurationInOpenState;
    private final Class<? extends PredicateStrategy> predicateStrategy;
    private final String state;

    private CircuitBreakerConfigPojo(float failureRateThreshold, int ringBufferSizeInHalfOpenState,
                                     int ringBufferSizeInClosedState, long maxSpendTimeMs,
                                     Class<? extends Throwable>[] ignoreExceptions, String waitDurationInOpenState,
                                     Class<? extends PredicateStrategy> predicateStrategy,
                                     CircuitBreaker.State state) {
        this.failureRateThreshold = failureRateThreshold;
        this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        this.maxSpendTimeMs = maxSpendTimeMs;
        this.ignoreExceptions = ignoreExceptions;
        this.waitDurationInOpenState = waitDurationInOpenState;
        this.predicateStrategy = predicateStrategy;
        this.state = state.name();
    }

    static CircuitBreakerConfigPojo from(CircuitBreakerConfig config) {
        return new CircuitBreakerConfigPojo(config.getFailureRateThreshold(),
                config.getRingBufferSizeInHalfOpenState(), config.getRingBufferSizeInClosedState(),
                config.getMaxSpendTimeMs(), config.getIgnoreExceptions(),
                DurationUtils.toString(config.getWaitDurationInOpenState()),
                config.getPredicateStrategy(),
                config.getState());
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public int getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    public int getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public long getMaxSpendTimeMs() {
        return maxSpendTimeMs;
    }

    public Class<? extends Throwable>[] getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public String getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public Class<? extends PredicateStrategy> getPredicateStrategy() {
        return predicateStrategy;
    }

    public String getState() {
        return state;
    }
}
