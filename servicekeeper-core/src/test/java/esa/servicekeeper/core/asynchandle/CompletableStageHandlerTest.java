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
package esa.servicekeeper.core.asynchandle;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.fallback.FallbackToValue;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class CompletableStageHandlerTest {

    @Test
    void testWhenHandleNull() {
        final String fallbackValue = "XYZ";
        final String name = "testWhenHandleNull";
        final List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from(name)),
                ConcurrentLimitConfig.ofDefault(), null,
                Collections.emptyList()));
        final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats,
                new FallbackToValue(fallbackValue, false));

        final CompletableStageHandler<?> handler = new CompletableStageHandler<>();
        assertThrows(IllegalStateException.class,
                () -> handler.handle(null, RequestHandleImpl.createAllowHandle(chain, new AsyncContext(name), null)));
    }

    @Test
    void testConcurrentLimit() throws Throwable {
        final String fallbackValue = "XYZ";
        final String name = "testConcurrentLimit";
        final int maxConcurrentLimit = 1;
        final List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(new MoatConfig(
                ResourceId.from(name)),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));
        final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, new FallbackToValue(fallbackValue, false));

        final CountDownLatch latch = new CountDownLatch(maxConcurrentLimit);

        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(3L);
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
                    executable, new CompletableStageHandler<>());
            if (result.equals(fallbackValue)) {
                fallbackCount.incrementAndGet();
            } else {
                normalCount.incrementAndGet();
            }
        }

        latch.await();
        then(fallbackCount.get()).isEqualTo(maxConcurrentLimit);
        then(normalCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testRateLimit() {
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> "Hello");

        final String name = "testRateLimit";
        final int limitForPeriod = 1;
        final List<Moat<?>> moats = Collections.singletonList(new RateLimitMoat(getConfig(name),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod).build(), null,
                Collections.emptyList()));
        final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
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
    void testCircuitBreaker0() throws InterruptedException {
        final String name = "testCircuitBreaker0";

        final int ringBufferSizeInCloseState = 2;
        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException();
        });

        final List<Moat<?>> moats = Collections.singletonList(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInCloseState).build(),
                CircuitBreakerConfig.ofDefault(),
                new PredicateByException()));
        for (int i = 0; i < ringBufferSizeInCloseState; i++) {
            final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
            try {
                chain.asyncExecute(new AsyncContext(name), null,
                        executable, new CompletableStageHandler<>());
            } catch (Throwable throwable) {
                fail();
            }
        }

        // Wait until all CompletableFuture completed
        TimeUnit.MILLISECONDS.sleep(500L);
        int circuitBreakerNotPermittedCount = 0;
        for (int i = 0; i < ringBufferSizeInCloseState; i++) {
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
        then(circuitBreakerNotPermittedCount).isEqualTo(ringBufferSizeInCloseState);
    }

    @Test
    void testCircuitBreaker1() throws InterruptedException {
        final String name = "testCircuitBreaker1";

        final int ringBufferSizeInClosedState = 2;
        final CountDownLatch latch = new CountDownLatch(ringBufferSizeInClosedState);

        final Executable<CompletableFuture<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(30L);
            } catch (Exception ex) {
                // Do nothing
            } finally {
                latch.countDown();
            }
            return "ABC";
        });

        List<Moat<?>> moats = Collections.singletonList(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(),
                new PredicateBySpendTime(3L)));
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
        TimeUnit.MILLISECONDS.sleep(500L);
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

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }
}
