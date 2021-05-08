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
package esa.servicekeeper.configsource.cache;

import esa.servicekeeper.configsource.utils.RandomUtils;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
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
        then(regexConfigs.getAll().get(regex).getConfig()).isEqualTo(new ExternalConfig());
        then(regexConfigs.getAll().get(regex).getItems()).isEmpty();

        then(regexMaxSizeLimits.getAll().size()).isEqualTo(1);
        then(regexMaxSizeLimits.getAll().get(regex).getConfig()).isEqualTo(100);
        then(regexMaxSizeLimits.getAll().get(regex).getItems()).isEmpty();
    }

    @Test
    void testGetConfig() throws InterruptedException {
        final String regex = "a.b.*";

        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    then(regexConfigs.getConfig(ResourceId.from("a.b.c"))).isNotNull();
                    then(regexConfigs.getConfig(ResourceId.from("a.b.d"))).isNotNull();

                    then(regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                            RATE_LIMIT))).isEqualTo(100);
                    then(regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                            RATE_LIMIT))).isEqualTo(100);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
    }

    @Test
    void testGetItems() throws InterruptedException {
        final String regex = "a.b.*";

        regexConfigs.addRegexConfig(regex, new ExternalConfig());
        regexMaxSizeLimits.addRegexConfig(regex, 100);
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    regexConfigs.getConfig(ResourceId.from("a.b.c"));
                    regexConfigs.getConfig(ResourceId.from("a.b.d"));
                    then(regexConfigs.getItems(regex).size()).isEqualTo(2);
                    then(regexConfigs.getItems(regex).contains(ResourceId.from("a.b.c")));
                    then(regexConfigs.getItems(regex).contains(ResourceId.from("a.b.d")));

                    regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                            RATE_LIMIT));
                    regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.d"), "",
                            RATE_LIMIT));
                    then(regexMaxSizeLimits.getItems(regex).size()).isEqualTo(2);
                    then(regexMaxSizeLimits.getItems(regex).contains(
                            new ArgConfigKey(ResourceId.from("a.b.c"), "", RATE_LIMIT)));
                    then(regexMaxSizeLimits.getItems(regex).contains(
                            new ArgConfigKey(ResourceId.from("a.b.d"), "", RATE_LIMIT)));
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
        then(configValue.getItems()).isEmpty();
        then(configValue.getConfig()).isEqualTo(new ExternalConfig());
        then(regexConfigs.getAll()).isEmpty();

        regexMaxSizeLimits.removeRegex(null);
        RegexValue<Integer, ArgConfigKey> argValue = regexMaxSizeLimits
                .removeRegex(regex);

        assert (argValue != null);
        then(argValue.getItems()).isEmpty();
        then(argValue.getConfig()).isEqualTo(100);
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
        regexConfigs.getConfig(ResourceId.from("a.b.c"));
        regexConfigs.getConfig(ResourceId.from("a.b.d"));
        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                RATE_LIMIT));
        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.d"), "",
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
        then(regexConfigs.getItems("a.b.*").size()).isEqualTo(2);
        then(regexConfigs.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(regexConfigs.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);

        then(regexMaxSizeLimits.getAll().size()).isEqualTo(3);
        then(regexMaxSizeLimits.getItems("a.b.*").size()).isEqualTo(2);
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
                        regexConfigs.getConfig(ResourceId.from("a.b.c"));
                        regexConfigs.getConfig(ResourceId.from("a.b.d"));

                        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                                RATE_LIMIT));
                        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.d"), "",
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
                        regexConfigs.getConfig(ResourceId.from("a.b.c"));
                        regexConfigs.getConfig(ResourceId.from("a.b.d"));

                        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.c"), "",
                                RATE_LIMIT));
                        regexMaxSizeLimits.getConfig(new ArgConfigKey(ResourceId.from("a.b.d"), "",
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
