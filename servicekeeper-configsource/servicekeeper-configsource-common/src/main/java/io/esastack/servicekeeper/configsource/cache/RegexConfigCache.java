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

import esa.commons.annotation.Beta;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@link ConfigCache} which supports regular match for {@link ResourceId}.
 */
@Beta
public class RegexConfigCache extends ConfigCacheImp {

    private static final Logger logger = LogUtils.logger();

    private final RegexConfigCenter<ExternalConfig, ResourceId> regexConfigs = new RegexConfigCenter<>();

    @Override
    public ExternalConfig configOf(ResourceId resourceId) {
        final ExternalConfig config = super.configOf(resourceId);
        if (config != null) {
            return config;
        }
        return regexConfigs.configOf(resourceId);
    }

    @Override
    public void updateConfigs(Map<ResourceId, ExternalConfig> configs) {
        if (configs == null) {
            super.updateConfigs(null);
            this.regexConfigs.updateRegexConfigs(null);
            return;
        }

        final Map<ResourceId, ExternalConfig> regexConfigs = new HashMap<>(0);
        configs.forEach((id, config) -> {
            if (id.isRegex()) {
                regexConfigs.put(id, config);
            }
        });

        super.updateConfigs(configs);

        // Format regex configs
        final Map<String, ExternalConfig> newRegexConfigs = new HashMap<>(regexConfigs.size());
        regexConfigs.forEach((id, config) -> newRegexConfigs.put(id.getName(), config));

        logger.info("The newest regex configs are: {}", LogUtils.concatValue(newRegexConfigs));
        this.regexConfigs.updateRegexConfigs(newRegexConfigs);
    }

    @Override
    public void updateConfig(ResourceId resourceId, ExternalConfig config) {
        super.updateConfig(resourceId, config);

        if (resourceId.isRegex()) {
            this.regexConfigs.updateRegexConfig(resourceId.getName(), config);
        }
    }

    public RegexValue<ExternalConfig, ResourceId> regexConfigOf(String regex) {
        return regexConfigs.getAll().get(regex);
    }

    Map<String, RegexValue<ExternalConfig, ResourceId>> regexConfigs() {
        return regexConfigs.getAll();
    }

}
