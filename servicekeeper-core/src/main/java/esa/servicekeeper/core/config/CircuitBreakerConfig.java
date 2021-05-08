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
package esa.servicekeeper.core.config;

import esa.commons.Checks;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.utils.DurationUtils;
import esa.servicekeeper.core.utils.ParamCheckUtils;

import java.io.Serializable;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.AUTO;
import static esa.servicekeeper.core.utils.ClassCastUtils.cast;

public class CircuitBreakerConfig implements Serializable {

    private static final long serialVersionUID = 4474669140305366603L;

    private final float failureRateThreshold;
    private final int ringBufferSizeInHalfOpenState;
    private final int ringBufferSizeInClosedState;
    private final CircuitBreaker.State state;
    private final Duration waitDurationInOpenState;

    private final Class<? extends PredicateStrategy> predicateStrategy;
    private Class<? extends Throwable>[] ignoreExceptions;
    private long maxSpendTimeMs;

    private CircuitBreakerConfig(float failureRateThreshold,
                                 int ringBufferSizeInHalfOpenState,
                                 int ringBufferSizeInClosedState, long maxSpendTimeMs,
                                 Class<? extends Throwable>[] ignoreExceptions,
                                 Duration waitDurationInOpenState,
                                 Class<? extends PredicateStrategy> predicateStrategy,
                                 CircuitBreaker.State state) {
        this.failureRateThreshold = failureRateThreshold;
        this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        this.maxSpendTimeMs = maxSpendTimeMs;
        this.ignoreExceptions = ignoreExceptions;
        this.waitDurationInOpenState = waitDurationInOpenState;
        this.predicateStrategy = predicateStrategy;
        this.state = state;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CircuitBreakerConfig ofDefault() {
        return builder().build();
    }

    public static Builder from(CircuitBreakerConfig config) {
        Checks.checkNotNull(config, "The CircuitBreakerConfig to copy from must not be null");
        return new Builder()
                .failureRateThreshold(config.getFailureRateThreshold())
                .ringBufferSizeInHalfOpenState(config.getRingBufferSizeInHalfOpenState())
                .ringBufferSizeInClosedState(config.getRingBufferSizeInClosedState())
                .waitDurationInOpenState(config.getWaitDurationInOpenState())
                .maxSpendTimeMs(config.getMaxSpendTimeMs())
                .ignoreExceptions(config.getIgnoreExceptions())
                .predicateStrategy(config.getPredicateStrategy())
                .state(config.getState());
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

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public Class<? extends PredicateStrategy> getPredicateStrategy() {
        return predicateStrategy;
    }

    public long getMaxSpendTimeMs() {
        return maxSpendTimeMs;
    }

    public void updateMaxSpendTimeMs(long maxSpendTimeMs) {
        this.maxSpendTimeMs = maxSpendTimeMs;
    }

    public void updateIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    public Class<? extends Throwable>[] getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public CircuitBreaker.State getState() {
        return state;
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfig{" + "failureRateThreshold=" + failureRateThreshold +
                ", ringBufferSizeInHalfOpenState=" + ringBufferSizeInHalfOpenState +
                ", ringBufferSizeInClosedState=" + ringBufferSizeInClosedState +
                ", maxSpendTimeMs=" + maxSpendTimeMs +
                ", ignoreExceptions=" + Arrays.toString(ignoreExceptions) +
                ", waitDurationInOpenState=" + DurationUtils.toString(waitDurationInOpenState) +
                ", predicateStrategy=" + predicateStrategy +
                ", state=" + state.toString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CircuitBreakerConfig that = (CircuitBreakerConfig) o;
        return Float.compare(that.failureRateThreshold, failureRateThreshold) == 0 &&
                ringBufferSizeInHalfOpenState == that.ringBufferSizeInHalfOpenState &&
                ringBufferSizeInClosedState == that.ringBufferSizeInClosedState &&
                state == that.state &&
                Objects.equals(waitDurationInOpenState, that.waitDurationInOpenState);
    }

    public static class Builder {
        private float failureRateThreshold = 50.0f;
        private int ringBufferSizeInHalfOpenState = 10;
        private int ringBufferSizeInClosedState = 100;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60L);
        private long maxSpendTimeMs = -1;
        private Class<? extends Throwable>[] ignoreExceptions = cast(new Class[0]);
        private Class<? extends PredicateStrategy> predicateStrategy = PredicateByException.class;
        private CircuitBreaker.State state = AUTO;

        private Builder() {
        }

        public Builder failureRateThreshold(float failureRateThreshold) {
            ParamCheckUtils.legalFailureThreshold(failureRateThreshold, "illegal failureRateThreshold: "
                    + failureRateThreshold + " excepted([0, 100])");
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        public Builder ringBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
            return this;
        }

        public Builder ringBufferSizeInClosedState(int ringBufferSizeInClosedState) {
            this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
            return this;
        }

        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }

        public Builder predicateStrategy(Class<? extends PredicateStrategy> predicateStrategy) {
            this.predicateStrategy = predicateStrategy;
            return this;
        }

        public Builder maxSpendTimeMs(long maxSpendTimeMs) {
            this.maxSpendTimeMs = maxSpendTimeMs;
            return this;
        }

        public Builder ignoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
            this.ignoreExceptions = ignoreExceptions;
            return this;
        }

        public Builder state(CircuitBreaker.State state) {
            this.state = state;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(failureRateThreshold, ringBufferSizeInHalfOpenState,
                    ringBufferSizeInClosedState, maxSpendTimeMs, ignoreExceptions, waitDurationInOpenState,
                    predicateStrategy, state);
        }
    }
}
