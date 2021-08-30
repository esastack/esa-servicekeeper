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
package io.esastack.servicekeeper.core.moats.circuitbreaker.internal;

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

final class ClosedState extends CircuitBreakerState {

    private final Metrics metrics;
    private final float failureRateThreshold;

    ClosedState(CircuitBreakerStateMachine stateMachine) {
        this(stateMachine, null);
    }

    ClosedState(CircuitBreakerStateMachine stateMachine, Metrics metrics) {
        super(stateMachine);
        CircuitBreakerConfig config = stateMachine.config();
        if (metrics == null) {
            this.metrics = new Metrics(config.getRingBufferSizeInClosedState());
        } else {
            this.metrics = metrics.copy(config.getRingBufferSizeInClosedState());
        }
        failureRateThreshold = config.getFailureRateThreshold();
    }

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
        return CircuitBreaker.State.CLOSED;
    }

    /**
     * Checks if the current failure rate is above the threshold.
     * If the failure rate is above the threshold, transitions the state machine to OPEN state.
     *
     * @param currentFailureRate the current failure rate
     */
    private void checkFailureRate(float currentFailureRate) {
        if (currentFailureRate >= failureRateThreshold) {
            // Transition the state machine to OPEN state, because the failure rate is above the threshold.
            stateMachine.transitionToOpenState();
        }
    }
}
