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
package esa.servicekeeper.core.executionchain.async;

import esa.servicekeeper.core.asynchandle.CompletableStageHandler;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.fallback.FallbackToException;
import esa.servicekeeper.core.fallback.FallbackToValue;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

class AsyncExecutionChainTest {

    @Test
    void testAsyncTriggerConcurrentLimit0() throws Throwable {
        final String name = "testAsyncTriggerConcurrentLimit0";
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);

        final CountDownLatch latch = new CountDownLatch(maxConcurrentLimit);
        Executable<CompletableFuture<Void>> executable = () -> CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
        });

        final AtomicInteger concurrentOverFlowCount = new AtomicInteger(0);
        for (int i = 0; i < maxConcurrentLimit * 2; i++) {
            try {
                chain.asyncExecute(new AsyncContext(name), null, executable,
                        new CompletableStageHandler());
            } catch (ConcurrentOverFlowException ex) {
                concurrentOverFlowCount.incrementAndGet();
            }
        }

        latch.await();
        then(concurrentOverFlowCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testAsyncTriggerConcurrentLimit1() throws Throwable {
        final String fallbackValue = "XYZ";
        final String name = "testAsyncTriggerConcurrentLimit1";
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(new MoatConfig(ResourceId.from(name),
                new FallbackToValue(fallbackValue)),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);

        final CountDownLatch latch = new CountDownLatch(maxConcurrentLimit);
        Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(30L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
            return "ABC";
        });

        final AtomicInteger fallbackCount = new AtomicInteger(0);
        final AtomicInteger normalCount = new AtomicInteger(0);
        for (int i = 0; i < maxConcurrentLimit * 2; i++) {
            Object result = chain.asyncExecute(new AsyncContext(name), null,
                    executable, new CompletableStageHandler());
            if (fallbackValue.equals(result)) {
                fallbackCount.incrementAndGet();
            } else if (result != null) {
                normalCount.incrementAndGet();
            }
        }

        latch.await();
        then(fallbackCount.get()).isEqualTo(maxConcurrentLimit);
        then(normalCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testAsyncTriggerRateLimit0() {
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> "Hello");

        final String name = "testAsyncTriggerRateLimit0";
        final int limitForPeriod = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new RateLimitMoat(getConfig(name),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);
        final AtomicInteger rateLimitOverFlowCount = new AtomicInteger(0);

        for (int i = 0; i < limitForPeriod * 2; i++) {
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
            } catch (RateLimitOverflowException ex) {
                rateLimitOverFlowCount.incrementAndGet();
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(rateLimitOverFlowCount.get()).isEqualTo(limitForPeriod);
    }

    @Test
    void testAsyncTriggerRateLimit1() {
        final Exception fallbackException = new Exception("Custom fallback");
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> "Hello");

        final String name = "testAsyncTriggerRateLimit1";
        final int limitForPeriod = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new RateLimitMoat(
                new MoatConfig(ResourceId.from(name), new FallbackToException(fallbackException)),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);

        final AtomicInteger fallbackCount = new AtomicInteger(0);
        final AtomicInteger normalCount = new AtomicInteger(0);
        for (int i = 0; i < limitForPeriod * 2; i++) {
            try {
                final Object result = chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
                if (result != null) {
                    normalCount.incrementAndGet();
                }
            } catch (RateLimitOverflowException ex) {
                fail();
            } catch (Exception ex) {
                fallbackCount.incrementAndGet();
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(fallbackCount.get()).isEqualTo(limitForPeriod);
        then(normalCount.get()).isEqualTo(limitForPeriod);
    }

    @Test
    void testAsyncTriggerCircuitBreaker0() throws InterruptedException {
        final String name = "testAsyncTriggerCircuitBreaker0";

        final int ringBufferSizeInClosedState = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(ringBufferSizeInClosedState);
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
            throw new RuntimeException();
        });

        List<Moat<?>> moats = Collections.singletonList(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(),
                new PredicateByException()));
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
            } catch (Throwable throwable) {
                fail();
            }
        }
        latch.await();

        // Wait until all CompletableFuture completed
        TimeUnit.MILLISECONDS.sleep(20L);
        int circuitBreakerNotPermittedCount = 0;
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
            } catch (CircuitBreakerNotPermittedException ex) {
                circuitBreakerNotPermittedCount++;
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(circuitBreakerNotPermittedCount).isEqualTo(ringBufferSizeInClosedState);
    }

    @Test
    void testAsyncTriggerCircuitBreaker1() throws InterruptedException {
        final String fallbackValue = "XYZ";
        final String name = "testAsyncTriggerCircuitBreaker1";

        final int ringBufferSizeInClosedState = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(ringBufferSizeInClosedState);
        final Executable<CompletableFuture<Object>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
            throw new RuntimeException();
        });

        List<Moat<?>> moats = Collections.singletonList(new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from(fallbackValue), new FallbackToValue(fallbackValue)),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(), new PredicateByException()));
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
            } catch (Throwable throwable) {
                fail();
            }
        }
        latch.await();

        // Wait until all CompletableFuture completed
        TimeUnit.MILLISECONDS.sleep(20L);
        int circuitBreakerNotPermittedCount = 0;
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats);
            try {
                final Object result = chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler());
                if (fallbackValue.equals(result)) {
                    circuitBreakerNotPermittedCount++;
                }
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(circuitBreakerNotPermittedCount).isLessThanOrEqualTo(ringBufferSizeInClosedState);
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name), null);
    }
}
