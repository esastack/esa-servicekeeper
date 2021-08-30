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
package io.esastack.servicekeeper.core.executionchain.async;

import io.esastack.servicekeeper.core.asynchandle.CompletableStageHandler;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import io.esastack.servicekeeper.core.exception.ConcurrentOverFlowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.executionchain.AsyncContext;
import io.esastack.servicekeeper.core.executionchain.AsyncExecutionChain;
import io.esastack.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import io.esastack.servicekeeper.core.executionchain.Executable;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.fallback.FallbackMethod;
import io.esastack.servicekeeper.core.fallback.FallbackToException;
import io.esastack.servicekeeper.core.fallback.FallbackToFunction;
import io.esastack.servicekeeper.core.fallback.FallbackToValue;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class AsyncExecutionChainTest {

    @Test
    void testAsyncTriggerConcurrentLimit0() throws Throwable {
        final String name = "testAsyncTriggerConcurrentLimit0";
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);

        final CountDownLatch latch = new CountDownLatch(maxConcurrentLimit);
        Executable<CompletableFuture<Void>> executable = () -> CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(30L);
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
                        new CompletableStageHandler<>());
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
        List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(new MoatConfig(ResourceId.from(name)),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, new FallbackToValue(fallbackValue, false));

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

        final AtomicInteger fallbackTimesCount = new AtomicInteger(0);
        final AtomicInteger normalCount = new AtomicInteger(0);
        for (int i = 0; i < maxConcurrentLimit * 2; i++) {
            Object result = chain.asyncExecute(new AsyncContext(name), null,
                    executable, new CompletableStageHandler<>());
            if (result.equals(fallbackValue)) {
                fallbackTimesCount.incrementAndGet();
            } else {
                normalCount.incrementAndGet();
            }
        }

        latch.await();
        then(fallbackTimesCount.get()).isEqualTo(maxConcurrentLimit);
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
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final AtomicInteger rateLimitOverFlowCount = new AtomicInteger(0);

        for (int i = 0; i < limitForPeriod * 2; i++) {
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
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
                new MoatConfig(ResourceId.from(name)),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod)
                        .limitRefreshPeriod(Duration.ofSeconds(10L)).build(),
                null,
                Collections.emptyList()));
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats,
                new FallbackToException(fallbackException, false));

        final AtomicInteger fallbackTimesCount = new AtomicInteger(0);
        final AtomicInteger normalCount = new AtomicInteger(0);
        for (int i = 0; i < limitForPeriod * 2; i++) {
            try {
                final Object result = chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
                if (result != null) {
                    normalCount.incrementAndGet();
                }
            } catch (RateLimitOverflowException ex) {
                fail();
            } catch (Exception ex) {
                fallbackTimesCount.incrementAndGet();
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(fallbackTimesCount.get()).isEqualTo(limitForPeriod);
        then(normalCount.get()).isEqualTo(limitForPeriod);
    }

    @Test
    void testAsyncTriggerCircuitBreaker0() throws InterruptedException {
        final String name = "testAsyncTriggerCircuitBreaker0";

        final int ringBufferSizeInClosedState = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(ringBufferSizeInClosedState);
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(30L);
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
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
            } catch (Throwable throwable) {
                fail();
            }
        }
        latch.await();

        // Wait until all CompletableFuture completed
        TimeUnit.MILLISECONDS.sleep(30L);
        int circuitBreakerNotPermittedCount = 0;
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
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
                TimeUnit.MILLISECONDS.sleep(30L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
            throw new RuntimeException();
        });

        List<Moat<?>> moats = Collections.singletonList(new CircuitBreakerMoat(
                new MoatConfig(ResourceId.from(fallbackValue)),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(), new PredicateByException()));
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats,
                    new FallbackToValue(fallbackValue, false));
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
            } catch (Throwable throwable) {
                fail(throwable);
            }
        }
        latch.await();

        // Wait until all CompletableFuture completed
        TimeUnit.MILLISECONDS.sleep(20L);
        int circuitBreakerNotPermittedCount = 0;
        for (int i = 0; i < ringBufferSizeInClosedState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats,
                    new FallbackToValue(fallbackValue, false));
            try {
                final Object result = chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
                if (result.equals(fallbackValue)) {
                    circuitBreakerNotPermittedCount++;
                }
            } catch (Throwable throwable) {
                fail(throwable);
            }
        }
        then(circuitBreakerNotPermittedCount).isLessThanOrEqualTo(ringBufferSizeInClosedState);
    }

    @Test
    void testFallbackApplyToBizException() throws Throwable {
        Executable<CompletionStage<String>> executable = () -> {
            throw new RuntimeException();
        };
        final String name = "testFallbackApplyToBizException";
        List<Moat<?>> moats = new ArrayList<>(1);

        //fallbackToException
        final IllegalStateException fallbackEx = new IllegalStateException("fallback");
        FallbackHandler<?> fallbackToEx = new FallbackToException(fallbackEx, true);
        final AsyncExecutionChain fallbackToExChain = new AsyncExecutionChainImpl(moats, fallbackToEx);
        assertThrows(IllegalStateException.class, () -> fallbackToExChain.asyncExecute(new AsyncContext(name),
                null, executable, new CompletableStageHandler<>()).toCompletableFuture().get());

        final Set<FallbackMethod> fallbackMethods = new HashSet<>(1);
        fallbackMethods.add(new FallbackMethod(AsyncExecutionChainTest.class.getDeclaredMethod("fallbackMethod")));

        //fallbackToFunction
        FallbackHandler<String> fallbackToFunc = new FallbackToFunction<>(
                new AsyncExecutionChainTest(), fallbackMethods, true);
        final AsyncExecutionChain fallbackToFuncChain = new AsyncExecutionChainImpl(moats, fallbackToFunc);
        then(fallbackToFuncChain.asyncExecute(new AsyncContext(name),
                null, executable, new CompletableStageHandler<>()).toCompletableFuture().get())
                .isEqualTo("fallbackMethod");
    }

    private CompletionStage<String> fallbackMethod() {
        return CompletableFuture.completedFuture("fallbackMethod");
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }
}
