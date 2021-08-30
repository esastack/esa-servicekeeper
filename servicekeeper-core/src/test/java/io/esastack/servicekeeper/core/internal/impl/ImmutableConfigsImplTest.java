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
package io.esastack.servicekeeper.core.internal.impl;

import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.moats.MoatType;
import io.esastack.servicekeeper.core.utils.ParameterUtils;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.getMaxSizeLimit;
import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.CIRCUITBREAKER_CONFIG;
import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.CONCURRENTLIMIT_CONFIG;
import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.FALLBACK_CONFIG;
import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.RATELIMIT_CONFIG;
import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.RETRY_CONFIG;
import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class ImmutableConfigsImplTest {

    private final ImmutableConfigs configs = new ImmutableConfigsImpl();

    @Test
    void testGetGroupId() {
        configs.getOrCompute(ResourceId.from("testGetGroupId1"), () -> null);
        then(configs.getGroupId(ResourceId.from("testGetGroupId1"))).isNull();

        configs.getOrCompute(ResourceId.from("testGetGroupId2"),
                () -> CompositeServiceKeeperConfig.builder().group(GroupResourceId.from("demoGroup")).build());
        then(configs.getGroupId(ResourceId.from("testGetGroupId2")))
                .isEqualTo(GroupResourceId.from("demoGroup"));
    }

    @Test
    void testGetGroupItems() {
        then(configs.getGroupItems(null)).isEmpty();
        then(configs.getGroupItems(GroupResourceId.from("testGetGroupItems1"))).isEmpty();
        configs.getOrCompute(ResourceId.from("testGetGroupItems2"),
                () -> CompositeServiceKeeperConfig.builder().group(GroupResourceId.from("demoGroup1"))
                        .methodConfig(null).build());
        configs.getOrCompute(ResourceId.from("testGetGroupItems3"),
                () -> CompositeServiceKeeperConfig.builder().group(GroupResourceId.from("demoGroup1")).build());
        then(configs.getGroupItems(GroupResourceId.from("demoGroup1"))
                .contains(ResourceId.from("testGetGroupItems2"))).isTrue();
        then(configs.getGroupItems(GroupResourceId.from("demoGroup1"))
                .contains(ResourceId.from("testGetGroupItems3"))).isTrue();
    }

    @Test
    void testGetMaxSizeLimit() {
        final ResourceId resourceId = ResourceId.from("testGetMaxSizeLimit");
        final String argName0 = "arg0";
        then(configs.getMaxSizeLimit(resourceId, argName0, MoatType.RATE_LIMIT)).isEqualTo(null);
        then(configs.getMaxSizeLimit(resourceId, argName0, CIRCUIT_BREAKER)).isEqualTo(null);
        then(configs.getMaxSizeLimit(resourceId, argName0, MoatType.CONCURRENT_LIMIT)).isEqualTo(null);

        final Map<Object, Integer> concurrentLimitMap = new LinkedHashMap<>(3);
        concurrentLimitMap.put("abc", 20);
        concurrentLimitMap.put("def", 30);
        concurrentLimitMap.put("xyz", 40);

        final CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder()
                .argRateLimitConfig(0, "arg0", null, null, 77)
                .argConcurrentLimit(0, "arg0", concurrentLimitMap, null)
                .argCircuitBreakerConfig(0, "arg0", null, null,
                        99)
                .argConcurrentLimit(1, "arg1", concurrentLimitMap, 333)
                .argRateLimitConfig(1, "arg1", null, concurrentLimitMap, null)
                .argCircuitBreakerConfig(1, "arg1", null, null,
                        null)

                .argCircuitBreakerConfig(2, "arg2", null, null,
                        null)
                .argRateLimitConfig(2, "arg2", null, null, null)
                .argConcurrentLimit(2, "arg2", null, null).build();


        configs.getOrCompute(resourceId, () -> config);
        then(configs.getMaxSizeLimit(resourceId, argName0, MoatType.RATE_LIMIT)).isEqualTo(77);
        then(configs.getMaxSizeLimit(resourceId, argName0, MoatType.CONCURRENT_LIMIT))
                .isEqualTo(getMaxSizeLimit());
        then(configs.getMaxSizeLimit(resourceId, argName0, CIRCUIT_BREAKER)).isEqualTo(99);

        final String argName1 = "arg1";
        then(configs.getMaxSizeLimit(resourceId, argName1, MoatType.RATE_LIMIT)).isEqualTo(getMaxSizeLimit());
        then(configs.getMaxSizeLimit(resourceId, argName1, MoatType.CONCURRENT_LIMIT)).isEqualTo(333);
        then(configs.getMaxSizeLimit(resourceId, argName1, CIRCUIT_BREAKER)).isNull();

        final String argName2 = "arg2";
        then(configs.getMaxSizeLimit(resourceId, argName2, MoatType.RATE_LIMIT)).isNull();
        then(configs.getMaxSizeLimit(resourceId, argName2, MoatType.CONCURRENT_LIMIT)).isNull();
        then(configs.getMaxSizeLimit(resourceId, argName2, CIRCUIT_BREAKER)).isNull();
    }

    @Test
    void testGetFallbackConfig() {
        then(configs.getConfig(null, FALLBACK_CONFIG)).isNull();
        then(configs.getConfig(ResourceId.from("testGetFallbackConfig1"), FALLBACK_CONFIG)).isNull();

        Map<Object, Float> failureRateThresholdMap = new HashMap<>(2);
        failureRateThresholdMap.putIfAbsent("abc", RandomUtils.randomFloat(100));
        failureRateThresholdMap.putIfAbsent("def", RandomUtils.randomFloat(100));

        final ResourceId methodId = ResourceId.from("testGetFallbackConfig2");
        configs.getOrCompute(methodId,
                () -> CompositeServiceKeeperConfig.builder()
                        .methodConfig(ServiceKeeperConfig.builder()
                                .fallbackConfig(FallbackConfig.ofDefault()).build())
                        .argCircuitBreakerConfig(0, "arg0",
                                null, failureRateThresholdMap, null).build());
        then(configs.getConfig(methodId, FALLBACK_CONFIG)).isNotNull();
        then(configs.getConfig(new ArgResourceId(methodId,
                "arg0", "abc"), FALLBACK_CONFIG)).isNull();
        then(configs.getConfig(new ArgResourceId(methodId,
                "arg0", "def"), FALLBACK_CONFIG)).isNull();
    }

    @Test
    void testGetRetryConfig() {
        then(configs.getConfig(null, RETRY_CONFIG)).isNull();
        then(configs.getConfig(ResourceId.from("testGetRetryConfig1"), RETRY_CONFIG)).isNull();

        Map<Object, Float> failureRateThresholdMap = new HashMap<>(2);
        failureRateThresholdMap.putIfAbsent("abc", RandomUtils.randomFloat(100));
        failureRateThresholdMap.putIfAbsent("def", RandomUtils.randomFloat(100));

        final ResourceId methodId = ResourceId.from("testGetRetryConfig2");
        configs.getOrCompute(methodId,
                () -> CompositeServiceKeeperConfig.builder()
                        .methodConfig(ServiceKeeperConfig.builder()
                                .retryConfig(RetryConfig.ofDefault()).build())
                        .argCircuitBreakerConfig(0, "arg0",
                                null, failureRateThresholdMap, null).build());
        then(configs.getConfig(methodId, RETRY_CONFIG)).isNotNull();
        then(configs.getConfig(new ArgResourceId(methodId,
                "arg0", "abc"), RETRY_CONFIG)).isNull();
        then(configs.getConfig(new ArgResourceId(methodId,
                "arg0", "def"), RETRY_CONFIG)).isNull();
    }

    @Test
    void testGetRateLimitConfig() {
        then(configs.getConfig(null, RATELIMIT_CONFIG)).isNull();
        then(configs.getConfig(ResourceId.from("testGetRateLimitConfig1"), RATELIMIT_CONFIG)).isNull();

        Map<Object, Integer> limitForPeriodMap = new HashMap<>(2);
        limitForPeriodMap.putIfAbsent("abc", 88);
        limitForPeriodMap.putIfAbsent("def", 99);

        final ResourceId methodId = ResourceId.from("testGetRateLimitConfig2");
        configs.getOrCompute(methodId,
                () -> CompositeServiceKeeperConfig.builder()
                        .methodConfig(ServiceKeeperConfig.builder()
                                .rateLimiterConfig(RateLimitConfig.ofDefault()).build())
                        .argRateLimitConfig(0, "arg0",
                                null, limitForPeriodMap, null).build());
        then(configs.getConfig(methodId, RATELIMIT_CONFIG)).isNotNull();
        then(((RateLimitConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "abc"), RATELIMIT_CONFIG)).getLimitForPeriod()).isEqualTo(88);
        then(((RateLimitConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "def"), RATELIMIT_CONFIG)).getLimitForPeriod()).isEqualTo(99);
    }

    @Test
    void testGetConcurrentLimitConfig() {
        then(configs.getConfig(null, CONCURRENTLIMIT_CONFIG)).isNull();
        then(configs.getConfig(ResourceId.from("testGetConcurrentLimitConfig1"), CONCURRENTLIMIT_CONFIG)).isNull();

        Map<Object, Integer> concurrentLimitMap = new HashMap<>(2);
        concurrentLimitMap.putIfAbsent("abc", 88);
        concurrentLimitMap.putIfAbsent("def", 99);

        final ResourceId methodId = ResourceId.from("testGetConcurrentLimitConfig2");
        configs.getOrCompute(methodId,
                () -> CompositeServiceKeeperConfig.builder()
                        .methodConfig(ServiceKeeperConfig.builder()
                                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build())
                        .argConcurrentLimit(0, "arg0", concurrentLimitMap, null).build());
        then(configs.getConfig(methodId, CONCURRENTLIMIT_CONFIG)).isNotNull();
        then(((ConcurrentLimitConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "abc"), CONCURRENTLIMIT_CONFIG)).getThreshold()).isEqualTo(88);
        then(((ConcurrentLimitConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "def"), CONCURRENTLIMIT_CONFIG)).getThreshold()).isEqualTo(99);
    }

    @Test
    void testGetCircuitBreakerConfig() {
        then(configs.getConfig(null, CIRCUITBREAKER_CONFIG)).isNull();
        then(configs.getConfig(ResourceId.from("testGetCircuitBreakerConfig1"), CIRCUITBREAKER_CONFIG)).isNull();

        Map<Object, Float> failureRateThresholdMap = new HashMap<>(2);
        failureRateThresholdMap.putIfAbsent("abc", 88.0f);
        failureRateThresholdMap.putIfAbsent("def", 99.0f);

        final ResourceId methodId = ResourceId.from("testGetCircuitBreakerConfig2");
        configs.getOrCompute(methodId,
                () -> CompositeServiceKeeperConfig.builder()
                        .methodConfig(ServiceKeeperConfig.builder()
                                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build())
                        .argCircuitBreakerConfig(0, "arg0", failureRateThresholdMap,
                                null).build());
        then(configs.getConfig(methodId, CIRCUITBREAKER_CONFIG)).isNotNull();
        then(((CircuitBreakerConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "abc"), CIRCUITBREAKER_CONFIG)).getFailureRateThreshold()).isEqualTo(88.0f);
        then(((CircuitBreakerConfig) configs.getConfig(new ArgResourceId(methodId,
                "arg0", "def"), CIRCUITBREAKER_CONFIG)).getFailureRateThreshold()).isEqualTo(99.0f);
    }

    @Test
    void testGetConfigOfMatchAll() {
        final ResourceId methodId = ResourceId.from("testGetConfigOfMatchAll");

        configs.getOrCompute(methodId, () -> CompositeServiceKeeperConfig.builder()
                .argRateLimitConfig(0, Collections.singletonMap(VALUE_MATCH_ALL, 10))
                .argConcurrentLimit(0, Collections.singletonMap(VALUE_MATCH_ALL, 20))
                .argCircuitBreakerConfig(0, Collections.singletonMap(VALUE_MATCH_ALL, 30.0f))
                .build());

        final String argName = ParameterUtils.defaultName(0);
        then(configs.getMaxSizeLimit(methodId, argName, CIRCUIT_BREAKER)).isEqualTo(100);
        then(configs.getMaxSizeLimit(methodId, argName, RATE_LIMIT)).isEqualTo(100);
        then(configs.getMaxSizeLimit(methodId, argName, CONCURRENT_LIMIT)).isEqualTo(100);

        final RateLimitConfig config = (RateLimitConfig) configs.getConfig(
                new ArgResourceId(methodId, argName, "arg0"), RATELIMIT_CONFIG);
        then(config).isNotNull();
        then(config.getLimitForPeriod()).isEqualTo(10);

        final ConcurrentLimitConfig config1 = (ConcurrentLimitConfig) configs.getConfig(
                new ArgResourceId(methodId, argName, "arg0"), CONCURRENTLIMIT_CONFIG);
        then(config1).isNotNull();
        then(config1.getThreshold()).isEqualTo(20);

        final CircuitBreakerConfig config2 = (CircuitBreakerConfig) configs.getConfig(
                new ArgResourceId(methodId, argName, "arg0"), CIRCUITBREAKER_CONFIG);
        then(config2).isNotNull();
        then(config2.getFailureRateThreshold()).isEqualTo(30.0f);
    }

    @Test
    void testParallel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                configs.getOrCompute(ResourceId.from("testParallel"),
                        () -> CompositeServiceKeeperConfig.builder()
                                .methodConfig(ServiceKeeperConfig.builder()
                                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                                        .retryConfig(RetryConfig.ofDefault())
                                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                                        .rateLimiterConfig(RateLimitConfig.ofDefault()).build()).build());
                latch.countDown();
            }).start();
        }
        latch.await();
        then(configs.getConfig(ResourceId.from("testParallel"), CIRCUITBREAKER_CONFIG))
                .isEqualTo(CircuitBreakerConfig.ofDefault());

        then(configs.getConfig(ResourceId.from("testParallel"), RATELIMIT_CONFIG))
                .isEqualTo(RateLimitConfig.ofDefault());

        then(configs.getConfig(ResourceId.from("testParallel"), CONCURRENTLIMIT_CONFIG))
                .isEqualTo(ConcurrentLimitConfig.ofDefault());

        then(configs.getConfig(ResourceId.from("testParallel"), RETRY_CONFIG))
                .isEqualTo(RetryConfig.ofDefault());
    }
}
