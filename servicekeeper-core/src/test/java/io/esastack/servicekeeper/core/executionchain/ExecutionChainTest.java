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
package io.esastack.servicekeeper.core.executionchain;

import io.esastack.servicekeeper.core.asynchandle.RequestHandle;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.exception.ConcurrentOverflowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.fallback.FallbackMethod;
import io.esastack.servicekeeper.core.fallback.FallbackToException;
import io.esastack.servicekeeper.core.fallback.FallbackToFunction;
import io.esastack.servicekeeper.core.fallback.FallbackToValue;
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.OPEN;
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
                RequestHandle handle = chain.tryToExecute(ctx);
                if (!handle.isAllowed()) {
                    if (handle.getNotAllowedCause() instanceof ConcurrentOverflowException) {
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
                RequestHandle handle = chain.tryToExecute(ctx);
                if (!handle.isAllowed()) {
                    if (handle.getNotAllowedCause() instanceof ConcurrentOverflowException) {
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
            final RequestHandle handle = chain.tryToExecute(ctx);
            if (handle.isAllowed()) {
                continue;
            }
            if (handle.getNotAllowedCause() instanceof RateLimitOverflowException) {
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
            final RequestHandle handle = chain.tryToExecute(ctx);
            if (handle.isAllowed()) {
                continue;
            }
            if (handle.getNotAllowedCause() instanceof RateLimitOverflowException) {
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
            RequestHandle handle = chain.tryToExecute(new AsyncContext(name));
            Throwable notAllowCause = handle.getNotAllowedCause();
            assertNull(notAllowCause);
            then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(CLOSED);
            handle.endWithError(new RuntimeException());
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
            RequestHandle handle = chain.tryToExecute(new AsyncContext(name));
            assertNull(handle.getNotAllowedCause());
            then(circuitBreakerMoat.getCircuitBreaker().getState()).isEqualTo(CLOSED);
            handle.endWithError(new RuntimeException());
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
        final RequestHandle handle = chain.tryToExecute(ctx);
        handle.endWithSuccess();
        assertThrows(IllegalStateException.class, handle::endWithSuccess);
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
        assertThrows(ClassCastException.class,
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

    @Test
    void testFallbackApplyToBizException() throws Throwable {
        Executable<String> executable = () -> {
            throw new RuntimeException();
        };
        final String name = "testFallbackApplyToBizException";
        List<Moat<?>> moats = new ArrayList<>(1);

        //fallbackToValue
        final String fallbackResult = "DEF";
        FallbackHandler<String> fallbackToString = new FallbackToValue(fallbackResult, true);
        final SyncExecutionChain fallbackToStringChain = new SyncExecutionChainImpl(moats, fallbackToString);
        then(fallbackToStringChain.execute(new AsyncContext(name), null, executable)).isEqualTo(fallbackResult);

        //fallbackToException
        final IllegalStateException fallbackEx = new IllegalStateException("fallback");
        FallbackHandler<?> fallbackToEx = new FallbackToException(fallbackEx, true);
        final SyncExecutionChain fallbackToExChain = new SyncExecutionChainImpl(moats, fallbackToEx);
        assertThrows(IllegalStateException.class,
                () -> fallbackToExChain.execute(new AsyncContext(name), null, executable));

        final Set<FallbackMethod> fallbackMethods = new HashSet<>(1);
        fallbackMethods.add(new FallbackMethod(ExecutionChainTest.class.getDeclaredMethod("fallbackMethod")));

        //fallbackToFunction
        FallbackHandler<String> fallbackToFunc = new FallbackToFunction<>(
                new ExecutionChainTest(), fallbackMethods, true);
        final SyncExecutionChain fallbackToFuncChain = new SyncExecutionChainImpl(moats, fallbackToFunc);
        then(fallbackToFuncChain.execute(new AsyncContext(name), null, executable)).isEqualTo("fallbackMethod");
    }

    private String fallbackMethod() {
        return "fallbackMethod";
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }
}
