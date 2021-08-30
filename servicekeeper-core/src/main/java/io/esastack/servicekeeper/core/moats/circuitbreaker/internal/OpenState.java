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

import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

import java.time.Duration;
import java.time.Instant;

final class OpenState extends CircuitBreakerState {
    private final Instant retryAfterWaitDuration;
    private final Metrics metrics;

    OpenState(CircuitBreakerStateMachine stateMachine, Metrics metrics) {
        super(stateMachine);
        final Duration waitDurationInOpenState = stateMachine.config().getWaitDurationInOpenState();
        this.retryAfterWaitDuration = Instant.now().plus(waitDurationInOpenState);
        this.metrics = metrics;
    }

    /**
     * Returns false, if the wait duration has not elapsed.
     * Returns true, if the wait duration has elapsed and transitions the internal machine to HALF_OPEN internal.
     *
     * @return false, if the wait duration has not elapsed. true, if the wait duration has elapsed.
     */
    @Override
    boolean isCallPermitted() {
        // Thread-safe
        if (Instant.now().isAfter(retryAfterWaitDuration)) {
            stateMachine.transitionToHalfOpenState();
            return true;
        }
        metrics.onCallNotPermitted();
        return false;
    }

    @Override
    void onSuccess() {
        // Could be called when Thread 1 invokes isCallPermitted when the internal is CLOSED, but in the meantime
        // another Thread 2 calls onError and the internal changes from CLOSED to OPEN before Thread 1 calls onSuccess.
        // But the onSuccess event should still be recorded, even if it happened after the internal transition.
        metrics.onSuccess();
    }

    @Override
    void onFailure() {
        // Could be called when Thread 1 invokes isCallPermitted when the internal is CLOSED, but in the meantime
        // another Thread 2 calls onError and the internal changes from CLOSED to OPEN before Thread 1 calls onError.
        // But the onError event should still be recorded, even if it happened after the internal transition.
        metrics.onError();
    }

    @Override
    Metrics getMetrics() {
        return metrics;
    }

    @Override
    CircuitBreaker.State getState() {
        return CircuitBreaker.State.OPEN;
    }
}
