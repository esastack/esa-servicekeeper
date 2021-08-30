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

import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;
import static java.util.Collections.unmodifiableMap;

public class ConfigCacheImp implements ConfigCache {

    private static final Logger logger = LogUtils.logger();

    private final Map<ResourceId, ExternalConfig> configs = new ConcurrentHashMap<>(64);

    private final Map<ArgConfigKey, Integer> maxSizeLimits = new ConcurrentHashMap<>(1);

    @Override
    public ExternalConfig configOf(ResourceId resourceId) {
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
    public Integer maxSizeLimitOf(ArgConfigKey key) {
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
    public Map<ResourceId, ExternalConfig> configs() {
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
    public Map<ArgConfigKey, Integer> maxSizeLimits() {
        return unmodifiableMap(this.maxSizeLimits);
    }

}
