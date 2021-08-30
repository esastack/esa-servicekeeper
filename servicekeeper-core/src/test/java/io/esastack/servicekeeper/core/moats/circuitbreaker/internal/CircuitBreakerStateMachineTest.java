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
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class CircuitBreakerStateMachineTest {

    private final float failureRateThreshold = RandomUtils.randomFloat(10);
    private final int ringBufferSizeInClosedState = RandomUtils.randomInt(20);
    private final int ringBufferSizeInHalfOpenState = RandomUtils.randomInt(30);
    private final Duration waitDurationInOpenState = Duration.ofMillis(10L);

    private final CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.builder()
            .failureRateThreshold(failureRateThreshold)
            .ringBufferSizeInClosedState(ringBufferSizeInClosedState)
            .ringBufferSizeInHalfOpenState(ringBufferSizeInHalfOpenState)
            .waitDurationInOpenState(waitDurationInOpenState)
            .build();

    private CircuitBreakerStateMachine stateMachine;
    private CircuitBreakerMetrics metrics;

    @BeforeEach
    void setUp() {
        String name = "CircuitBreakerStateMachine-Test";
        stateMachine = new CircuitBreakerStateMachine(name, breakerConfig, null, null);
        metrics = stateMachine.metrics();
    }

    @Test
    void testTransitionToOpenState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        stateMachine.transitionToOpenState();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            then(stateMachine.isCallPermitted()).isFalse();
        }
        then(metrics.numberOfNotPermittedCalls()).isEqualTo(ringBufferSizeInClosedState);
        then(metrics.numberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    void testTransitionToHalfOpenState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        stateMachine.transitionToHalfOpenState();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        for (int i = 0; i < ringBufferSizeInHalfOpenState; i++) {
            then(stateMachine.isCallPermitted()).isTrue();
            BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
            stateMachine.onSuccess();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testForceToDisabledState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        stateMachine.forceToDisabledState();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.FORCED_DISABLED);
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.FORCED_DISABLED);
            then(stateMachine.isCallPermitted()).isTrue();
            stateMachine.onFailure();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.FORCED_DISABLED);
    }

    @Test
    void testTransitionFromHalfOpenToClosedState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);
            then(metrics.numberOfBufferedCalls()).isEqualTo(i);
            stateMachine.onFailure();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(metrics.failureRateThreshold()).isEqualTo(100f);
        then(metrics.numberOfFailedCalls()).isEqualTo(ringBufferSizeInClosedState);
        then(metrics.numberOfSuccessfulCalls()).isEqualTo(0);
        then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);
        then(metrics.numberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);

        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + waitDurationInOpenState.toMillis());
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(stateMachine.isCallPermitted()).isTrue();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        metrics = stateMachine.metrics();
        for (int i = 0; i < ringBufferSizeInHalfOpenState; i++) {
            then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInHalfOpenState);
            then(metrics.numberOfBufferedCalls()).isEqualTo(i);
            then(stateMachine.isCallPermitted()).isTrue();
            stateMachine.onSuccess();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        then(stateMachine.isCallPermitted()).isTrue();
    }

    @Test
    void testTransitionFromHalfOpenToOpenState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);
            then(metrics.numberOfBufferedCalls()).isEqualTo(i);
            stateMachine.onFailure();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(metrics.failureRateThreshold()).isEqualTo(100f);
        then(metrics.numberOfFailedCalls()).isEqualTo(ringBufferSizeInClosedState);
        then(metrics.numberOfSuccessfulCalls()).isEqualTo(0);
        then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);
        then(metrics.numberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);

        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + waitDurationInOpenState.toMillis());
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(stateMachine.isCallPermitted()).isTrue();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        metrics = stateMachine.metrics();
        for (int i = 0; i < ringBufferSizeInHalfOpenState; i++) {
            then(metrics.maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInHalfOpenState);
            then(metrics.numberOfBufferedCalls()).isEqualTo(i);
            then(stateMachine.isCallPermitted()).isTrue();
            stateMachine.onFailure();
        }
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        then(stateMachine.isCallPermitted()).isFalse();
    }

    @Test
    void testReset() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            then(stateMachine.isCallPermitted()).isTrue();
            stateMachine.onFailure();
            BDDAssertions.then(stateMachine.metrics().numberOfNotPermittedCalls()).isEqualTo(0);
            BDDAssertions.then(stateMachine.metrics().numberOfSuccessfulCalls()).isEqualTo(0);
            BDDAssertions.then(stateMachine.metrics().maxNumberOfBufferedCalls())
                    .isEqualTo(ringBufferSizeInClosedState);
            BDDAssertions.then(stateMachine.metrics().numberOfBufferedCalls()).isEqualTo(i + 1);
            BDDAssertions.then(stateMachine.metrics().numberOfFailedCalls()).isEqualTo(i + 1);
        }
        BDDAssertions.then(stateMachine.metrics().failureRateThreshold()).isEqualTo(100.0f);
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        BDDAssertions.then(stateMachine.metrics().maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);

        // Reset
        stateMachine.reset();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        BDDAssertions.then(stateMachine.metrics().maxNumberOfBufferedCalls()).isEqualTo(ringBufferSizeInClosedState);
        BDDAssertions.then(stateMachine.metrics().numberOfBufferedCalls()).isEqualTo(0);
        BDDAssertions.then(stateMachine.metrics().numberOfFailedCalls()).isEqualTo(0);
        BDDAssertions.then(stateMachine.metrics().numberOfSuccessfulCalls()).isEqualTo(0);
    }

    @Test
    void testForceToForcedOpenState() {
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        stateMachine.forceToForcedOpenState();
        BDDAssertions.then(stateMachine.getState()).isEqualTo(CircuitBreaker.State.FORCED_OPEN);
        for (int i = 0; i < ringBufferSizeInClosedState * 10; i++) {
            then(stateMachine.isCallPermitted()).isFalse();
        }
    }

}
