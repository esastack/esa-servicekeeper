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
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;

class RequestHandleImplTest {

    @Test
    void testEndWithSuccessParallel() throws InterruptedException {
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(Collections.singletonList(
                new ConcurrentLimitMoat(new MoatConfig(ResourceId.from("testEndWithSuccessParallel")),
                        ConcurrentLimitConfig.ofDefault(),
                        ConcurrentLimitConfig.ofDefault(), null)), null);
        RequestHandle handle = chain.tryToExecute(new AsyncContext("testEndWithSuccessParallel"));

        int count = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(count);
        final AtomicInteger failsCount = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    handle.endWithSuccess();
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        then(failsCount.get()).isEqualTo(count - 1);
    }

    @Test
    void testEndWithResultParallel() throws InterruptedException {
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(Collections.singletonList(
                new RateLimitMoat(new MoatConfig(ResourceId.from("testEndWithResultParallel")),
                        RateLimitConfig.ofDefault(),
                        RateLimitConfig.ofDefault(), null)), null);
        RequestHandle handle = chain.tryToExecute(new AsyncContext("testEndWithResultParallel"));

        int count = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(count);
        final AtomicInteger failsCount = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    handle.endWithResult("ABC");
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        then(failsCount.get()).isEqualTo(count - 1);
    }

    @Test
    void testEndWithErrorParallel() throws InterruptedException {
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(Collections.singletonList(
                new CircuitBreakerMoat(new MoatConfig(ResourceId.from("testEndWithErrorParallel")),
                        CircuitBreakerConfig.ofDefault(),
                        CircuitBreakerConfig.ofDefault(), new PredicateByException())), null);
        RequestHandle handle = chain.tryToExecute(new AsyncContext("testEndWithErrorParallel"));

        int count = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(count);
        final AtomicInteger failsCount = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    handle.endWithError(new Throwable());
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        then(failsCount.get()).isEqualTo(count - 1);
    }

    @Test
    void testCompositeEndParallel() throws InterruptedException {
        AsyncExecutionChain chain = new AsyncExecutionChainImpl(Collections.singletonList(
                new CircuitBreakerMoat(new MoatConfig(ResourceId.from("testCompositeEndParallel")),
                        CircuitBreakerConfig.ofDefault(),
                        CircuitBreakerConfig.ofDefault(), new PredicateByException())), null);
        RequestHandle handle = chain.tryToExecute(new AsyncContext("testCompositeEndParallel"));

        int count = RandomUtils.randomInt(5);
        final CountDownLatch latch = new CountDownLatch(count * 3);
        final AtomicInteger failsCount = new AtomicInteger();
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    handle.endWithError(new Throwable());
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    handle.endWithSuccess();
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();

            new Thread(() -> {
                try {
                    handle.endWithResult("ABC");
                } catch (IllegalStateException ex) {
                    failsCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        then(failsCount.get()).isEqualTo(count * 3 - 1);
    }
}
