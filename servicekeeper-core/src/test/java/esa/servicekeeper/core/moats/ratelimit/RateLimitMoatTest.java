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
package esa.servicekeeper.core.moats.ratelimit;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.MoatEvent;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitMoatTest {

    private final int limitForPeriod = RandomUtils.randomInt(5);
    private final MoatConfig moatConfig = new MoatConfig(ResourceId.from("rateLimitMoat-test"));
    private final RateLimitConfig limitConfig = RateLimitConfig.builder().limitRefreshPeriod(Duration.ofMillis(50L))
            .limitForPeriod(limitForPeriod).build();

    private RateLimitMoat limitMoat;

    @BeforeEach
    void setUp() {
        limitMoat = new RateLimitMoat(moatConfig, limitConfig, null, Collections.emptyList());
    }

    @Test
    void testTryThrough() {
        final MoatConfig moatConfig = new MoatConfig(ResourceId.from("testATryThrough"));
        final RateLimitMoat limitMoat = new RateLimitMoat(moatConfig, limitConfig,
                null,
                Collections.singletonList(new MoatEventProcessor() {
                    @Override
                    public void process(String name, MoatEvent event) {

                    }
                }));

        for (int i = 0; i < limitForPeriod; i++) {
            assertDoesNotThrow(() -> limitMoat.tryThrough(null));
        }
        assertThrows(RateLimitOverflowException.class, () -> limitMoat.tryThrough(null));
        for (int i = 0; i < limitForPeriod; i++) {
            limitMoat.exit(null);
        }
    }

    @Test
    void testToString() {
        then(limitMoat.toString()).isEqualTo("RateLimitMoat-rateLimitMoat-test");
        then(limitMoat.name()).isEqualTo("rateLimitMoat-test");
    }

    @Test
    void testGetFondConfig() {
        then(limitMoat.getFond(null)).isNull();
        then(limitMoat.getFond(new ExternalConfig())).isNull();

        ExternalConfig config = new ExternalConfig();
        then(limitMoat.getFond(config)).isNull();

        config.setLimitForPeriod(10);
        then(limitMoat.getFond(config)).isNotNull();
    }

    @Test
    void testGetLifeCycleType() {
        then(limitMoat.lifeCycleType()).isEqualTo(LifeCycleSupport.LifeCycleType.TEMPORARY);
    }

    @Test
    void testListeningKey() {
        then(limitMoat.listeningKey()).isEqualTo(ResourceId.from("rateLimitMoat-test"));
    }

    @Test
    void updateWhenFondConfigIsNull() {
        // Original immutable config is null
        limitMoat.updateWhenNewestConfigIsNull();
        then(limitMoat.shouldDelete()).isTrue();

        // Original immutable config is not null
        RateLimitConfig immutableConfig = mock(RateLimitConfig.class);
        int limitForPeriod = RandomUtils.randomInt(5);
        when(immutableConfig.getLimitForPeriod()).thenReturn(limitForPeriod);
        then(limitMoat.lifeCycleType()).isEqualTo(LifeCycleSupport.LifeCycleType.TEMPORARY);

        final RateLimitMoat limitMoat = new RateLimitMoat(new MoatConfig(
                ResourceId.from("updateWhenFondConfigIsNull")),
                limitConfig, immutableConfig, Collections.emptyList());
        then(limitMoat.shouldDelete()).isFalse();
    }

    @Test
    void updateWithFondConfig() {
        final int limitForPeriod = RandomUtils.randomInt(50);
        ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(limitForPeriod);

        final RateLimitMoat limitMoat = new RateLimitMoat(
                new MoatConfig(ResourceId.from("updateWithFondConfig")),
                limitConfig,
                RateLimitConfig.builder()
                        .limitRefreshPeriod(Duration.ofMillis(200L)).build(), Collections.emptyList());
        limitMoat.updateWithNewestConfig(limitMoat.getFond(config));

        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + 200L);
        for (int i = 0; i < limitForPeriod; i++) {
            assertDoesNotThrow(() -> limitMoat.tryThrough(null));
        }
        assertThrows(RateLimitOverflowException.class, () -> limitMoat.tryThrough(null));
    }

    @Test
    void testUpdateLimitForPeriod0() throws InterruptedException {
        // Update limitForPeriod when original immutable RateLimitConfig is null.
        then(limitMoat.rateLimiter().immutableConfig()).isNull();
        then(limitMoat.shouldDelete()).isFalse();

        // Case1: DynamicConfig is null
        final CountDownLatch latch0 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat.onUpdate(null);
            } finally {
                latch0.countDown();
            }
        }).start();
        latch0.await();
        then(limitMoat.shouldDelete()).isTrue();

        // Case2: DynamicConfig's limitForPeriod is null
        final RateLimitMoat limitMoat0 = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitForPeriod0-case2")),
                limitConfig, null, Collections.emptyList());
        final CountDownLatch latch1 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat0.onUpdate(new ExternalConfig());
            } finally {
                latch1.countDown();
            }
        }).start();
        latch1.await();
        then(limitMoat0.shouldDelete()).isTrue();

        // Case3: DynamicConfig's limitForPeriod is not null.
        final RateLimitMoat limitMoat1 = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitForPeriod0-case3")),
                limitConfig, null, Collections.emptyList());
        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(200));
        final CountDownLatch latch2 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat1.onUpdate(config);
            } finally {
                latch2.countDown();
            }
        }).start();
        latch2.await();
        then(limitMoat1.shouldDelete()).isFalse();
    }

    @Test
    void testUpdateLimitForPeriod1() {
        // Update limitForPeriod when original immutable RateLimitConfig is not null.
        final int limitForPeriod = RandomUtils.randomInt(20);
        RateLimitConfig immutableConfig = RateLimitConfig.builder().limitForPeriod(limitForPeriod).build();

        // Case1: DynamicConfig is null
        final RateLimitMoat limitMoat0 = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitForPeriod1-case1")),
                limitConfig, immutableConfig, Collections.emptyList());
        then(limitMoat0.rateLimiter().config().getLimitRefreshPeriod()).isNotEqualTo(limitForPeriod);
        limitMoat0.onUpdate(null);
        then(limitMoat0.shouldDelete()).isFalse();
        then(limitMoat0.rateLimiter().config().getLimitForPeriod()).isEqualTo(limitForPeriod);

        // Case2: DynamicConfig's limitForPeriod is null
        final RateLimitMoat limitMoat1 = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitForPeriod1-case2")),
                limitConfig, immutableConfig, Collections.emptyList());
        then(limitMoat1.rateLimiter().config().getLimitRefreshPeriod()).isNotEqualTo(limitForPeriod);
        limitMoat1.onUpdate(new ExternalConfig());
        then(limitMoat1.shouldDelete()).isFalse();
        then(limitMoat1.rateLimiter().config().getLimitForPeriod()).isEqualTo(limitForPeriod);

        // Case3: DynamicConfig's limitForPeriod has updated
        final int newestLimitForPeriod = RandomUtils.randomInt(20);
        final RateLimitMoat limitMoat2 = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitForPeriod1-case3")),
                limitConfig, immutableConfig, Collections.emptyList());
        then(limitMoat2.rateLimiter().config().getLimitRefreshPeriod()).isNotEqualTo(limitForPeriod);
        final ExternalConfig config2 = new ExternalConfig();
        config2.setLimitForPeriod(newestLimitForPeriod);
        limitMoat2.onUpdate(config2);
        then(limitMoat2.shouldDelete()).isFalse();
        then(limitMoat2.rateLimiter().config().getLimitForPeriod()).isEqualTo(newestLimitForPeriod);

        // Case4: DynamicConfig's limitForPeriod hasn't updated
        final RateLimitMoat limitMoat3 = new RateLimitMoat(new MoatConfig(ResourceId.from("rateLimitMoat-test4")),
                limitConfig, immutableConfig, Collections.emptyList());
        then(limitMoat3.rateLimiter().config().getLimitRefreshPeriod()).isNotEqualTo(limitForPeriod);
        final ExternalConfig config3 = new ExternalConfig();
        config3.setLimitForPeriod(this.limitForPeriod);
        limitMoat3.onUpdate(config3);
        then(limitMoat3.shouldDelete()).isFalse();
        then(limitMoat3.rateLimiter().config().getLimitForPeriod()).isEqualTo(this.limitForPeriod);
    }

    @Test
    void testUpdateLimitRefreshPeriod() throws InterruptedException {
        final RateLimitConfig config = RateLimitConfig.builder()
                .limitForPeriod(1).limitRefreshPeriod(Duration.ofMillis(10)).build();

        RateLimitMoat limitMoat = new RateLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateLimitRefreshPeriod")),
                config, config, Collections.emptyList());

        final ExternalConfig config1 = new ExternalConfig();
        config1.setLimitRefreshPeriod(Duration.ofSeconds(10L));
        limitMoat.onUpdate(config1);

        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + 20L);
        then(limitMoat.config().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(10L));

        config1.setLimitForPeriod(20);
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    limitMoat.onUpdate(config1);
                } catch (Throwable th) {
                    fail();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }
}
