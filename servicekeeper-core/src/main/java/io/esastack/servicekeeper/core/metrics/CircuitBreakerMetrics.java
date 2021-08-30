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
package io.esastack.servicekeeper.core.metrics;

import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

/**
 * The class is directly copied from Resilience4j(https://github.com/resilience4j/resilience4j).
 */
public interface CircuitBreakerMetrics extends Metrics {

    /**
     * Returns the failure rate in percentage. If the number of measured calls is below the minimum number
     * of measured calls, it returns -1.
     *
     * @return the failure rate in percentage
     */
    float failureRateThreshold();

    /**
     * Returns the current number of buffered calls.
     *
     * @return he current number of buffered calls
     */
    int numberOfBufferedCalls();

    /**
     * Returns the current number of failed calls.
     *
     * @return the current number of failed calls
     */
    int numberOfFailedCalls();

    /**
     * Returns the current number of not permitted calls, when the internal is OPEN.
     * <p>
     * The number of denied calls is always 0, when the CircuitBreaker internal is CLOSED or HALF_OPEN.
     * The number of denied calls is only increased when the CircuitBreaker internal is OPEN.
     *
     * @return the current number of not permitted calls
     */
    long numberOfNotPermittedCalls();

    /**
     * Returns the maximum number of buffered calls.
     *
     * @return the maximum number of buffered calls
     */
    int maxNumberOfBufferedCalls();

    /**
     * Returns the current number of successful calls.
     *
     * @return the current number of successful calls
     */
    int numberOfSuccessfulCalls();

    /**
     * Get the state of the circuitBreaker
     *
     * @return the current state of the circuitBreaker.
     */
    CircuitBreaker.State state();

    /**
     * Get the type of current collector.
     *
     * @return type
     */
    @Override
    default Type type() {
        return Type.CIRCUIT_BREAKER;
    }
}
