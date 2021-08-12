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
package esa.servicekeeper.core.moats.circuitbreaker;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.MoatEvent;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.utils.ClassCastUtils;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.CLOSED;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_DISABLED;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_OPEN;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CircuitBreakerMoatTest {

    private static final String NAME = "circuitBreaker-Test";
    private static final PredicateStrategy DEFAULT_PREDICATE = new PredicateByException(null);

    private final MoatConfig moatConfig = new MoatConfig(ResourceId.from(NAME));

    private final int ringBufferSizeInClosedOpen = RandomUtils.randomInt(20);
    private final float failureRateThreshold = RandomUtils.randomFloat(100);
    private final CircuitBreakerMoat breakerMoat = new CircuitBreakerMoat(moatConfig,
            CircuitBreakerConfig.builder()
                    .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                    .failureRateThreshold(failureRateThreshold).build(),
            null, DEFAULT_PREDICATE);

    @Test
    void testGetListeningKey() {
        then(breakerMoat.listeningKey()).isEqualTo(ResourceId.from(NAME));
        then(breakerMoat.getPredicate()).isInstanceOf(PredicateByException.class);
    }

    @Test
    void testToString() {
        then(breakerMoat.toString()).isEqualTo("CircuitBreakerMoat-circuitBreaker-Test");
        then(breakerMoat.name()).isEqualTo("circuitBreaker-Test");
    }

    @Test
    void testPredicateByException() {
        final Class<? extends Throwable>[] ignoreExceptions = ClassCastUtils.cast(new Class[]{RuntimeException.class});
        PredicateStrategy predicateStrategy0 = new PredicateByException(ignoreExceptions, ignoreExceptions, null);

        final MoatConfig moatConfig0 = mock(MoatConfig.class);
        when(moatConfig0.getResourceId()).thenReturn(ResourceId.from("CircuitBreakerMoat-Test0"));
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(moatConfig0,
                CircuitBreakerConfig.builder()
                        .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                        .failureRateThreshold(failureRateThreshold).build(), null,
                predicateStrategy0,
                Collections.singletonList(new MoatEventProcessor() {
                    @Override
                    public void process(String name, MoatEvent event) {

                    }
                }), null);

        final MoatConfig moatConfig1 = mock(MoatConfig.class);
        when(moatConfig1.getResourceId()).thenReturn(ResourceId.from("CircuitBreakerMoat-Test1"));
        PredicateStrategy predicateStrategy1 = new PredicateByException(null);
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(moatConfig1,
                CircuitBreakerConfig.builder()
                        .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                        .failureRateThreshold(failureRateThreshold).build(),
                null, predicateStrategy1);

        final Context ctx = mock(Context.class);

        when(ctx.getBizException()).thenReturn(new RuntimeException());
        for (int i = 0; i < ringBufferSizeInClosedOpen; i++) {
            assertDoesNotThrow(() -> breakerMoat0.enter(ctx));
            assertDoesNotThrow(() -> breakerMoat1.enter(ctx));
            breakerMoat0.exit(ctx);
            breakerMoat1.exit(ctx);
        }

        assertDoesNotThrow(() -> breakerMoat0.enter(ctx));
        assertThrows(CircuitBreakerNotPermittedException.class,
                () -> breakerMoat1.enter(ctx));
    }

    @Test
    void testPredicateBySpendTime() {
        final long maxSpendTimeMs0 = 100L;
        Context ctx0 = mock(Context.class);
        when(ctx0.getSpendTimeMs()).thenReturn(101L);
        final MoatConfig config0 = mock(MoatConfig.class);
        when(config0.getResourceId()).thenReturn(ResourceId.from("CircuitBreakerMoat-Test2"));

        final PredicateStrategy predicateStrategy = new PredicateBySpendTime(maxSpendTimeMs0,
                -1L, null);
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(config0, CircuitBreakerConfig.builder()
                .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen).build(),
                null, predicateStrategy);
        for (int i = 0; i < ringBufferSizeInClosedOpen; i++) {
            assertDoesNotThrow(() -> breakerMoat0.enter(ctx0));
            breakerMoat0.exit(ctx0);
        }
        assertThrows(CircuitBreakerNotPermittedException.class, () -> breakerMoat0.enter(ctx0));

        Context ctx1 = mock(Context.class);
        when(ctx1.getSpendTimeMs()).thenReturn(5L);
        final MoatConfig config1 = mock(MoatConfig.class);
        when(config1.getResourceId()).thenReturn(ResourceId.from("CircuitBreakerMoat-Test3"));
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(config1, CircuitBreakerConfig.builder()
                .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen).build(),
                null, predicateStrategy);
        for (int i = 0; i < ringBufferSizeInClosedOpen; i++) {
            assertDoesNotThrow(() -> breakerMoat1.enter(ctx1));
            breakerMoat1.exit(ctx1);
        }
        assertDoesNotThrow(() -> breakerMoat1.enter(ctx1));
    }

    @Test
    void testPredicateByExceptionAndSpendTime() {
        final long maxSpendTimeMs = RandomUtils.randomLong();
        final Context ctx0 = mock(Context.class);
        when(ctx0.getSpendTimeMs()).thenReturn(maxSpendTimeMs + 1L);

        final Context ctx1 = mock(Context.class);
        when(ctx1.getBizException()).thenReturn(new RuntimeException());

        MoatConfig config = mock(MoatConfig.class);
        when(config.getResourceId()).thenReturn(ResourceId.from("CircuitBreakerMoat-Test4"));
        PredicateStrategy predicateStrategy = new PredicateByExceptionAndSpendTime(new PredicateByException(null),
                new PredicateBySpendTime(maxSpendTimeMs, -1L, null));
        CircuitBreakerMoat breakerMoat = new CircuitBreakerMoat(config, CircuitBreakerConfig.builder()
                .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                .failureRateThreshold(failureRateThreshold).build(),
                null, predicateStrategy);

        for (int i = 0; i < ringBufferSizeInClosedOpen; i++) {
            if (i % 2 == 0) {
                assertDoesNotThrow(() -> breakerMoat.enter(ctx0));
                breakerMoat.exit(ctx0);
            } else {
                assertDoesNotThrow(() -> breakerMoat.enter(ctx1));
                breakerMoat.exit(ctx1);
            }
        }
        then(breakerMoat.getCircuitBreaker().metrics().failureRateThreshold()).isEqualTo(100.0f);
        then(breakerMoat.getCircuitBreaker().metrics().numberOfSuccessfulCalls()).isEqualTo(0);
        then(breakerMoat.getCircuitBreaker().metrics().numberOfFailedCalls())
                .isEqualTo(ringBufferSizeInClosedOpen);
        then(breakerMoat.getCircuitBreaker().metrics().numberOfNotPermittedCalls()).isEqualTo(0);
        assertThrows(CircuitBreakerNotPermittedException.class, () -> breakerMoat.enter(ctx0));
        assertThrows(CircuitBreakerNotPermittedException.class, () -> breakerMoat.enter(ctx1));
    }

    @Test
    void testGetLifeCycleType() {
        then(breakerMoat.lifeCycleType()).isEqualTo(LifeCycleSupport.LifeCycleType.TEMPORARY);
        CircuitBreakerMoat breakerMoat = new CircuitBreakerMoat(moatConfig, CircuitBreakerConfig.ofDefault(),
                CircuitBreakerConfig.ofDefault(), DEFAULT_PREDICATE);
        then(breakerMoat.lifeCycleType()).isEqualTo(LifeCycleSupport.LifeCycleType.PERMANENT);
    }

    @Test
    void testGetFondConfig() {
        then(breakerMoat.getFond(null)).isNull();

        ExternalConfig config = new ExternalConfig();
        then(breakerMoat.getFond(config)).isNull();

        final float failureRateThreshold = RandomUtils.randomFloat(100);
        config.setFailureRateThreshold(failureRateThreshold);
        then(breakerMoat.getFond(config)).isNotNull();

        config.setFailureRateThreshold(null);
        config.setForcedDisabled(true);
        then(breakerMoat.getFond(config)).isNotNull();

        config.setForcedDisabled(null);
        config.setForcedOpen(true);
        then(breakerMoat.getFond(config)).isNotNull();

        config.setForcedOpen(null);
        then(breakerMoat.getFond(config)).isNull();
    }

    @Test
    void testUpdateWhenNewestConfigIsNull() {
        CircuitBreakerMoat breakerMoat = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testUpdateWhenNewestConfigIsNull-0")),
                CircuitBreakerConfig.builder()
                        .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                        .failureRateThreshold(failureRateThreshold).build(),
                null, DEFAULT_PREDICATE);

        // Current immutable config is null
        breakerMoat.updateWhenNewestConfigIsNull();
        then(breakerMoat.shouldDelete()).isTrue();
        Context ctx = mock(Context.class);
        when(ctx.getBizException()).thenReturn(new RuntimeException());

        for (int i = 0; i < ringBufferSizeInClosedOpen; i++) {
            assertDoesNotThrow(() -> breakerMoat.enter(ctx));
            breakerMoat.exit(ctx);
        }
        assertThrows(CircuitBreakerNotPermittedException.class, () -> breakerMoat.enter(ctx));

        // Immutable config is not null
        final int newRingBufferSizeInClosedOpen = RandomUtils.randomInt(200);
        final float newFailureRateThreshold = RandomUtils.randomFloat(100);

        CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testUpdateWhenNewestConfigIsNull-1")),
                CircuitBreakerConfig.builder()
                        .waitDurationInOpenState(Duration.ofSeconds(1L))
                        .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                        .failureRateThreshold(failureRateThreshold).build(),
                CircuitBreakerConfig.builder()
                        .ringBufferSizeInClosedState(newRingBufferSizeInClosedOpen)
                        .failureRateThreshold(newFailureRateThreshold).build(),
                DEFAULT_PREDICATE);

        breakerMoat1.updateWhenNewestConfigIsNull();
        then(breakerMoat1.shouldDelete()).isFalse();

        for (int i = 0; i < newRingBufferSizeInClosedOpen; i++) {
            assertDoesNotThrow(() -> breakerMoat1.enter(ctx));
            breakerMoat1.exit(ctx);
            then(breakerMoat1.getCircuitBreaker().metrics().numberOfFailedCalls()).isEqualTo(i + 1);
            then(breakerMoat1.getCircuitBreaker().metrics().numberOfSuccessfulCalls()).isEqualTo(0);
        }
        assertThrows(CircuitBreakerNotPermittedException.class, () -> breakerMoat1.enter(ctx));

        // Reset the circuit breaker
        breakerMoat1.getCircuitBreaker().reset();
        then(breakerMoat1.getCircuitBreaker().metrics().numberOfSuccessfulCalls()).isEqualTo(0);
        then(breakerMoat1.getCircuitBreaker().metrics().numberOfFailedCalls()).isEqualTo(0);
        then(breakerMoat1.getCircuitBreaker().metrics().numberOfBufferedCalls()).isEqualTo(0);
        then(breakerMoat1.getCircuitBreaker().metrics().maxNumberOfBufferedCalls())
                .isEqualTo(newRingBufferSizeInClosedOpen);
        then(breakerMoat1.getCircuitBreaker().metrics().numberOfNotPermittedCalls())
                .isEqualTo(0);
        then(breakerMoat1.getCircuitBreaker().metrics().failureRateThreshold()).isEqualTo(-1.0f);
    }

    @Test
    void testUpdateWithNewestConfig() {
        final CircuitBreakerMoat breakerMoat = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testUpdateWithNewestConfig-0")),
                CircuitBreakerConfig.builder()
                        .ringBufferSizeInClosedState(ringBufferSizeInClosedOpen)
                        .failureRateThreshold(failureRateThreshold).build(),
                null, DEFAULT_PREDICATE);
        then(breakerMoat.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(failureRateThreshold);

        // FORCED_DISABLED
        float newFailureRateThreshold = RandomUtils.randomFloat(100);
        ExternalConfig config = new ExternalConfig();
        config.setFailureRateThreshold(newFailureRateThreshold);
        config.setForcedDisabled(true);
        breakerMoat.updateWithNewestConfig(breakerMoat.getFond(config));
        then(breakerMoat.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(newFailureRateThreshold);
        then(breakerMoat.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);
        then(breakerMoat.shouldDelete()).isFalse();

        // FORCED_OPEN
        newFailureRateThreshold = RandomUtils.randomFloat(100);
        config = new ExternalConfig();
        config.setFailureRateThreshold(newFailureRateThreshold);
        config.setForcedOpen(true);
        breakerMoat.updateWithNewestConfig(breakerMoat.getFond(config));
        then(breakerMoat.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(newFailureRateThreshold);
        then(breakerMoat.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);
        then(breakerMoat.shouldDelete()).isFalse();

        breakerMoat.updateWhenNewestConfigIsNull();
        then(breakerMoat.shouldDelete()).isTrue();
    }

    @Test
    void testFailureRateThresholdUpdate0() throws InterruptedException {
        // Update failureRateThreshold when original immutable config is null.

        // Case1: DynamicConfig is null
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate0-case1")),
                CircuitBreakerConfig.ofDefault(), null, DEFAULT_PREDICATE);
        final CountDownLatch latch0 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                breakerMoat0.onUpdate(null);
            } finally {
                latch0.countDown();
            }
        }).start();
        latch0.await();
        then(breakerMoat0.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(50.0f);
        then(breakerMoat0.shouldDelete()).isTrue();

        // Case2: DynamicConfig's failureRateThreshold is null
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate0-case2")),
                CircuitBreakerConfig.ofDefault(), null, DEFAULT_PREDICATE);
        final CountDownLatch latch1 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                breakerMoat1.onUpdate(new ExternalConfig());
            } finally {
                latch1.countDown();
            }
        }).start();
        latch1.await();
        then(breakerMoat1.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(50.0f);
        then(breakerMoat1.shouldDelete()).isTrue();

        // Case3: DynamicConfig's failureRateThreshold is not null
        final CircuitBreakerMoat breakerMoat2 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate0-case3")),
                CircuitBreakerConfig.ofDefault(), null, DEFAULT_PREDICATE);
        final float newestFailureRateThreshold = RandomUtils.randomFloat(100);
        final ExternalConfig config = new ExternalConfig();
        config.setFailureRateThreshold(newestFailureRateThreshold);
        final CountDownLatch latch2 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                breakerMoat2.onUpdate(config);
            } finally {
                latch2.countDown();
            }
        }).start();
        latch2.await();
        then(breakerMoat2.getCircuitBreaker().config().getFailureRateThreshold())
                .isEqualTo(newestFailureRateThreshold);
        then(breakerMoat2.shouldDelete()).isFalse();
    }

    @Test
    void testFailureRateThresholdUpdate1() {
        // Update failureRateThreshold when original immutable config is not null.
        final float failureRateThreshold = RandomUtils.randomFloat(100);
        final CircuitBreakerConfig immutableConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(failureRateThreshold).build();

        // Case1: DynamicConfig is null
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate1-case1")),
                CircuitBreakerConfig.ofDefault(), immutableConfig, DEFAULT_PREDICATE);
        breakerMoat0.onUpdate(null);
        then(breakerMoat0.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(failureRateThreshold);
        then(breakerMoat0.shouldDelete()).isFalse();

        // Case2: DynamicConfig's failureRateThreshold is null
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate1-case2")),
                CircuitBreakerConfig.ofDefault(), immutableConfig, DEFAULT_PREDICATE);
        breakerMoat1.onUpdate(new ExternalConfig());
        then(breakerMoat1.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(failureRateThreshold);
        then(breakerMoat1.shouldDelete()).isFalse();

        // Case3: DynamicConfig's failureRateThreshold has updated
        final CircuitBreakerMoat breakerMoat2 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate1-case3")),
                CircuitBreakerConfig.ofDefault(), immutableConfig, DEFAULT_PREDICATE);
        final ExternalConfig config2 = new ExternalConfig();
        final float newestFailureRateThreshold = RandomUtils.randomFloat(100);
        config2.setFailureRateThreshold(newestFailureRateThreshold);
        then(breakerMoat2.isConfigEquals(breakerMoat2.getFond(config2))).isFalse();
        breakerMoat2.onUpdate(config2);
        then(breakerMoat2.getCircuitBreaker().config().getFailureRateThreshold())
                .isEqualTo(newestFailureRateThreshold);
        then(breakerMoat2.shouldDelete()).isFalse();

        // Case4: DynamicConfig's failureRateThreshold hasn't updated
        final CircuitBreakerMoat breakerMoat3 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testFailureRateThresholdUpdate1-case3")),
                CircuitBreakerConfig.ofDefault(), immutableConfig, DEFAULT_PREDICATE);
        final ExternalConfig config3 = new ExternalConfig();
        config3.setFailureRateThreshold(newestFailureRateThreshold);
        then(breakerMoat3.isConfigEquals(breakerMoat3.getFond(config3))).isTrue();
        breakerMoat3.onUpdate(config3);
        then(breakerMoat3.getCircuitBreaker().config().getFailureRateThreshold())
                .isEqualTo(newestFailureRateThreshold);
        then(breakerMoat3.shouldDelete()).isFalse();
    }

    @Test
    void testForcedOpenUpdate() {
        // Case1: transition from normal state to FORCED_OPEN state
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testForcedOpenUpdate")),
                CircuitBreakerConfig.ofDefault(), null, DEFAULT_PREDICATE);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);
        then(breakerMoat0.getCircuitBreaker().immutableConfig()).isNull();

        ExternalConfig config = new ExternalConfig();
        config.setForcedOpen(true);
        config.setFailureRateThreshold(60.0f);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.shouldDelete()).isFalse();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

        // Original ImmutableConfig is null
        config.setForcedOpen(false);
        config.setFailureRateThreshold(60.0f);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        config.setForcedOpen(true);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.shouldDelete()).isFalse();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

        config.setForcedOpen(null);
        config.setFailureRateThreshold(60.0f);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);
        then(breakerMoat0.shouldDelete()).isFalse();

        // Original ImmutableConfig is not null
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testForcedOpenUpdate")),
                CircuitBreakerConfig.ofDefault(), CircuitBreakerConfig.ofDefault(),
                DEFAULT_PREDICATE);
        breakerMoat1.getCircuitBreaker().forceToForcedOpenState();
        config.setForcedOpen(null);
        breakerMoat1.onUpdate(config);
        then(breakerMoat1.getCircuitBreaker().getState()).isEqualTo(CLOSED);
        breakerMoat1.getCircuitBreaker().forceToForcedOpenState();
        config.setForcedOpen(false);
        breakerMoat1.onUpdate(config);
        then(breakerMoat1.getCircuitBreaker().getState()).isEqualTo(CLOSED);
    }

    @Test
    void testForcedDisabledUpdate() {
        // Case1: transition from normal state to FORCED_OPEN state
        final CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testForcedDisabledUpdate")),
                CircuitBreakerConfig.ofDefault(), null, DEFAULT_PREDICATE);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);
        then(breakerMoat0.getCircuitBreaker().immutableConfig()).isNull();

        ExternalConfig config = new ExternalConfig();
        config.setForcedDisabled(true);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.shouldDelete()).isFalse();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);

        // Case2: transition from FORCED_STATE state to normal state

        // Original ImmutableConfig is null
        config.setForcedDisabled(false);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        breakerMoat0.getCircuitBreaker().forceToDisabledState();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);
        config.setForcedDisabled(null);
        breakerMoat0.onUpdate(config);
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);
        then(breakerMoat0.shouldDelete()).isTrue();

        // Original ImmutableConfig is not null
        final CircuitBreakerMoat breakerMoat1 = new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from("testForcedDisabledUpdate")),
                CircuitBreakerConfig.ofDefault(), CircuitBreakerConfig.ofDefault(),
                DEFAULT_PREDICATE);
        breakerMoat1.getCircuitBreaker().forceToDisabledState();
        config.setForcedDisabled(null);
        breakerMoat1.onUpdate(config);
        then(breakerMoat1.getCircuitBreaker().getState()).isEqualTo(CLOSED);
        breakerMoat1.getCircuitBreaker().forceToDisabledState();
        config.setForcedDisabled(false);
        breakerMoat1.onUpdate(config);
        then(breakerMoat1.getCircuitBreaker().getState()).isEqualTo(CLOSED);
    }

    @Test
    void testInitWithForcedOpen() {
        final ResourceId resourceId0 = ResourceId.from("testInitWithForcedOpen");
        final MoatConfig config0 = new MoatConfig(resourceId0);

        CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(config0, CircuitBreakerConfig.builder()
                .state(FORCED_OPEN)
                .build(), CircuitBreakerConfig.ofDefault(),
                new PredicateByException(null));
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

    }

    @Test
    void testInitWithForcedDisable() {
        final ResourceId resourceId0 = ResourceId.from("testInitWithForcedDisable");
        final MoatConfig config0 = new MoatConfig(resourceId0);

        CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(config0, CircuitBreakerConfig.builder()
                .state(FORCED_DISABLED)
                .build(), CircuitBreakerConfig.ofDefault(),
                new PredicateByException(null));
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);

    }

    @Test
    void testTransitionFromForcedOpen() throws InterruptedException {
        final ResourceId resourceId0 = ResourceId.from("testTransitionFromForcedOpen");
        final MoatConfig config0 = new MoatConfig(resourceId0);

        CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(config0, CircuitBreakerConfig.builder()
                .state(FORCED_OPEN)
                .build(), CircuitBreakerConfig.ofDefault(),
                new PredicateByException(null));
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

        final ExternalConfig config = new ExternalConfig();
        config.setForcedOpen(null);
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                breakerMoat0.onUpdate(config);
            } finally {
                latch.countDown();
            }
        }).start();

        latch.await();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);
    }

    @Test
    void testTransitionFromForcedDisable() throws InterruptedException {
        final ResourceId resourceId0 = ResourceId.from("testTransitionFromForcedDisable");
        final MoatConfig config0 = new MoatConfig(resourceId0);

        CircuitBreakerMoat breakerMoat0 = new CircuitBreakerMoat(config0, CircuitBreakerConfig.builder()
                .state(FORCED_DISABLED)
                .build(), CircuitBreakerConfig.ofDefault(),
                new PredicateByException(null));
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);

        final ExternalConfig config = new ExternalConfig();
        config.setForcedDisabled(null);
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                breakerMoat0.onUpdate(config);
            } finally {
                latch.countDown();
            }
        }).start();

        latch.await();
        then(breakerMoat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);
    }

}
