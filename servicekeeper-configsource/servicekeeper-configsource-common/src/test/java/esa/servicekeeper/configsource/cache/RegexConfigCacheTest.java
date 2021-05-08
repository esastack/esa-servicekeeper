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
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

class RegexConfigCacheTest {

    @Test
    void testGetConfig() {
        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        ConfigCache cache = build(config, ResourceId.from("a.b.*", true));

        then(cache.getConfig(ResourceId.from("d.e.f"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("x.y.z"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config);

        cache = build(config, ResourceId.from("a.b.*"));
        then(cache.getConfig(ResourceId.from("d.e.f"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("x.y.z"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("a.b.c"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.d"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.f"))).isNull();
    }

    @Test
    void testGetConfigs() {
        ConfigCache cache = new RegexConfigCache();
        then(cache.getConfigs()).isEmpty();

        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        final ResourceId resourceId = ResourceId.from("a.b.*", true);
        cache = build(config, resourceId);
        then(cache.getConfigs().size()).isEqualTo(3);
        then(cache.getConfig(resourceId)).isEqualTo(config);
    }

    @Test
    void testUpdateConfig() {
        // Add
        ConfigCache cache = new RegexConfigCache();

        final ResourceId resourceId = ResourceId.from("a.b.*", true);
        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        then(cache.getConfig(resourceId)).isNull();

        cache.updateConfig(resourceId, config);
        then(cache.getConfig(resourceId)).isEqualTo(config);

        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config);

        // Update
        final ExternalConfig config1 = new ExternalConfig();
        config1.setLimitForPeriod(RandomUtils.randomInt(200));
        config1.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config1.setFailureRateThreshold(RandomUtils.randomFloat(100));
        cache.updateConfig(resourceId, config1);

        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config1);

        // Remove
        cache.updateConfig(resourceId, null);
        then(cache.getConfig(ResourceId.from("a.b.c"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.d"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.f"))).isNull();
    }

    @Test
    void testUpdateConfigs() {
        // Add
        final RegexConfigCache cache = new RegexConfigCache();
        final ResourceId resourceId1 = ResourceId.from("a.b.*", true);
        final ResourceId resourceId2 = ResourceId.from("d.e.*", true);

        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        final Map<ResourceId, ExternalConfig> configs = new HashMap<>(2);
        configs.put(resourceId1, config);
        configs.put(resourceId2, config);
        then(cache.getConfigs()).isEmpty();
        then(cache.getRegexConfigs()).isEmpty();

        cache.updateConfigs(configs);
        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config);

        then(cache.getConfig(ResourceId.from("d.e.a"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("d.e.b"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("d.e.c"))).isEqualTo(config);

        then(cache.getConfigs().size()).isEqualTo(2);
        then(cache.getConfigs().get(resourceId1)).isEqualTo(config);
        then(cache.getConfigs().get(resourceId2)).isEqualTo(config);

        final Map<String, RegexValue<ExternalConfig, ResourceId>> regexValues = cache.getRegexConfigs();
        then(regexValues.get("a.b.*").getConfig()).isEqualTo(config);
        then(regexValues.get("a.b.*").getItems().contains(ResourceId.from("a.b.c"))).isTrue();
        then(regexValues.get("a.b.*").getItems().contains(ResourceId.from("a.b.d"))).isTrue();
        then(regexValues.get("a.b.*").getItems().contains(ResourceId.from("a.b.f"))).isTrue();

        then(regexValues.get("d.e.*").getConfig()).isEqualTo(config);
        then(regexValues.get("d.e.*").getItems().contains(ResourceId.from("d.e.a"))).isTrue();
        then(regexValues.get("d.e.*").getItems().contains(ResourceId.from("d.e.b"))).isTrue();
        then(regexValues.get("d.e.*").getItems().contains(ResourceId.from("d.e.c"))).isTrue();


        // Update
        final ExternalConfig config1 = new ExternalConfig();
        config1.setLimitForPeriod(RandomUtils.randomInt(200));
        config1.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config1.setFailureRateThreshold(RandomUtils.randomFloat(100));

        final Map<ResourceId, ExternalConfig> configs2 = new HashMap<>(2);
        configs2.put(resourceId1, config1);
        configs2.put(resourceId2, config1);
        cache.updateConfigs(configs2);

        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config1);

        then(cache.getConfig(ResourceId.from("d.e.a"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("d.e.b"))).isEqualTo(config1);
        then(cache.getConfig(ResourceId.from("d.e.c"))).isEqualTo(config1);

        then(cache.getConfigs().size()).isEqualTo(2);
        then(cache.getConfigs().get(resourceId1)).isEqualTo(config1);
        then(cache.getConfigs().get(resourceId2)).isEqualTo(config1);

        final Map<String, RegexValue<ExternalConfig, ResourceId>> regexValues1 = cache.getRegexConfigs();
        then(regexValues1.get("a.b.*").getConfig()).isEqualTo(config1);
        then(regexValues1.get("a.b.*").getItems().contains(ResourceId.from("a.b.c"))).isTrue();
        then(regexValues1.get("a.b.*").getItems().contains(ResourceId.from("a.b.d"))).isTrue();
        then(regexValues1.get("a.b.*").getItems().contains(ResourceId.from("a.b.f"))).isTrue();

        then(regexValues1.get("d.e.*").getConfig()).isEqualTo(config1);
        then(regexValues1.get("d.e.*").getItems().contains(ResourceId.from("d.e.a"))).isTrue();
        then(regexValues1.get("d.e.*").getItems().contains(ResourceId.from("d.e.b"))).isTrue();
        then(regexValues1.get("d.e.*").getItems().contains(ResourceId.from("d.e.c"))).isTrue();


        // Remove
        cache.updateConfigs(null);
        then(cache.getConfig(ResourceId.from("a.b.c"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.d"))).isNull();
        then(cache.getConfig(ResourceId.from("a.b.f"))).isNull();

        then(cache.getConfig(ResourceId.from("d.e.a"))).isNull();
        then(cache.getConfig(ResourceId.from("d.e.b"))).isNull();
        then(cache.getConfig(ResourceId.from("d.e.c"))).isNull();
    }

    @Test
    void testGetRegexConfig() {
        RegexConfigCache cache = new RegexConfigCache();
        then(cache.getRegexConfig("*")).isNull();


        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        cache = build(config, ResourceId.from("a.b.*", true));

        then(cache.getConfig(ResourceId.from("d.e.f"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("x.y.z"))).isEqualTo(new ExternalConfig());
        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config);

        then(cache.getRegexConfig("*")).isNull();
        final RegexValue<ExternalConfig, ResourceId> regexValue = cache.getRegexConfig("a.b.*");
        then(regexValue.getConfig()).isEqualTo(config);
        then(regexValue.getItems().size()).isEqualTo(3);
        then(regexValue.getItems().contains(ResourceId.from("a.b.c"))).isTrue();
        then(regexValue.getItems().contains(ResourceId.from("a.b.d"))).isTrue();
        then(regexValue.getItems().contains(ResourceId.from("a.b.f"))).isTrue();
    }

    @Test
    void testGetRegexConfigs() {
        RegexConfigCache cache = new RegexConfigCache();
        then(cache.getRegexConfigs()).isEmpty();


        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));

        cache = build(config, ResourceId.from("a.b.*", true));
        then(cache.getConfig(ResourceId.from("a.b.c"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.d"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("a.b.f"))).isEqualTo(config);

        cache.updateConfig(ResourceId.from("d.e.*", true), config);
        then(cache.getConfig(ResourceId.from("d.e.a"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("d.e.b"))).isEqualTo(config);
        then(cache.getConfig(ResourceId.from("d.e.c"))).isEqualTo(config);

        Map<String, RegexValue<ExternalConfig, ResourceId>> regexValues = cache.getRegexConfigs();
        then(regexValues.size()).isEqualTo(2);

        final RegexValue<ExternalConfig, ResourceId> regexValue0 = cache.getRegexConfig("a.b.*");
        then(regexValue0.getConfig()).isEqualTo(config);
        then(regexValue0.getItems().size()).isEqualTo(3);
        then(regexValue0.getItems().contains(ResourceId.from("a.b.c"))).isTrue();
        then(regexValue0.getItems().contains(ResourceId.from("a.b.d"))).isTrue();
        then(regexValue0.getItems().contains(ResourceId.from("a.b.f"))).isTrue();

        final RegexValue<ExternalConfig, ResourceId> regexValue1 = cache.getRegexConfig("d.e.*");
        then(regexValue1.getConfig()).isEqualTo(config);
        then(regexValue1.getItems().size()).isEqualTo(3);
        then(regexValue1.getItems().contains(ResourceId.from("d.e.a"))).isTrue();
        then(regexValue1.getItems().contains(ResourceId.from("d.e.b"))).isTrue();
        then(regexValue1.getItems().contains(ResourceId.from("d.e.c"))).isTrue();
    }

    private RegexConfigCache build(ExternalConfig config, final ResourceId resourceId) {
        final Map<ResourceId, ExternalConfig> configs = new HashMap<>(3);
        configs.put(ResourceId.from("d.e.f"), new ExternalConfig());
        configs.put(ResourceId.from("x.y.z"), new ExternalConfig());
        configs.put(resourceId, config);

        final RegexConfigCache cache = new RegexConfigCache();
        cache.updateConfigs(configs);
        return cache;
    }

}
