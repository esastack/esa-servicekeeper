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
package esa.servicekeeper.core.retry;

import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.ServiceRetryException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.metrics.RetryMetrics;
import esa.servicekeeper.core.retry.internal.impl.ExceptionPredicate;
import esa.servicekeeper.core.retry.internal.impl.ExponentialBackOffPolicy;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetryOperationsImplTest {

    @Test
    void testGetConfig() {
        final RetryConfig config = RetryConfig.ofDefault();
        final RetryOperations operations = new RetryOperationsImpl(ResourceId.from("testGetConfig"),
                null, new ExponentialBackOffPolicy(0, 0, 0),
                new ExceptionPredicate(3), config, null);
        then(operations.getConfig()).isEqualTo(config);
    }

    @Test
    void testGetFond() {
        final RetryConfig config = RetryConfig.ofDefault();
        final RetryOperationsImpl operations = new RetryOperationsImpl(ResourceId.from("testGetFond"),
                null, new ExponentialBackOffPolicy(0, 0, 0),
                new ExceptionPredicate(3), config, null);
        then(operations.getFond(null)).isNull();

        final ExternalConfig config0 = new ExternalConfig();
        final int maxAttempts = RandomUtils.randomInt(100);
        config0.setMaxAttempts(maxAttempts);

        final long delay = RandomUtils.randomLong();
        config0.setDelay(delay);

        final long maxDelay = RandomUtils.randomLong();
        config0.setMaxDelay(maxDelay);

        final double multiplier = ThreadLocalRandom.current().nextDouble();
        config0.setMultiplier(multiplier);

        final RetryConfig config1 = operations.getFond(config0);
        then(config1.getMaxAttempts()).isEqualTo(maxAttempts);
        then(config1.getBackoffConfig().getDelay()).isEqualTo(delay);
        then(config1.getBackoffConfig().getMaxDelay()).isEqualTo(maxDelay);
        then(config1.getBackoffConfig().getMultiplier()).isEqualTo(multiplier);
    }

    @Test
    void testListeningKey() {
        final ResourceId resourceId = ResourceId.from("testListeningKey");

        final RetryConfig config = RetryConfig.ofDefault();
        final RetryOperationsImpl operations = new RetryOperationsImpl(resourceId,
                null, new ExponentialBackOffPolicy(0, 0, 0),
                new ExceptionPredicate(3), config, null);
        then(operations.listeningKey()).isEqualTo(resourceId);
    }

    @Test
    void testExecute() throws Throwable {
        final RetryOperations operations = new RetryOperationsImpl(ResourceId.from("testExecute"),
                null,
                new ExponentialBackOffPolicy(0, 0, 0),
                new ExceptionPredicate(3), RetryConfig.ofDefault(), null);

        final RetryContext context0 = buildContext();
        final FakeService service = new FakeService();
        operations.execute(context0, () -> {
            service.doIncrement0();
            return null;
        });

        then(service.index0.get()).isEqualTo(3);

        final RetryMetrics metrics0 = operations.getMetrics();
        then(metrics0.maxAttempts()).isEqualTo(3);
        then(metrics0.retriedTimes()).isEqualTo(1);
        then(metrics0.totalRetriedCount()).isEqualTo(2);

        final RetryContext context1 = buildContext();
        operations.execute(context1, () -> {
            service.doIncrement1();
            return null;
        });
        then(service.index1.get()).isEqualTo(2);

        final RetryMetrics metrics1 = operations.getMetrics();
        then(metrics1.maxAttempts()).isEqualTo(3);
        then(metrics1.retriedTimes()).isEqualTo(2);
        then(metrics1.totalRetriedCount()).isEqualTo(3);
    }

    @Test
    void testUpdate() {
        final RetryOperationsImpl operations = new RetryOperationsImpl(ResourceId.from("testUpdate"),
                null, new ExponentialBackOffPolicy(0,
                0, 0),
                new ExceptionPredicate(3), RetryConfig.ofDefault(), null);

        operations.onUpdate(null);
        then(operations.shouldDelete()).isTrue();

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxAttempts(1);
        operations.onUpdate(config0);

        final RetryContext context0 = buildContext();
        final FakeService service = new FakeService();
        try {
            operations.execute(context0, () -> {
                service.doIncrement0();
                return null;
            });
        } catch (Throwable th) {
            // Do nothing
        }

        then(service.index0.get()).isEqualTo(1);

        final RetryMetrics metrics0 = operations.getMetrics();
        then(metrics0.maxAttempts()).isEqualTo(1);
        then(metrics0.retriedTimes()).isEqualTo(0);
        then(metrics0.totalRetriedCount()).isEqualTo(0);
    }

    @Test
    void testContextProxy() {
        final Context ctx = mock(Context.class);
        final RuntimeException bizEx = new RuntimeException();
        final ServiceRetryException retryEx = new ServiceRetryException("", new IllegalStateException());
        final RetryOperationsImpl.ContextProxy proxy = new RetryOperationsImpl.ContextProxy(ctx,
                bizEx, retryEx);
        then(proxy.getBizException()).isSameAs(bizEx);
        then(proxy.getThroughFailsCause()).isSameAs(retryEx);

        when(ctx.getResult()).thenReturn("ABC");
        then(proxy.getResult()).isEqualTo("ABC");

        when(ctx.getSpendTimeMs()).thenReturn(100L);
        then(proxy.getSpendTimeMs()).isEqualTo(100L);
    }

    @Test
    void testUpdateParallel() throws InterruptedException {
        final RetryOperationsImpl operations = new RetryOperationsImpl(ResourceId.from("testUpdate"),
                null,
                new ExponentialBackOffPolicy(0, 0, 0),
                new ExceptionPredicate(3), RetryConfig.ofDefault(), null);

        final CountDownLatch latch0 = new CountDownLatch(1);
        new Thread(() -> {
            operations.onUpdate(null);
            latch0.countDown();
        }).start();

        latch0.await();
        then(operations.shouldDelete()).isTrue();

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxAttempts(1);

        final CountDownLatch latch1 = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                operations.onUpdate(config0);
                latch1.countDown();
            }).start();
        }
        latch1.await();

        final RetryContext context0 = buildContext();
        final FakeService service = new FakeService();
        try {
            operations.execute(context0, () -> {
                service.doIncrement0();
                return null;
            });
        } catch (Throwable th) {
            // Do nothing
        }

        then(service.index0.get()).isEqualTo(1);

        final RetryMetrics metrics0 = operations.getMetrics();
        then(metrics0.maxAttempts()).isEqualTo(1);
        then(metrics0.retriedTimes()).isEqualTo(0);
        then(metrics0.totalRetriedCount()).isEqualTo(0);
    }

    private RetryContext buildContext() {
        final OriginalInvocation invocation = mock(OriginalInvocation.class);
        final Context context = mock(Context.class);
        return new RetryContext(context, invocation);
    }

    private static class FakeService {

        private final AtomicInteger index0 = new AtomicInteger(0);
        private final AtomicInteger index1 = new AtomicInteger(0);

        private void doIncrement0() {
            index0.incrementAndGet();
            throw new RuntimeException();
        }

        private void doIncrement1() {
            index1.incrementAndGet();
            if (index1.get() < 2) {
                throw new RuntimeException();
            }
        }
    }

}
