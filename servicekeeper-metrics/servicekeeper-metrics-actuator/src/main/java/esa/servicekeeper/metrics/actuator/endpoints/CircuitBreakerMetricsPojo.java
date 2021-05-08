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
package esa.servicekeeper.metrics.actuator.endpoints;

import esa.servicekeeper.core.metrics.CircuitBreakerMetrics;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

class CircuitBreakerMetricsPojo {

    private final float failureThreshold;
    private final int maxNumberOfBufferedCalls;
    private final int numberOfBufferedCalls;
    private final int numberOfSuccessfulCalls;
    private final int numberOfFailedCalls;
    private final long numberOfNotPermittedCalls;
    private final String state;

    private CircuitBreakerMetricsPojo(float failureThreshold, int maxNumberOfBufferedCalls,
                                      int numberOfBufferedCalls, int numberOfSuccessfulCalls,
                                      int numberOfFailedCalls, long numberOfNotPermittedCalls,
                                      CircuitBreaker.State state) {
        this.failureThreshold = failureThreshold;
        this.maxNumberOfBufferedCalls = maxNumberOfBufferedCalls;
        this.numberOfBufferedCalls = numberOfBufferedCalls;
        this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
        this.numberOfFailedCalls = numberOfFailedCalls;
        this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
        this.state = state.name();
    }

    static CircuitBreakerMetricsPojo from(CircuitBreakerMetrics metrics) {
        return new CircuitBreakerMetricsPojo(metrics.failureRateThreshold(), metrics.maxNumberOfBufferedCalls(),
                metrics.numberOfBufferedCalls(), metrics.numberOfSuccessfulCalls(),
                metrics.numberOfFailedCalls(), metrics.numberOfNotPermittedCalls(),
                metrics.state());

    }

    public float getFailureThreshold() {
        return failureThreshold;
    }

    public int getMaxNumberOfBufferedCalls() {
        return maxNumberOfBufferedCalls;
    }

    public int getNumberOfBufferedCalls() {
        return numberOfBufferedCalls;
    }

    public int getNumberOfSuccessfulCalls() {
        return numberOfSuccessfulCalls;
    }

    public int getNumberOfFailedCalls() {
        return numberOfFailedCalls;
    }

    public long getNumberOfNotPermittedCalls() {
        return numberOfNotPermittedCalls;
    }

    public String getState() {
        return state;
    }
}
