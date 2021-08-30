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
package io.esastack.servicekeeper.configsource.cache;

import io.esastack.servicekeeper.configsource.utils.RandomUtils;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class RegexConfigCenterTest {

    private final RegexConfigCenter<ExternalConfig, ResourceId> regexConfigs = new RegexConfigCenter<>();
    private final RegexConfigCenter<Integer, ArgConfigKey> regexMaxSizeLimits = new RegexConfigCenter<>();

    @Test
    void testAddRegexConfig() throws InterruptedException {
        final String regex = "abc.def.*";

        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    regexConfigs.addRegexConfig(regex, new ExternalConfig());
                    regexMaxSizeLimits.addRegexConfig(regex, 100);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);

        then(regexConfigs.getAll().size()).isEqualTo(1);
        then(regexConfigs.getAll().get(regex).config()).isEqualTo(new ExternalConfig());
        then(regexConfigs.getAll().get(regex).items()).isEmpty();

        then(regexMaxSizeLimits.getAll().size()).isEqualTo(1);
        then(regexMaxSizeLimits.getAll().get(regex).config()).isEqualTo(100);
        then(regexMaxSizeLimits.getAll().get(regex).items()).isEmpty();
    }

    @Test
    void testConfig() throws InterruptedException {
        final String regex = "a.b.*";

        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    then(regexConfigs.configOf(ResourceId.from("a.b.c"))).isNotNull();
                    then(regexConfigs.configOf(ResourceId.from("a.b.d"))).isNotNull();

                    then(regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                            RATE_LIMIT))).isEqualTo(100);
                    then(regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                            RATE_LIMIT))).isEqualTo(100);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
    }

    @Test
    void testItems() throws InterruptedException {
        final String regex = "a.b.*";

        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    regexConfigs.configOf(ResourceId.from("a.b.c"));
                    regexConfigs.configOf(ResourceId.from("a.b.d"));
                    then(regexConfigs.items(regex).size()).isEqualTo(2);
                    then(regexConfigs.items(regex).contains(ResourceId.from("a.b.c"))).isTrue();
                    then(regexConfigs.items(regex).contains(ResourceId.from("a.b.d"))).isTrue();

                    regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                            RATE_LIMIT));
                    regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                            RATE_LIMIT));
                    then(regexMaxSizeLimits.items(regex).size()).isEqualTo(2);
                    then(regexMaxSizeLimits.items(regex).contains(
                            new ArgConfigKey(ResourceId.from("a.b.c"), "", RATE_LIMIT))).isTrue();
                    then(regexMaxSizeLimits.items(regex).contains(
                            new ArgConfigKey(ResourceId.from("a.b.d"), "", RATE_LIMIT))).isTrue();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

    @Test
    void testRemoveRegex() {
        final String regex = "a.b.*";

        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);

        regexConfigs.removeRegex(null);
        RegexValue<ExternalConfig, ResourceId> configValue = regexConfigs
                .removeRegex(regex);

        assert (configValue != null);
        then(configValue.items()).isEmpty();
        then(configValue.config()).isEqualTo(new ExternalConfig());
        then(regexConfigs.getAll()).isEmpty();

        regexMaxSizeLimits.removeRegex(null);
        RegexValue<Integer, ArgConfigKey> argValue = regexMaxSizeLimits
                .removeRegex(regex);

        assert (argValue != null);
        then(argValue.items()).isEmpty();
        then(argValue.config()).isEqualTo(100);
        then(regexMaxSizeLimits.getAll()).isEmpty();
    }

    @Test
    void testUpdateRegexConfig() throws InterruptedException {
        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        regexConfigs.addRegexConfig("a.b.*", new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig("a.b.*", 100);
        regexConfigs.configOf(ResourceId.from("a.b.c"));
        regexConfigs.configOf(ResourceId.from("a.b.d"));
        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                RATE_LIMIT));
        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                RATE_LIMIT));

        final Map<String, ExternalConfig> externalConfigMap = new HashMap<>(2);
        externalConfigMap.put("d.e.*", new ExternalConfig());
        externalConfigMap.put("x.y.*", new ExternalConfig());
        externalConfigMap.put("a.b.*", config);

        final Map<String, Integer> limitConfigMap = new HashMap<>(2);
        limitConfigMap.put("d.e.*", RandomUtils.randomInt(200));
        limitConfigMap.put("x.y.*", RandomUtils.randomInt(300));
        limitConfigMap.put("a.b.*", RandomUtils.randomInt(100));

        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    regexConfigs.updateRegexConfigs(externalConfigMap);
                    regexMaxSizeLimits.updateRegexConfigs(limitConfigMap);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        then(regexConfigs.getAll().size()).isEqualTo(3);
        then(regexConfigs.items("a.b.*").size()).isEqualTo(2);
        then(regexConfigs.configOf(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(regexConfigs.configOf(ResourceId.from("a.b.d"))).isEqualTo(config);

        then(regexMaxSizeLimits.getAll().size()).isEqualTo(3);
        then(regexMaxSizeLimits.items("a.b.*").size()).isEqualTo(2);
    }

    @Test
    void testParallel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(10);
        final AtomicInteger count = new AtomicInteger();

        final AtomicInteger exceptionCount = new AtomicInteger();
        for (; count.get() < 10; count.incrementAndGet()) {
            new Thread(() -> {
                try {
                    if (count.get() % 5 == 0) {
                        regexConfigs.addRegexConfig("abc.def.*", new ExternalConfig());
                        regexMaxSizeLimits.addRegexConfig("abc.def.*", 100);
                    } else if (count.get() % 5 == 1) {
                        regexConfigs.configOf(ResourceId.from("a.b.c"));
                        regexConfigs.configOf(ResourceId.from("a.b.d"));

                        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                                RATE_LIMIT));
                        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                                RATE_LIMIT));
                    } else if (count.get() % 5 == 2) {
                        regexConfigs
                                .removeRegex("a.b.*");
                        regexMaxSizeLimits
                                .removeRegex("a.b.*");
                    } else if (count.get() % 5 == 3) {
                        regexConfigs.updateRegexConfigs(null);
                        regexMaxSizeLimits.updateRegexConfigs(null);
                    } else {
                        regexConfigs.configOf(ResourceId.from("a.b.c"));
                        regexConfigs.configOf(ResourceId.from("a.b.d"));

                        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                                RATE_LIMIT));
                        regexMaxSizeLimits.configOf(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                                RATE_LIMIT));
                    }
                } catch (Throwable th) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        then(exceptionCount.get()).isEqualTo(0);
    }

}
