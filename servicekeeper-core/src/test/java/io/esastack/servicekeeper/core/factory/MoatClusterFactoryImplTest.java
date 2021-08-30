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
package io.esastack.servicekeeper.core.factory;

import esa.commons.StringUtils;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.BackoffConfig;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import io.esastack.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import io.esastack.servicekeeper.core.moats.FallbackMoatCluster;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.MoatClusterImpl;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.BDDAssertions.then;

class MoatClusterFactoryImplTest {

    private final ImmutableConfigs immutableConfigs = new ImmutableConfigsImpl();
    private final InternalMoatCluster cluster = new CacheMoatClusterImpl();
    private final MoatClusterFactoryImpl factory = new MoatClusterFactoryImpl(LimitableMoatFactoryContext.builder()
            .limite(key -> true)
            .processors(Collections.emptyList())
            .listeners(null)
            .cProcessors(new SateTransitionProcessorFactoryImpl())
            .build(), cluster, immutableConfigs);

    @Test
    void testMethodsMoats() {
        final ResourceId resourceId = ResourceId.from("testMethodsMoats");
        MoatCluster cluster0 = factory.getOrCreate(resourceId, () -> null, () ->
                ServiceKeeperConfig.builder()
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault())
                        .build(), () -> null, false);
        then(cluster0.getAll().size()).isEqualTo(3);
        then(cluster0).isInstanceOf(MoatClusterImpl.class);
        cluster.remove(resourceId);

