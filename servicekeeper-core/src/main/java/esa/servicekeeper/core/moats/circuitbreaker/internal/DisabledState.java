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

import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

final class DisabledState extends CircuitBreakerState {

    private final Metrics metrics;

    DisabledState(CircuitBreakerStateMachine stateMachine) {
        super(stateMachine);
        this.metrics = new Metrics(stateMachine.config()
                .getRingBufferSizeInClosedState());
    }

    /**
     * Returns always true, because the CircuitBreaker is disabled.
     *
     * @return always true, because the CircuitBreaker is disabled.
     */
    @Override
    boolean isCallPermitted() {
        return true;
    }

    @Override
    void onSuccess() {
        // Do nothing
    }

    @Override
    void onFailure() {
        // Do nothing
    }

    @Override
    Metrics getMetrics() {
        return metrics;
    }

    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.FORCED_DISABLED;
    }
}
