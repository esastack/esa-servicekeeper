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
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;

import java.util.Map;

/**
 * Loads the {@link ExternalConfig} from file or sailor or others source and then cache them in memory.
 */
public interface ConfigCache {

    /**
     * Obtains {@link ExternalConfig} of {@link ResourceId}.
     *
     * @param resourceId resourceId
     * @return config
     */
    ExternalConfig getConfig(ResourceId resourceId);

    /**
     * Update {@link ExternalConfig} of {@link ResourceId}.
     *
     * @param resourceId resourceId
     * @param config     config
     */
    void updateConfig(ResourceId resourceId, ExternalConfig config);

    /**
     * Obtains max size limit of {@link ArgConfigKey}.
     *
     * @param key key
     * @return max size limit
     */
    Integer getMaxSizeLimit(ArgConfigKey key);

    /**
     * Updates maxSizeLimit of {@link ArgConfigKey}.
     *
     * @param key   key
     * @param maxSizeLimit  max size limit
     */
    void updateMaxSizeLimit(ArgConfigKey key, Integer maxSizeLimit);

    /**
     * Updates configs.
     *
     * @param configs configs
     */
    void updateConfigs(Map<ResourceId, ExternalConfig> configs);

    /**
     * Obtains configs which is a copy of current configs.
     *
     * @return configs
     */
    Map<ResourceId, ExternalConfig> getConfigs();

    /**
     * Updates maxSizeLimits.
     *
     * @param maxSizeLimits     maxSizeLimits
     */
    void updateMaxSizeLimits(Map<ArgConfigKey, Integer> maxSizeLimits);

    /**
     * Obtains maxSizeLimits.
     *
     * @return maxSizeLimits
     */
    Map<ArgConfigKey, Integer> getMaxSizeLimits();
}
