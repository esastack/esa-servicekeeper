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

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;
import static java.util.Collections.unmodifiableMap;

public class ConfigCacheImp implements ConfigCache {

    private static final Logger logger = LogUtils.logger();

    private final Map<ResourceId, ExternalConfig> configs = new ConcurrentHashMap<>(64);

    private final Map<ArgConfigKey, Integer> maxSizeLimits = new ConcurrentHashMap<>(1);

    @Override
    public ExternalConfig getConfig(ResourceId resourceId) {
        final ExternalConfig config = configs.get(resourceId);
        if (config != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}'s config: {}", resourceId, config);
            }
            return config;
        }

        // fallback to get arg config which value is *
        if (resourceId instanceof ArgResourceId) {
            final ArgResourceId argId = (ArgResourceId) resourceId;
            final ArgResourceId matchAllId = new ArgResourceId(argId.getMethodId(),
                    argId.getArgName(), VALUE_MATCH_ALL);

            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}'s config: {}", matchAllId, configs.get(matchAllId));
            }
            return configs.get(matchAllId);
        }

        return null;
    }

    @Override
    public void updateConfig(ResourceId resourceId, ExternalConfig config) {
        if (config == null) {
            configs.remove(resourceId);
            return;
        }
        configs.put(resourceId, config);
    }

    @Override
    public Integer getMaxSizeLimit(ArgConfigKey key) {
        final Integer maxSizeLimit = maxSizeLimits.get(key);
        if (logger.isDebugEnabled()) {
            logger.debug("Obtained {}'s maxSizeLimit: {}", key, maxSizeLimit);
        }
        return maxSizeLimit;
    }

    @Override
    public void updateMaxSizeLimit(ArgConfigKey key, Integer maxSizeLimit) {
        if (maxSizeLimit == null) {
            maxSizeLimits.remove(key);
            return;
        }
        maxSizeLimits.put(key, maxSizeLimit);
    }

    @Override
    public void updateConfigs(Map<ResourceId, ExternalConfig> configs) {
        this.configs.clear();
        if (configs != null) {
            this.configs.putAll(configs);
        }
    }

    @Override
    public Map<ResourceId, ExternalConfig> getConfigs() {
        return unmodifiableMap(this.configs);
    }

    @Override
    public void updateMaxSizeLimits(Map<ArgConfigKey, Integer> maxSizeLimits) {
        this.maxSizeLimits.clear();
        if (maxSizeLimits != null) {
            this.maxSizeLimits.putAll(maxSizeLimits);
        }
    }

    @Override
    public Map<ArgConfigKey, Integer> getMaxSizeLimits() {
        return unmodifiableMap(this.maxSizeLimits);
    }

}
