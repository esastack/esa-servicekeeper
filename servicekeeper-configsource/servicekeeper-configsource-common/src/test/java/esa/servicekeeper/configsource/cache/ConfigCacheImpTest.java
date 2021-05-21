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

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class ConfigCacheImpTest {

    @Test
    void testConfigOf() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.configOf(ResourceId.from("testConfigOf"))).isNull();
    }

    @Test
    void testMaxSizeLimitOf() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.maxSizeLimitOf(new ArgConfigKey(ResourceId.from("testMaxSizeLimitOf"),
                "arg0", RATE_LIMIT))).isNull();
    }

    @Test
    void testUpdateConfig() {
        final ConfigCache cache = new ConfigCacheImp();
        final ResourceId resourceId = ResourceId.from("testUpdateConfig");
        cache.updateConfig(resourceId, new ExternalConfig());

        then(cache.configOf(resourceId)).isNotNull();
        cache.updateConfig(resourceId, null);
        then(cache.configOf(resourceId)).isNull();
    }

    @Test
    void testUpdateMaxSizeLimit() {
        final ConfigCache cache = new ConfigCacheImp();
        final ArgConfigKey key = new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimit"),
                "arg0", RATE_LIMIT);
        final int maxSizeLimit = RandomUtils.randomInt(500);
        cache.updateMaxSizeLimit(key, maxSizeLimit);
        then(cache.maxSizeLimitOf(key)).isEqualTo(maxSizeLimit);

        cache.updateMaxSizeLimit(key, null);
        then(cache.maxSizeLimitOf(key)).isNull();
    }

    @Test
    void testConfigs() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.configs()).isEmpty();

        final ResourceId resourceId = ResourceId.from("testConfigs");
        cache.updateConfig(resourceId, new ExternalConfig());

        final Map<ResourceId, ExternalConfig> result = cache.configs();
        then(result.size()).isEqualTo(1);

        cache.updateConfig(resourceId, null);
        then(result.size()).isEqualTo(0);
    }

    @Test
    void testUpdateConfigs() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.configs()).isEmpty();

        Map<ResourceId, ExternalConfig> configs = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            configs.put(ResourceId.from("testUpdateConfigs" + i), new ExternalConfig());
        }
        cache.updateConfigs(configs);
        then(cache.configs().size()).isEqualTo(10);

        cache.updateConfigs(null);
        then(cache.configs()).isEmpty();
    }

    @Test
    void testMaxSizeLimitsOf() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.maxSizeLimits()).isEmpty();

        final ArgConfigKey key = new ArgConfigKey(ResourceId.from("testConfigs"), "arg0", RATE_LIMIT);
        cache.updateMaxSizeLimit(key, RandomUtils.randomInt(200));

        final Map<ArgConfigKey, Integer> result = cache.maxSizeLimits();
        then(result.size()).isEqualTo(1);

        cache.updateMaxSizeLimit(key, null);
        then(result.size()).isEqualTo(0);
    }

    @Test
    void testUpdateMaxSizeLimits() {
        final ConfigCache cache = new ConfigCacheImp();
        then(cache.maxSizeLimits()).isEmpty();

        Map<ArgConfigKey, Integer> configs = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            configs.put(new ArgConfigKey(ResourceId.from("testUpdateConfigs" + i),
                    "arg" + i, CIRCUIT_BREAKER), RandomUtils.randomInt(300));
        }
        cache.updateMaxSizeLimits(configs);
        then(cache.maxSizeLimits().size()).isEqualTo(10);

        cache.updateMaxSizeLimits(null);
        then(cache.maxSizeLimits()).isEmpty();
    }

    @Test
    void testParallel() throws InterruptedException {
        final ConfigCache cache = new ConfigCacheImp();

        final AtomicInteger exceptionCount = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            final ResourceId resourceId = ResourceId.from("testParallel" + i);
            final ArgConfigKey key = new ArgConfigKey(resourceId, "arg" + i, CIRCUIT_BREAKER);

            new Thread(() -> {
                try {
                    cache.updateMaxSizeLimit(key, null);
                    cache.updateMaxSizeLimit(key, RandomUtils.randomInt(200));
                    cache.updateConfig(resourceId, null);
                    cache.updateConfig(resourceId, new ExternalConfig());
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
