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
package esa.servicekeeper.configsource.file;

import esa.commons.Checks;
import esa.servicekeeper.configsource.ConfigsHandler;
import esa.servicekeeper.configsource.InternalsUpdater;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

import java.util.HashMap;
import java.util.Map;

class InternalConfigsHandler implements ConfigsHandler {

    private static final Logger logger = LogUtils.logger();

    private final ConfigsHandler handler;
    private final InternalsUpdater updater;
    private final PropertyFileConfigCache cache;

    InternalConfigsHandler(ConfigsHandler handler, InternalsUpdater updater, PropertyFileConfigCache cache) {
        Checks.checkNotNull(handler, "handler");
        Checks.checkNotNull(updater, "updater");
        Checks.checkNotNull(cache, "cache");
        this.handler = handler;
        this.updater = updater;
        this.cache = cache;
    }

    @Override
    public void update(Map<ResourceId, ExternalConfig> configs) {
        handler.update(configs);
    }

    /**
     * Processes newest global config.
     *
     * @param globalDisable  global disable value
     * @param argLevelEnable arg level enable value
     * @param retryEnable    retry enable value
     */
    void updateGlobalConfigs(Boolean globalDisable, Boolean argLevelEnable, Boolean retryEnable) {
        logger.info("The newest global disable:{}, arg level enable:{}, retry enable:{}",
                globalDisable, argLevelEnable, retryEnable);

        updater.updateGlobalDisable(globalDisable);
        updater.updateArgLevelEnable(argLevelEnable);
        updater.updateRetryEnable(retryEnable);
    }

    /**
     * Processes newest max size limit.
     *
     * @param maxSizeLimits maxSizeLimits
     */
    void updateMaxSizeLimits(Map<ArgConfigKey, Integer> maxSizeLimits) {
        final Map<ArgConfigKey, Integer> oldConfigs = new HashMap<>(cache.maxSizeLimits());
        for (Map.Entry<ArgConfigKey, Integer> entry : maxSizeLimits.entrySet()) {
            updater.updateMaxSizeLimit(entry.getKey(), oldConfigs.get(entry.getKey()), entry.getValue());
            oldConfigs.remove(entry.getKey());
        }

        for (Map.Entry<ArgConfigKey, Integer> entry : oldConfigs.entrySet()) {
            updater.updateMaxSizeLimit(entry.getKey(), entry.getValue(), null);
        }
        cache.updateMaxSizeLimits(maxSizeLimits);
    }
}
