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
package io.esastack.servicekeeper.configsource.core;

import io.esastack.servicekeeper.configsource.cache.ConfigCache;
import io.esastack.servicekeeper.configsource.cache.ConfigCacheImp;
import io.esastack.servicekeeper.configsource.utils.RandomUtils;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.ExternalGroupConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseConfigSourceTest {

    private final ConfigCache configCache = new ConfigCacheImp();

    private final BaseConfigSource configSource = new BaseConfigSource(configCache) {
    };

    @Test
    void testConstruct() {
        assertThrows(NullPointerException.class, () -> new BaseConfigSource(null) {
        });
    }

    @Test
    void testInit() {
        final ResourceId resourceId = ResourceId.from("testInit");
        final GroupResourceId groupId = GroupResourceId.from("testInit");
        then(configSource.config(resourceId)).isNull();
        then(configSource.all()).isEmpty();
        then(configSource.maxSizeLimit(new ArgConfigKey(resourceId, "arg0",
                RATE_LIMIT))).isNull();
        then(configSource.allGroups()).isEmpty();
        then(configSource.config(groupId)).isNull();
        then(configSource.mappingGroupId(groupId)).isNull();
        then(configSource.mappingResourceIds(groupId)).isEmpty();
    }

    @Test
    void testAfterUpdate() throws InterruptedException {
        final ResourceId resourceId = ResourceId.from("testAfterUpdate");
        final GroupResourceId groupId = GroupResourceId.from("testAfterUpdate");
        final String argName = "arg0";

        final Map<ArgConfigKey, Integer> maxSizeLimitMap = new HashMap<>(3);
        maxSizeLimitMap.put(new ArgConfigKey(resourceId, argName, RATE_LIMIT), 200);
        maxSizeLimitMap.put(new ArgConfigKey(resourceId, argName, CIRCUIT_BREAKER), 300);
        maxSizeLimitMap.put(new ArgConfigKey(resourceId, argName, CONCURRENT_LIMIT), 400);

        final Map<ResourceId, ExternalConfig> configMap = new HashMap<>(7);
        final ExternalGroupConfig groupConfig = new ExternalGroupConfig();
        final Set<ResourceId> items = new HashSet<>();
        items.add(resourceId);
        items.add(ResourceId.from("testAfterUpdate1"));
        groupConfig.setItems(items);

        configMap.put(resourceId, new ExternalConfig());
        configMap.put(groupId, groupConfig);
        configMap.put(GroupResourceId.from("testAfterUpdate1"), new ExternalConfig());

        final ExternalConfig config0 = new ExternalConfig();
        config0.setFailureRateThreshold(RandomUtils.randomFloat(100));
        config0.setMaxConcurrentLimit(RandomUtils.randomInt(1000));
        config0.setLimitForPeriod(RandomUtils.randomInt(200));
        configMap.put(new ArgResourceId(resourceId, argName, "A"), config0);

        final ExternalConfig config1 = new ExternalConfig();
        config1.setFailureRateThreshold(RandomUtils.randomFloat(100));
        configMap.put(new ArgResourceId(resourceId, argName, "B"), config1);

        final ExternalConfig config2 = new ExternalConfig();
        config2.setMaxConcurrentLimit(RandomUtils.randomInt(1000));
        configMap.put(new ArgResourceId(resourceId, argName, "C"), config2);

        final ExternalConfig config3 = new ExternalConfig();
        config3.setLimitForPeriod(RandomUtils.randomInt(200));
        configMap.put(new ArgResourceId(resourceId, argName, "D"), config3);


        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    configCache.updateConfigs(configMap);
                    configCache.updateMaxSizeLimits(maxSizeLimitMap);
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        then(configSource.maxSizeLimit(new ArgConfigKey(resourceId, argName, RATE_LIMIT))).isEqualTo(200);
        then(configSource.maxSizeLimit(new ArgConfigKey(resourceId, argName, CIRCUIT_BREAKER))).isEqualTo(300);
        then(configSource.maxSizeLimit(new ArgConfigKey(resourceId, argName, CONCURRENT_LIMIT))).isEqualTo(400);

        then(configSource.config(groupId)).isNotNull();
        then(configSource.allGroups().size()).isEqualTo(2);

        then((configSource.mappingResourceIds(groupId)).size()).isEqualTo(2);
        then((configSource.mappingResourceIds(groupId)).contains(resourceId)).isTrue();
        then((configSource.mappingResourceIds(groupId)).contains(ResourceId.from("testAfterUpdate1"))).isTrue();

        then(configSource.mappingGroupId(resourceId)).isEqualTo(groupId);
    }

}
