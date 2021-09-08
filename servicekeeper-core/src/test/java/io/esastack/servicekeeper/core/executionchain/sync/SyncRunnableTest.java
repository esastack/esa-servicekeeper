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
package io.esastack.servicekeeper.core.executionchain.sync;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.exception.ConcurrentOverflowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.executionchain.SyncContext;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChain;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChainImpl;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

class SyncRunnableTest {

    @Test
    void testRunnableTriggerConcurrentLimit() throws InterruptedException {
        final Runnable runnable = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500L);
            } catch (InterruptedException e) {
                // Do nothing
            }
        };

        final String name = "testRunnableTriggerConcurrentLimit";
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        final ConcurrentLimitMoat concurrentLimitMoat = new ConcurrentLimitMoat(getConfig(name),
                ConcurrentLimitConfig.builder().threshold(maxConcurrentLimit).build(), null,
                Collections.emptyList());
        List<Moat<?>> moats = Collections.singletonList(concurrentLimitMoat);
        SyncExecutionChain chain = new SyncExecutionChainImpl(moats, null);

        final CountDownLatch latch = new CountDownLatch(maxConcurrentLimit * 2);
        final AtomicInteger concurrentOverFlowCount = new AtomicInteger(0);
        for (int i = 0; i < maxConcurrentLimit * 2; i++) {
            new Thread(() -> {
                try {
                    chain.execute(new SyncContext(name), null, runnable);
                } catch (ConcurrentOverflowException ex) {
                    concurrentOverFlowCount.incrementAndGet();
                } catch (Throwable throwable) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        then(concurrentOverFlowCount.get()).isEqualTo(maxConcurrentLimit);
    }

    @Test
    void testRunnableTriggerRateLimit() {
        final Runnable Runnable = () -> {
        };

        final String name = "testRunnableTriggerRateLimit";
        final int limitForPeriod = RandomUtils.randomInt(5);
        List<Moat<?>> moats = Collections.singletonList(new RateLimitMoat(getConfig(name),
                RateLimitConfig.builder().limitForPeriod(limitForPeriod).build(), null,
                Collections.emptyList()));
        SyncExecutionChain chain = new SyncExecutionChainImpl(moats, null);
        final AtomicInteger rateLimitOverFlowCount = new AtomicInteger(0);

        for (int i = 0; i < limitForPeriod * 2; i++) {
            try {
                chain.execute(new SyncContext(name), null, Runnable);
            } catch (RateLimitOverflowException ex) {
                rateLimitOverFlowCount.incrementAndGet();
            } catch (Throwable throwable) {
                fail();
            }
        }
        then(rateLimitOverFlowCount.get()).isEqualTo(limitForPeriod);
    }

    private MoatConfig getConfig(String name) {
        return new MoatConfig(ResourceId.from(name));
    }
}