        cluster0 = factory.getOrCreate(resourceId, () -> null, () ->
                ServiceKeeperConfig.builder()
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault())
                        .retryConfig(RetryConfig.ofDefault())
                        .build(), () -> null, false);
        then(cluster0).isInstanceOf(RetryableMoatCluster.class);
        then(cluster0.getAll().size()).isEqualTo(3);
        cluster.remove(resourceId);
        BDDAssertions.then(cluster.getAll()).isEmpty();
    }

    @Test
    void testCount() {
        final int originalCount = cluster.getAll().size();

        final RateLimitConfig rateLimitConfig = RateLimitConfig.builder()
                .limitForPeriod(RandomUtils.randomInt(500))
                .limitRefreshPeriod(Duration.ofSeconds(RandomUtils.randomLong())).build();
        final ConcurrentLimitConfig concurrentLimitConfig = ConcurrentLimitConfig.builder()
                .threshold(RandomUtils.randomInt(200)).build();
        final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.builder()
                .ringBufferSizeInClosedState(RandomUtils.randomInt(500))
                .ringBufferSizeInHalfOpenState(RandomUtils.randomInt(800))
                .failureRateThreshold(RandomUtils.randomFloat(80))
                .predicateStrategy(PredicateBySpendTime.class)
                .maxSpendTimeMs(RandomUtils.randomLong())
                .build();
        final RetryConfig retryConfig = RetryConfig.builder().maxAttempts(RandomUtils.randomInt(5))
                .backoffConfig(BackoffConfig.builder()
                        .delay(RandomUtils.randomLong())
                        .multiplier(2.0d).build()).build();

        final int count = RandomUtils.randomInt(5000);
        for (int i = 0; i < count; i++) {
            factory.getOrCreate(ResourceId.from("testCount" + i),
                    null,
                    () -> ServiceKeeperConfig.builder().rateLimiterConfig(rateLimitConfig)
                            .concurrentLimiterConfig(concurrentLimitConfig)
                            .circuitBreakerConfig(circuitBreakerConfig)
                            .retryConfig(retryConfig)
                            .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);
        }
        then(cluster.getAll().size()).isEqualTo(count + originalCount);
    }

    @Test
    void testArgsMoats0() {
        final RetryableMoatCluster cluster0 =
                (RetryableMoatCluster) factory.getOrCreate(ResourceId.from("testArgsMoats0"), null,
                        () -> ServiceKeeperConfig.builder().fallbackConfig(
                                FallbackConfig.builder().specifiedValue("ABC").build()).build(), null, false);
        BDDAssertions.then(cluster0.fallbackHandler().getType())
                .isEqualTo(FallbackHandler.FallbackType.FALLBACK_TO_VALUE);

        final MoatCluster cluster = factory.getOrCreate(new
                        ArgResourceId(ResourceId.from("testArgsMoats0"), "a", "b"),
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .retryConfig(RetryConfig.ofDefault())
                        .build(), null, false);
        then(cluster).isNotNull();
        then(cluster.getAll().size()).isEqualTo(3);
        then(cluster).isNotInstanceOf(RetryableMoatCluster.class);
    }

    @Test
    void testArgsMoats1() {
        final ExternalConfig config = new ExternalConfig();
        config.setFallbackValue("ABC");
        final FallbackMoatCluster cluster0 =
                (FallbackMoatCluster) factory.getOrCreate(ResourceId.from("testArgsMoats1"), null,
                        () -> null, () -> config, false);
        then(cluster0.fallbackHandler().getType()).isEqualTo(FallbackHandler.FallbackType.FALLBACK_TO_VALUE);

        final MoatCluster cluster = factory.getOrCreate(new
                        ArgResourceId(ResourceId.from("testArgsMoats1"), "a", "b"),
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .retryConfig(RetryConfig.ofDefault())
                        .build(), null, false);
        then(cluster).isNotNull();
        then(cluster.getAll().size()).isEqualTo(3);
        then(cluster).isNotInstanceOf(RetryableMoatCluster.class);
    }

    @Test
    void testTryToNewAndAddCircuitBreakerMoat() {
        // Method level
        final ResourceId resourceId = ResourceId.from("testTryToNewAndAddCircuitBreakerMoat");
        final MoatCluster cluster = factory.getOrCreate(resourceId,
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(resourceId, cluster, null);
        then(cluster.getAll().size()).isEqualTo(2);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxSpendTimeMs(20L);
        config0.setFailureRateThreshold(RandomUtils.randomFloat(50));

        factory.update(resourceId, cluster, config0);
        then(cluster.getAll().size()).isEqualTo(3);


        // Arg level
        final ArgResourceId argId = new ArgResourceId(resourceId, "abc", "xyz");
        final MoatCluster cluster1 = factory.getOrCreate(argId,
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(argId, cluster1, null);
        then(cluster1.getAll().size()).isEqualTo(1);

        // Only external circuitBreaker config
        final ExternalConfig config1 = new ExternalConfig();
        config1.setMaxSpendTimeMs(20L);
        config1.setFailureRateThreshold(RandomUtils.randomFloat(50));

        factory.update(argId, cluster1, config1);
        then(cluster1.getAll().size()).isEqualTo(2);
    }

    @Test
    void testTryToNewAndAddCircuitBreakerMoat1() {
        // Method level
        final ResourceId resourceId = ResourceId.from("testTryToNewAndAddCircuitBreakerMoat1");
        final MoatCluster cluster = factory.getOrCreate(resourceId,
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(resourceId, cluster, null);
        then(cluster.getAll().size()).isEqualTo(2);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setForcedOpen(true);
        factory.update(resourceId, cluster, config0);
        then(cluster.getAll().size()).isEqualTo(3);
    }

    @Test
    void testTryToNewAndAddConcurrentLimitMoat() {
        final ResourceId resourceId = ResourceId.from("testTryToNewAndAddConcurrentLimitMoat");
        final MoatCluster cluster = factory.getOrCreate(resourceId,
                null,
                () -> ServiceKeeperConfig.builder().rateLimiterConfig(RateLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(resourceId, cluster, null);
        then(cluster.getAll().size()).isEqualTo(1);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxConcurrentLimit(10);

        factory.update(resourceId, cluster, config0);
        then(cluster.getAll().size()).isEqualTo(2);
    }

    @Test
    void testTryToNewAndAddRateLimitMoat() {
        final ResourceId resourceId = ResourceId.from("testTryToNewAndAddRateLimitMoat");
        final MoatCluster cluster = factory.getOrCreate(resourceId,
                null,
                () -> ServiceKeeperConfig.builder()
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(resourceId, cluster, null);
        then(cluster.getAll().size()).isEqualTo(1);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setLimitForPeriod(10);

        factory.update(resourceId, cluster, config0);
        then(cluster.getAll().size()).isEqualTo(2);
    }

    @Test
    void testTryToNewAndAddRetryExecutor() {
        // Method level
        final ResourceId resourceId = ResourceId.from("testTryToNewAndAddRetryExecutor");
        final RetryableMoatCluster cluster = (RetryableMoatCluster) factory.getOrCreate(resourceId,
                null,
                () -> ServiceKeeperConfig.builder()
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(resourceId, cluster, null);
        then(cluster.getAll().size()).isEqualTo(1);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxAttempts(10);

        factory.update(resourceId, cluster, config0);
        then(cluster.getAll().size()).isEqualTo(1);
        then(cluster.retryExecutor()).isNotNull();


        // Arg level
        final ArgResourceId argId = new ArgResourceId(ResourceId.from("testTryToNewAndAddRetryExecutor"),
                "abc", "xyz");
        final MoatCluster cluster1 = factory.getOrCreate(argId,
                null,
                () -> ServiceKeeperConfig.builder()
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build(), null, false);

        factory.update(argId, cluster1, null);
        then(cluster1.getAll().size()).isEqualTo(1);

        final ExternalConfig config1 = new ExternalConfig();
        config1.setMaxAttempts(10);

        factory.update(argId, cluster1, config1);
        then(cluster1.getAll().size()).isEqualTo(1);
        then(cluster1).isNotInstanceOf(RetryableMoatCluster.class);
    }

    @Test
    void testFillingConfig() throws Exception {
        final Method method = Hello.class.getDeclaredMethod("sayHello", String.class);
        // Case 1: FallbackConfig is null
        factory.tryToFillConfig(null, method);

        // Case 2: Method is null
        final FallbackConfig config = FallbackConfig.ofDefault();
        factory.tryToFillConfig(config, null);
        then(config.getSpecifiedException()).isNull();
        then(config.getTargetClass()).isNull();
        then(StringUtils.isEmpty(config.getMethodName())).isTrue();
        then(StringUtils.isEmpty(config.getSpecifiedValue())).isTrue();

        // Case 3: FallbackConfig's methodName and targetClass are null
        final FallbackConfig config1 = FallbackConfig.builder().specifiedException(RuntimeException.class)
                .specifiedValue("AB").build();
        factory.tryToFillConfig(config1, method);
        then(config1.getSpecifiedValue()).isEqualTo("AB");
        then(config1.getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(StringUtils.isEmpty(config1.getMethodName())).isTrue();
        then(config1.getTargetClass()).isNull();

        // Case 4: FallbackConfig's methodName is null
        final FallbackConfig config2 = FallbackConfig.builder().specifiedException(RuntimeException.class)
                .specifiedValue("AB").targetClass(Object.class).build();
        factory.tryToFillConfig(config2, method);
        then(config2.getSpecifiedValue()).isEqualTo("AB");
        then(config2.getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(config2.getMethodName()).isEqualTo(method.getName());
        then(config2.getTargetClass()).isEqualTo(Object.class);

        // Case 5: FallbackConfig's targetClass is null
        final FallbackConfig config3 = FallbackConfig.builder().specifiedException(RuntimeException.class)
                .specifiedValue("AB").methodName("DEF").build();
        factory.tryToFillConfig(config3, method);
        then(config3.getSpecifiedValue()).isEqualTo("AB");
        then(config3.getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(config3.getMethodName()).isEqualTo("DEF");
        then(config3.getTargetClass()).isEqualTo(method.getDeclaringClass());

        // Case 6: FallbackConfig's methodName and targetClass aren't null
        final FallbackConfig config4 = FallbackConfig.builder().specifiedException(RuntimeException.class)
                .specifiedValue("AB").methodName("DEF").targetClass(Object.class).build();
        factory.tryToFillConfig(config4, method);
        then(config4.getSpecifiedValue()).isEqualTo("AB");
        then(config4.getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(config4.getMethodName()).isEqualTo("DEF");
        then(config4.getTargetClass()).isEqualTo(Object.class);
    }

    @Test
    void testParallel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(10);

        final ResourceId resourceId = ResourceId.from("testParallel");
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                factory.getOrCreate(resourceId, () -> null, () ->
                        ServiceKeeperConfig.builder()
                                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                                .rateLimiterConfig(RateLimitConfig.ofDefault())
                                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                                .fallbackConfig(FallbackConfig.ofDefault())
                                .build(), () -> null, false);

                latch.countDown();
            }).start();
        }

        latch.await();
        then(cluster.getAll().size()).isEqualTo(1);
        final MoatCluster cluster0 = cluster.get(resourceId);
        then(cluster0).isInstanceOf(MoatClusterImpl.class);
    }

    private static class Hello {

        private String sayHello(String name) {
            return name;
        }

    }
}
