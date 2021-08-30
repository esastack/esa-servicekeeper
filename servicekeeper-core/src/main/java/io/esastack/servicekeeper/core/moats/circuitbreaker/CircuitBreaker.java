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
package io.esastack.servicekeeper.core.moats.circuitbreaker;

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;

public interface CircuitBreaker {

    /**
     * Get the name of circuitBreaker.
     *
     * @return name
     */
    String name();

    /**
     * Whether the original invocation is allowed to pass.
     *
     * @return false if the circuitBreaker is not allowed to pass, else true.
     */
    boolean isCallPermitted();

    /**
     * Reset the circuit breaker to its original closed state, losing statistics.
     * <p>
     * Should only be used, when you want to fully reset the circuit breaker without creating a new one.
     */
    void reset();

    /**
     * change to OPEN internal
     */
    void transitionToOpenState();

    /**
     * change to HALF_OPEN internal
     */
    void transitionToHalfOpenState();

    /**
     * change to CLOSED internal
     */
    void transitionToClosedState();

    /**
     * force change to FORCED_DISABLED internal
     */
    void forceToDisabledState();

    /**
     * force change to FORCED_OPEN state
     */
    void forceToForcedOpenState();

    /**
     * Record a success invocation.
     */
    void onSuccess();

    /**
     * Record a failure invocation.
     */
    void onFailure();

    /**
     * Get the internal of current circuitBreaker
     *
     * @return the internal of current circuitBreaker
     */
    State getState();

    /**
     * Get circuit breaker config.
     *
     * @return config
     */
    CircuitBreakerConfig config();

    /**
     * Get immutable configuration
     *
     * @return immutable config
     */
    CircuitBreakerConfig immutableConfig();

    /**
     * Get the collector of current circuitBreaker
     *
     * @return CircuitBreakerMetrics
     */
    CircuitBreakerMetrics metrics();

    enum State {
        /**
         * All invocation is allowed to pass when a circuitBreaker is disabled
         */
        FORCED_DISABLED("FORCED_DISABLED", 3),

        /**
         * HALF_OPEN
         */
        HALF_OPEN("HALF_OPEN", 4),

        /**
         * OPEN
         */
        OPEN("OPEN", 5),

        /**
         * FORCED_OPEN, All invocation is not allowed to pass when a circuitBreaker is forced open
         */
        FORCED_OPEN("FORCED_OPEN", 2),

        /**
         * CLOSED
         */
        CLOSED("CLOSED", 6),

        /**
         * AUTO
         */
        AUTO("AUTO", 1);

        private final String name;

        /**
         * the int value represent the state
         */
        private final int value;

        State(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public int getValue() {
            return value;
        }
    }
}
