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
import esa.servicekeeper.configsource.cache.ConfigCache;
import esa.servicekeeper.configsource.file.constant.PropertyFileConstant;
import esa.servicekeeper.configsource.file.utils.PropertiesUtils;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * The base collection to hold the newest external config and global config. This class is designed as singleton, and
 * the {@link PropertyFileConfigSource} gets the newest config from it and the {@link PropertyFileUpdater} updates
 * it with newest all usually.
 */
class PropertyFileConfigCache implements ConfigCache {

    private static final Logger logger = LogUtils.logger();

    private final ConfigCache cache;

    PropertyFileConfigCache(ConfigCache cache) {
        Checks.checkNotNull(cache, "cache");
        this.cache = cache;
        doInit();
    }

    @Override
    public ExternalConfig configOf(ResourceId resourceId) {
        return cache.configOf(resourceId);
    }

    @Override
    public void updateConfig(ResourceId resourceId, ExternalConfig config) {
        cache.updateConfig(resourceId, config);
    }

    @Override
    public Integer maxSizeLimitOf(ArgConfigKey key) {
        return cache.maxSizeLimitOf(key);
    }

    @Override
    public void updateMaxSizeLimit(ArgConfigKey key, Integer maxSizeLimit) {
        cache.updateMaxSizeLimit(key, maxSizeLimit);
    }

    @Override
    public void updateConfigs(Map<ResourceId, ExternalConfig> configs) {
        cache.updateConfigs(configs);
    }

    @Override
    public Map<ResourceId, ExternalConfig> configs() {
        return cache.configs();
    }

    @Override
    public void updateMaxSizeLimits(Map<ArgConfigKey, Integer> maxSizeLimits) {
        cache.updateMaxSizeLimits(maxSizeLimits);
    }

    @Override
    public Map<ArgConfigKey, Integer> maxSizeLimits() {
        return cache.maxSizeLimits();
    }

    private void doInit() {
        final PropertyItem item = loadProperties();
        Map<ResourceId, ExternalConfig> configs = PropertiesUtils.configs(item.properties);
        logger.info("ServiceKeeper initial configuration map from file " + item.file.getAbsolutePath() +
                " are: " + LogUtils.concatValue(configs));
        cache.updateConfigs(configs);
        cache.updateMaxSizeLimits(PropertiesUtils.maxSizeLimits(item.properties));
    }

    private PropertyItem loadProperties() {
        final Properties properties = new Properties();
        final String configDir = PropertyFileConstant.configDir();
        // Gets the config from file if it exists.
        final File file = new File(configDir, PropertyFileConstant.configName());
        if (!file.exists()) {
            return new PropertyItem(file, properties);
        }

        try {
            properties.load(new FileInputStream(file));
        } catch (IOException ex) {
            logger.error("Failed to get service keeper's initial configuration from {}", file.getAbsolutePath(), ex);
            return new PropertyItem(file, properties);
        }
        return new PropertyItem(file, properties);
    }

    private static class PropertyItem {
        private final File file;
        private final Properties properties;

        private PropertyItem(File file, Properties properties) {
            this.file = file;
            this.properties = properties;
        }
    }
}
