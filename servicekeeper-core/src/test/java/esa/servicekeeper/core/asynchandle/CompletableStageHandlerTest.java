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
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.fallback.FallbackMethod;
import esa.servicekeeper.core.fallback.FallbackToFunction;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletableStageHandlerTest {

    private static final String fallbackValue = "XYZ";

    @Test
    void testWhenHandleNull() throws Throwable {
        final String name = "testWhenHandleNull";
        final List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from(name)),
                ConcurrentLimitConfig.ofDefault(), null,
                Collections.emptyList()));
        final Executable<CompletionStage<String>> executable = () -> null;

        final AsyncExecutionChain chain = new AsyncExecutionChainImpl(moats,
                null);
        final CompletableStageHandler<?> handler = new CompletableStageHandler<>();
        AsyncContext context = new AsyncContext(name);
        assertNull(chain.asyncExecute(context, null, executable, handler));

        assertTrue(context.getSpendTimeMs() >= 0);
        assertNull(context.getResult());
        assertNull(context.getBizException());
    }

    @Test
    void testConcurrentLimit() throws Throwable {
        final String name = "testConcurrentLimit";
        final int maxConcurrentLimit = 1;

        AtomicInteger times = new AtomicInteger(0);
        final Supplier<List<Moat<?>>> moatsSupplier = () -> Collections.singletonList(new ConcurrentLimitMoat(new MoatConfig(
                ResourceId.from(name + times.incrementAndGet())),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList()));

        final Executable<CompletionStage<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(10L);
            } catch (Exception ex) {
                // Do nothing
            }
            return "ABC";
        });

        // dont,t use fallback and not apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, false,
                maxConcurrentLimit * 2, maxConcurrentLimit, maxConcurrentLimit, 0,
                0, true);

        // dont,t use fallback but apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, true,
                maxConcurrentLimit * 2, maxConcurrentLimit, maxConcurrentLimit, 0,
                0, true);

        // use fallback but not apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, false,
                maxConcurrentLimit * 2, maxConcurrentLimit, 0, maxConcurrentLimit,
                0, true);

        // use fallback and apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, true,
                maxConcurrentLimit * 2, maxConcurrentLimit, 0, maxConcurrentLimit,
                0, true);
    }

    @Test
    void testRateLimit() throws Throwable {
        final Executable<CompletionStage<String>> executable = () -> CompletableFuture.supplyAsync(() -> "Hello");

        final String name = "testRateLimit";
        final int limitForPeriod = 1;
        AtomicInteger times = new AtomicInteger(0);
        final Supplier<List<Moat<?>>> moatsSupplier = () -> Collections.singletonList(new RateLimitMoat(
                getConfig(name + times.incrementAndGet()),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod).build(), null,
                Collections.emptyList()));

        // dont,t use fallback and not apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, false,
                limitForPeriod * 2, limitForPeriod, limitForPeriod, 0,
                0, false);

        // dont,t use fallback but apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, true,
                limitForPeriod * 2, limitForPeriod, limitForPeriod, 0,
                0, false);

        // use fallback but not apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, false,
                limitForPeriod * 2, limitForPeriod, 0, limitForPeriod,
                0, false);

        // use fallback and apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, true,
                limitForPeriod * 2, limitForPeriod, 0, limitForPeriod,
                0, false);
    }

    @Test
    void testCircuitBreaker0() throws Throwable {
        final String name = "testCircuitBreaker0";

        final int ringBufferSizeInClosedState = 2;
        final Executable<CompletionStage<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException();
        });

        AtomicInteger times = new AtomicInteger(0);
        final Supplier<List<Moat<?>>> moatsSupplier = () -> Collections.singletonList(new CircuitBreakerMoat(
                getConfig(name + times.incrementAndGet()),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(),
                new PredicateByException()));

        // dont,t use fallback and not apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, false,
                ringBufferSizeInClosedState * 2, 0, ringBufferSizeInClosedState, 0,
                ringBufferSizeInClosedState, false);

        // dont,t use fallback but apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, true,
                ringBufferSizeInClosedState * 2, 0, ringBufferSizeInClosedState, 0,
                ringBufferSizeInClosedState, false);

        // use fallback but not apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, false,
                ringBufferSizeInClosedState * 2, 0, 0, ringBufferSizeInClosedState,
                ringBufferSizeInClosedState, false);

        // use fallback and apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, true,
                ringBufferSizeInClosedState * 2, 0, 0, 2 * ringBufferSizeInClosedState,
                0, false);
    }

    @Test
    void testCircuitBreaker1() throws Throwable {
        final String name = "testCircuitBreaker1";
        final int ringBufferSizeInClosedState = 2;
        final Executable<CompletionStage<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(30L);
            } catch (Exception ex) {
                // Do nothing
            }
            return "ABC";
        });

        Supplier<List<Moat<?>>> moatsSupplier = () -> Collections.singletonList(new CircuitBreakerMoat(getConfig(name + System.currentTimeMillis()),
                CircuitBreakerConfig.builder().ringBufferSizeInClosedState(ringBufferSizeInClosedState).build(),
                CircuitBreakerConfig.ofDefault(),
                new PredicateBySpendTime(3L)));

        // dont,t use fallback and not apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, false,
                ringBufferSizeInClosedState * 2, ringBufferSizeInClosedState, ringBufferSizeInClosedState, 0,
                0, false);

        // dont,t use fallback but apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, true,
                ringBufferSizeInClosedState * 2, ringBufferSizeInClosedState, ringBufferSizeInClosedState, 0,
                0, false);

        // use fallback but not apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, false,
                ringBufferSizeInClosedState * 2, ringBufferSizeInClosedState, 0, ringBufferSizeInClosedState,
                0, false);

        // use fallback and apply to BizException
        testAsyncExecute(executable, moatsSupplier, true, true,
                ringBufferSizeInClosedState * 2, ringBufferSizeInClosedState, 0, ringBufferSizeInClosedState,
                0, false);

    }

    @Test
    void testOnlyFallback() throws Throwable {
        final int maxPassRequestCount = 5;
        Supplier<List<Moat<?>>> moatsSupplier = ArrayList::new;
        final AtomicInteger passRequestCount = new AtomicInteger(0);
        final Executable<CompletionStage<String>> executable = () -> CompletableFuture.supplyAsync(() -> {
            if (passRequestCount.incrementAndGet() > maxPassRequestCount) {
                throw new RuntimeException("error");
            }
            return "ABC";
        });
        final int maxRequestCount = 10;
        // dont,t use fallback and not apply to BizException
        testAsyncExecute(executable, moatsSupplier, false, false,
                maxRequestCount, maxPassRequestCount, 0, 0,
                maxRequestCount - maxPassRequestCount, false);

        // dont,t use fallback but apply to BizException
        passRequestCount.set(0);
        testAsyncExecute(executable, moatsSupplier, false, true,
                maxRequestCount, maxPassRequestCount, 0, 0,
                maxRequestCount - maxPassRequestCount, false);

        // use fallback but not apply to BizException
        passRequestCount.set(0);
        testAsyncExecute(executable, moatsSupplier, true, false,
                maxRequestCount, maxPassRequestCount, 0, 0,
                maxRequestCount - maxPassRequestCount, false);

        // use fallback and apply to BizException
        passRequestCount.set(0);
        testAsyncExecute(executable, moatsSupplier, true, true,
                maxRequestCount, maxPassRequestCount, 0, maxRequestCount - maxPassRequestCount,
                0, false);
    }

    void testAsyncExecute(Executable<CompletionStage<String>> executable,
                          Supplier<List<Moat<?>>> moatsSupplier,
                          boolean useFallback,
                          boolean alsoApplyToBizException,
                          int maxRequestCount,
                          int expectSuccessRequestsCount,
                          int expectNotPermitRequestsCount,
                          int expectFallbacksCount,
                          int expectBizExceptionsCount,
                          boolean isConcurrent) throws Throwable {
        AsyncExecutionChain chain;
        if (useFallback) {
            chain = new AsyncExecutionChainImpl(moatsSupplier.get(),
                    createFallbackToFunction(fallbackValue, alsoApplyToBizException));
        } else {
            chain = new AsyncExecutionChainImpl(moatsSupplier.get(), null);
        }

        AtomicInteger fallbackCount = new AtomicInteger(0);
        AtomicInteger successRequestsCount = new AtomicInteger(0);
        AtomicInteger bizExceptionsCount = new AtomicInteger(0);
        int notPermitRequestsCount = 0;

        List<CompletionStage<String>> concurrentResultList = null;
        if (isConcurrent) {
            concurrentResultList = new ArrayList<>();
        }
        for (int i = 0; i < maxRequestCount; i++) {
            try {
                CompletionStage<String> result = chain.asyncExecute(new AsyncContext("testAsyncExecute"), null,
                        executable, new CompletableStageHandler<>());
                if (isConcurrent) {
                    concurrentResultList.add(result);
                } else {
                    countByResult(result, successRequestsCount, fallbackCount, bizExceptionsCount);
                }
            } catch (ServiceKeeperNotPermittedException e) {
                notPermitRequestsCount++;
            }
        }

        if (isConcurrent) {
            for (CompletionStage<String> result : concurrentResultList) {
                countByResult(result, successRequestsCount, fallbackCount, bizExceptionsCount);
            }
        }

        then(successRequestsCount.get()).isEqualTo(expectSuccessRequestsCount);
        then(fallbackCount.get()).isEqualTo(expectFallbacksCount);
        then(notPermitRequestsCount).isEqualTo(expectNotPermitRequestsCount);
        then(bizExceptionsCount.get()).isEqualTo(expectBizExceptionsCount);
    }

    private void countByResult(CompletionStage<String> result, AtomicInteger successRequestsCount,
                               AtomicInteger fallbackCount, AtomicInteger bizExceptionsCount) {
        try {
            if (fallbackValue.equals(result.toCompletableFuture().get())) {
                fallbackCount.incrementAndGet();
            } else {
                successRequestsCount.incrementAndGet();
            }
        } catch (Throwable bizException) {
            bizExceptionsCount.incrementAndGet();
        }
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }

    private <T> FallbackToFunction<T> createFallbackToFunction(T fallbackValue, boolean alsoApplyToBizException)
            throws Throwable {
        final Set<FallbackMethod> methods = new HashSet<>(1);
        methods.add(new FallbackMethod(FallbackFunction.class.getDeclaredMethod("doFallback")));
        return new FallbackToFunction<>(new FallbackFunction<>(fallbackValue), methods, alsoApplyToBizException);
    }

    private static class FallbackFunction<T> {
        private final T fallbackValue;

        public FallbackFunction(T fallbackValue) {
            this.fallbackValue = fallbackValue;
        }

        private CompletionStage<T> doFallback() {
            return CompletableFuture.completedFuture(fallbackValue);
        }
    }
}
