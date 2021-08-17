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
package esa.servicekeeper.core.moats.concurrentlimit;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.MoatEvent;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentLimitMoatTest {

    private final int maxConcurrentLimit = RandomUtils.randomInt(5);
    private final MoatConfig moatConfig = new MoatConfig(ResourceId.from("concurrentLimitMoat-test"));
    private final ConcurrentLimitConfig limitConfig = ConcurrentLimitConfig.builder()
            .threshold(maxConcurrentLimit).build();

    private ConcurrentLimitMoat limitMoat;

    @BeforeEach
    void setUp() {
        limitMoat = new ConcurrentLimitMoat(moatConfig, limitConfig, null, Collections.emptyList());
    }

    @Test
    void testTryThrough() {
        final MoatConfig moatConfig = new MoatConfig(ResourceId.from("testATryThrough"));
        final ConcurrentLimitMoat limitMoat = new ConcurrentLimitMoat(moatConfig, limitConfig,
                null,
                Collections.singletonList(new MoatEventProcessor() {
                    @Override
                    public void process(String name, MoatEvent event) {

                    }
                }));

        for (int i = 0; i < maxConcurrentLimit; i++) {
            assertDoesNotThrow(() -> limitMoat.enter(null));
        }
        assertThrows(ConcurrentOverFlowException.class, () -> limitMoat.enter(null));
        for (int i = 0; i < maxConcurrentLimit; i++) {
            limitMoat.exit(null);
        }
        assertDoesNotThrow(() -> limitMoat.enter(null));
        limitMoat.exit(null);
    }

    @Test
    void testToString() {
        then(limitMoat.toString()).isEqualTo("ConcurrentLimitMoat-concurrentLimitMoat-test");
        then(limitMoat.name()).isEqualTo("concurrentLimitMoat-test");
    }

    @Test
    void testGetLifeCycleType() {
        then(limitMoat.lifeCycleType()).isEqualTo(LifeCycleSupport.LifeCycleType.TEMPORARY);
    }

    @Test
    void testGetFondConfig() {
        then(limitMoat.getFond(null)).isNull();

        ExternalConfig config = new ExternalConfig();
        then(limitMoat.getFond(config)).isNull();

        config.setMaxConcurrentLimit(5);
        then(limitMoat.getFond(config)).isNotNull();
    }

    @Test
    void testUpdateWhenFondConfigIsNull() {
        // When the immutable config is null(Current limiter's immutable config is null)
        limitMoat.updateWhenNewestConfigIsNull();
        then(limitMoat.shouldDelete()).isTrue();

        // When the immutable config is not null
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        ConcurrentLimitConfig immutableConfig = ConcurrentLimitConfig.builder()
                .threshold(maxConcurrentLimit).build();
        final ConcurrentLimitMoat limitMoat = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateWhenFondConfigIsNull")),
                limitConfig, immutableConfig, Collections.emptyList());
        limitMoat.updateWhenNewestConfigIsNull();
        then(limitMoat.shouldDelete()).isFalse();
        for (int i = 0; i < maxConcurrentLimit; i++) {
            assertDoesNotThrow(() -> limitMoat.enter(null));
        }
        assertThrows(ConcurrentOverFlowException.class, () -> limitMoat.enter(null));
        for (int i = 0; i < maxConcurrentLimit; i++) {
            limitMoat.exit(null);
        }
    }

    @Test
    void testUpdateWithFondConfig() {
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(maxConcurrentLimit);
        limitMoat.updateWithNewestConfig(limitMoat.getFond(config));
        for (int i = 0; i < maxConcurrentLimit; i++) {
            assertDoesNotThrow(() -> limitMoat.enter(null));
        }
        for (int i = 0; i < maxConcurrentLimit; i++) {
            limitMoat.exit(null);
        }
    }

    @Test
    void testUpdateMaxConcurrentLimit0() throws InterruptedException {
        // Update maxConcurrentLimit when original immutable config is null.

        // Case1: DynamicConfig is null
        final ConcurrentLimitMoat limitMoat0 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit0-case1")),
                limitConfig, null, Collections.emptyList());
        final CountDownLatch latch0 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat0.onUpdate(null);
            } finally {
                latch0.countDown();
            }
        }).start();
        latch0.await();
        then(limitMoat0.shouldDelete()).isTrue();

        // Case2: DynamicConfig's maxConcurrentLimit is null
        final ConcurrentLimitMoat limitMoat1 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit0-case2")),
                limitConfig, null, Collections.emptyList());
        final CountDownLatch latch1 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat1.onUpdate(new ExternalConfig());
            } finally {
                latch1.countDown();
            }
        }).start();
        latch1.await();
        then(limitMoat1.shouldDelete()).isTrue();

        // Case3: DynamicConfig's maxConcurrentLimit has been updated
        final ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit0-case3")),
                limitConfig, null, Collections.emptyList());
        final ExternalConfig config = new ExternalConfig();
        final int newestMaxConcurrentLimit = RandomUtils.randomInt(5);
        config.setMaxConcurrentLimit(newestMaxConcurrentLimit);
        final CountDownLatch latch2 = new CountDownLatch(1);
        new Thread(() -> {
            try {
                limitMoat2.onUpdate(config);
            } finally {
                latch2.countDown();
            }
        }).start();
        latch2.await();
        then(limitMoat2.shouldDelete()).isFalse();
        then(limitMoat2.getConcurrentLimiter().immutableConfig()).isNull();
        then(limitMoat2.getConcurrentLimiter().metrics().threshold()).isEqualTo(newestMaxConcurrentLimit);
    }

    @Test
    void testUpdateMaxConcurrentLimit1() {
        // Update maxConcurrentLimit when original immutable config is not null.
        final int maxConcurrentLimit = RandomUtils.randomInt(5);
        final ConcurrentLimitConfig immutableConfig = ConcurrentLimitConfig.builder()
                .threshold(maxConcurrentLimit).build();

        // Case1: DynamicConfig is null.
        final ConcurrentLimitMoat limitMoat0 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit1-case1")),
                limitConfig, immutableConfig, Collections.emptyList());
        limitMoat0.onUpdate(null);
        then(limitMoat0.shouldDelete()).isFalse();
        then(limitMoat0.getConcurrentLimiter().metrics().threshold()).isEqualTo(maxConcurrentLimit);

        // Case2: DynamicConfig's maxConcurrentLimit is null.
        final ConcurrentLimitMoat limitMoat1 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit1-case2")),
                limitConfig, immutableConfig, Collections.emptyList());
        limitMoat1.onUpdate(new ExternalConfig());
        then(limitMoat1.shouldDelete()).isFalse();
        then(limitMoat1.getConcurrentLimiter().metrics().threshold()).isEqualTo(maxConcurrentLimit);

        // Case3: DynamicConfig's maxConcurrentLimit has updated
        final ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit1-case3")),
                limitConfig, immutableConfig, Collections.emptyList());
        final int newestMaxConcurrentLimit = RandomUtils.randomInt(5);
        final ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(newestMaxConcurrentLimit);
        limitMoat2.onUpdate(config);
        then(limitMoat2.shouldDelete()).isFalse();
        then(limitMoat2.getConcurrentLimiter().metrics().threshold()).isEqualTo(newestMaxConcurrentLimit);

        // Case4: DynamicConfig's maxConcurrentLimit hasn't updated
        final ConcurrentLimitMoat limitMoat3 = new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from("testUpdateMaxConcurrentLimit1-case4")),
                limitConfig, immutableConfig, Collections.emptyList());
        config.setMaxConcurrentLimit(this.maxConcurrentLimit);
        then(limitMoat3.isConfigEquals(limitMoat3.getFond(config))).isTrue();
        limitMoat3.onUpdate(config);
        then(limitMoat3.shouldDelete()).isFalse();
        then(limitMoat3.getConcurrentLimiter().metrics().threshold()).isEqualTo(this.maxConcurrentLimit);
    }

}
