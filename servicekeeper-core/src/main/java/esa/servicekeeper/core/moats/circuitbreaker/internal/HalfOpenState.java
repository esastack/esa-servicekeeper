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
package esa.servicekeeper.core.moats.circuitbreaker.internal;

import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

final class HalfOpenState extends CircuitBreakerState {

    private final float failureRateThreshold;
    private final Metrics metrics;

    HalfOpenState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        CircuitBreakerConfig circuitBreakerConfig = stateMachine.config();
        this.metrics = new Metrics(
                circuitBreakerConfig.getRingBufferSizeInHalfOpenState());
        this.failureRateThreshold = circuitBreakerConfig.getFailureRateThreshold();
    }

    /**
     * Returns always true, because the CircuitBreaker is half open.
     *
     * @return always true, because the CircuitBreaker is half open.
     */
    @Override
    boolean isCallPermitted() {
        return true;
    }

    @Override
    void onSuccess() {
        checkFailureRate(metrics.onSuccess());
    }

    @Override
    void onFailure() {
        checkFailureRate(metrics.onError());
    }

    @Override
    Metrics getMetrics() {
        return metrics;
    }

    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.HALF_OPEN;
    }

    /**
     * Checks if the current failure rate is above or below the threshold.
     * If the failure rate is above the threshold, transition the internal machine to OPEN internal.
     * If the failure rate is below the threshold, transition the internal machine to CLOSED internal.
     *
     * @param currentFailureRate the current failure rate
     */
    private void checkFailureRate(float currentFailureRate) {
        if (currentFailureRate != -1) {
            if (currentFailureRate >= failureRateThreshold) {
                stateMachine.transitionToOpenState();
            } else {
                stateMachine.transitionToClosedState();
            }
        }
    }
}
