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
package esa.servicekeeper.core.executionchain;

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.metrics.CircuitBreakerMetrics;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.CLOSED;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class ExecutionChainTest {

    @Test
    void testTriggerConcurrentLimit0() throws InterruptedException {
        final String name = "testTriggerConcurrentLimit0";

        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(),
                null, Collections.emptyList()));

        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final int cycleCount = maxConcurrentLimit * 2;
        final AtomicInteger concurrentOverFlowCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(cycleCount);
        for (int i = 0; i < cycleCount; i++) {
            new Thread(() -> {
                Context ctx = new AsyncContext(name);
                RequestHandle requestHandle = chain.tryToExecute(ctx);
                Throwable notAllowCause = requestHandle.getNotAllowedCause();
                if (notAllowCause != null) {
                    if (notAllowCause instanceof ConcurrentOverFlowException) {
                        concurrentOverFlowCount.incrementAndGet();
                    } else {
                        fail();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        then(concurrentOverFlowCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testTriggerConcurrentLimit1() throws InterruptedException {
        final String name = "testTriggerConcurrentLimit1";

        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.builder()
                .limitForPeriod(100).build(),
                null, Collections.emptyList()));
        final int maxConcurrentLimit = RandomUtils.randomInt(20);
        moats.add(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(),
                null, Collections.emptyList()));
        moats.add(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException()));

        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final int cycleCount = maxConcurrentLimit * 2;
        final AtomicInteger concurrentOverFlowCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(cycleCount);
        for (int i = 0; i < cycleCount; i++) {
            new Thread(() -> {
                Context ctx = new AsyncContext(name);
                RequestHandle requestHandle = chain.tryToExecute(ctx);
                Throwable notAllowCause = requestHandle.getNotAllowedCause();
                if (notAllowCause != null) {
                    if (notAllowCause instanceof ConcurrentOverFlowException) {
                        concurrentOverFlowCount.incrementAndGet();
                    } else {
                        fail();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        then(concurrentOverFlowCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testTriggerRateLimit0() {
        final String name = "testTriggerRateLimit0";
        final int limitForPeriod = RandomUtils.randomInt(20);

        List<Moat<?>> moats = Collections.singletonList(new RateLimitMoat(getConfig(name), RateLimitConfig.builder()
                .limitForPeriod(limitForPeriod).build(),
                null, Collections.emptyList()));
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final int cycleCount = limitForPeriod * 2;
        final AtomicInteger rateLimitOverFlowCount = new AtomicInteger(0);
        for (int i = 0; i < cycleCount; i++) {
            Context ctx = new AsyncContext(name);
            final RequestHandle requestHandle = chain.tryToExecute(ctx);
            Throwable notAllowCause = requestHandle.getNotAllowedCause();
            if (notAllowCause == null) {
                continue;
            }
            if (notAllowCause instanceof RateLimitOverflowException) {
                rateLimitOverFlowCount.incrementAndGet();
            } else {
                fail();
            }
        }

        then(rateLimitOverFlowCount.get()).isEqualTo(limitForPeriod);
    }

    @Test
    void testTriggerRateLimit1() {
        final String name = "testTriggerRateLimit1";

        int limitForPeriod = RandomUtils.randomInt(20);
        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.builder()
                .limitForPeriod(limitForPeriod).build(),
                null, Collections.emptyList()));
        moats.add(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(100).build(), null, Collections.emptyList()));
        moats.add(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException()));

        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final int cycleCount = limitForPeriod * 2;
        final AtomicInteger rateLimitOverFlowCount = new AtomicInteger(0);
        for (int i = 0; i < cycleCount; i++) {
            Context ctx = new AsyncContext(name);
            final RequestHandle requestHandle = chain.tryToExecute(ctx);
            Throwable notAllowCause = requestHandle.getNotAllowedCause();
            if (notAllowCause == null) {
                continue;
            }
            if (notAllowCause instanceof RateLimitOverflowException) {
                rateLimitOverFlowCount.incrementAndGet();
            } else {
                fail();
            }
        }

        then(rateLimitOverFlowCount.get()).isEqualTo(limitForPeriod);
    }

    @Test
    void testTriggerCircuitBreaker0() {
        final String name = "testTriggerCircuitBreaker0";

        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());
        List<Moat<?>> moats = Collections.singletonList(circuitBreakerMoat);
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        for (int i = 0; i < 100; i++) {
            RequestHandle requestHandle = chain.tryToExecute(new AsyncContext(name));
            Throwable notAllowCause = requestHandle.getNotAllowedCause();
            assertNull(notAllowCause);
            then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(CLOSED);
            requestHandle.endWithError(new RuntimeException());
        }
        then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(OPEN);
    }

    @Test
    void testTriggerCircuitBreaker1() {
        final String name = "testTriggerCircuitBreaker1";

        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());

        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(new CircuitBreakerMoat(getConfig(name), CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException()));

        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        for (int i = 0; i < 100; i++) {
            RequestHandle requestHandle = chain.tryToExecute(new AsyncContext(name));
            assertNull(requestHandle.getNotAllowedCause());
            then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(CLOSED);
            requestHandle.endWithError(new RuntimeException());
        }
        then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(OPEN);
    }

    @Test
    void testNormal() throws Throwable {
        final String name = "testNormal";

        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.builder()
                .limitForPeriod(200).build(),
                null, Collections.emptyList()));
        moats.add(new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(200).build(),
                null, Collections.emptyList()));
        moats.add(new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException()));

        AbstractExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final AtomicInteger callNotPermitCount = new AtomicInteger(0);
        for (int i = 0; i < 200; i++) {
            final Context ctx = new AsyncContext(name);
            chain.execute(ctx, null, () -> null);
            chain.endWithSuccess(ctx);
        }

        then(callNotPermitCount.get()).isEqualTo(0);
        then(((RateLimitMoat) moats.get(0)).rateLimiter().metrics().numberOfWaitingThreads()).isEqualTo(0);
        then(((ConcurrentLimitMoat) moats.get(1)).getConcurrentLimiter().metrics().currentCallCount())
                .isEqualTo(0);
        CircuitBreakerMetrics metrics = ((CircuitBreakerMoat) moats.get(2)).getCircuitBreaker().metrics();
        then(metrics.numberOfNotPermittedCalls()).isEqualTo(0);
        then(metrics.numberOfFailedCalls()).isEqualTo(0);
        then(metrics.numberOfSuccessfulCalls()).isEqualTo(100);
    }

    @Test
    void testParallelEndAndClean() {
        final String name = "testParallelEndAndClean";

        List<Moat<?>> moats = new ArrayList<>(1);
        moats.add(new ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.builder().threshold(20).build(),
                null, Collections.emptyList()));
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        final Context ctx = new AsyncContext(name);
        final RequestHandle requestHandle = chain.tryToExecute(ctx);
        requestHandle.endWithSuccess();
        assertThrows(IllegalStateException.class, requestHandle::endWithSuccess);
    }

    @Test
    void testNotStartedException0() {
        final String name = "testNotStartedException0";

        List<Moat<?>> moats = Collections.singletonList(new
                ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        assertThrows(IllegalStateException.class, () -> chain.endWithSuccess(new AsyncContext(name)));
    }

    @Test
    void testNotStartedException1() {
        final String name = "testNotStartedException1";

        List<Moat<?>> moats = Collections.singletonList(new
                ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        assertThrows(IllegalStateException.class, () -> chain.endWithResult(new AsyncContext(name), new Object()));
    }

    @Test
    void testNotStartedException2() {
        final String name = "testNotStartedException2";

        List<Moat<?>> moats = Collections.singletonList(new
                ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        ExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        assertThrows(IllegalStateException.class,
                () -> chain.endWithError(new AsyncContext(name), new RuntimeException()));
    }

    @Test
    void testGetThrowable() {
        Executable<Object> executable = () -> {
            throw new RuntimeException();
        };

        final String name = "testGetThrowable";
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());

        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(circuitBreakerMoat);
        SyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        assertThrows(RuntimeException.class,
                () -> chain.execute(new AsyncContext(name), null, executable));
    }

    @Test
    void testGetResult() throws Throwable {
        final String result = "ABC";
        Executable<String> executable = () -> result;
        final String name = "testGetResult";
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(getConfig(name),
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());

        List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new ConcurrentLimitMoat(getConfig(name), ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(new RateLimitMoat(getConfig(name), RateLimitConfig.ofDefault(),
                null, Collections.emptyList()));
        moats.add(circuitBreakerMoat);
        SyncExecutionChain chain = new AsyncExecutionChainImpl(moats, null);
        then(chain.execute(new AsyncContext(name), null, executable)).isEqualTo(result);
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }
}
