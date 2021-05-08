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
package esa.servicekeeper.configsource;

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.moats.MoatCluster;

import java.util.Set;

public interface InternalsUpdater {

    /**
     * Update {@link ResourceId}'s {@link MoatCluster} with {@link ExternalConfig}.
     *
     * @param resourceId resourceId
     * @param config     config
     */
    void update(ResourceId resourceId, ExternalConfig config);

    /**
     * Update argConfigs with newest config and given values.
     *
     * @param methodAndArgId    methodAndArgId
     * @param config            config
     * @param values            values
     */
    void updateMatchAllConfig(ResourceId methodAndArgId, ExternalConfig config, Set<Object> values);

    /**
     * Update {@link ArgConfigKey}'s maxSizeLimit.
     *
     * @param key             key
     * @param oldMaxSizeLimit old maxSizeLimit
     * @param newMaxSizeLimit new maxSizeLimit
     */
    void updateMaxSizeLimit(ArgConfigKey key, Integer oldMaxSizeLimit, Integer newMaxSizeLimit);

    /**
     * Updates global disable value.
     *
     * @param globalDisable global disable value
     */
    void updateGlobalDisable(Boolean globalDisable);

    /**
     * Updates arg level enable.
     *
     * @param argLevelEnable arg level enable
     */
    void updateArgLevelEnable(Boolean argLevelEnable);

    /**
     * Updates retry enable.
     *
     * @param retryEnable retry enable
     */
    void updateRetryEnable(Boolean retryEnable);

}
