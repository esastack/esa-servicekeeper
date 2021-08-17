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

import esa.commons.Checks;
import esa.servicekeeper.configsource.cache.ConfigCache;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;
import esa.servicekeeper.core.configsource.MoatLimitConfigSource;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static esa.servicekeeper.core.configsource.ExternalConfigUtils.getDynamicString;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.isDynamicEquals;

public class ConfigsHandlerImpl implements ConfigsHandler {

    private static final Logger logger = LogUtils.logger();

    private final ConfigCache cache;
    protected final InternalsUpdater updater;

    public ConfigsHandlerImpl(ConfigCache cache, InternalsUpdater updater) {
        Checks.checkNotNull(cache, "cache");
        Checks.checkNotNull(updater, "updater");
        this.cache = cache;
        this.updater = updater;
    }

    @Override
    public void update(Map<ResourceId, ExternalConfig> configs) {
        final Map<ResourceId, ExternalConfig> oldConfigs = new HashMap<>(cache.configs());
        final Map<ResourceId, ExternalConfig> newConfigs = new HashMap<>(configs);

        if (logger.isDebugEnabled()) {
            logger.debug("The old configs are: " + LogUtils.concatValue(oldConfigs));
        }
        logger.info("The newest configs are: " + LogUtils.concatValue(newConfigs));

        // Detect old configs' copy excludes group config
        final Map<GroupResourceId, ExternalGroupConfig> oldGroupConfigs = new HashMap<>(8);
        for (Map.Entry<ResourceId, ExternalConfig> entry : oldConfigs.entrySet()) {
            if (entry.getKey() != null && entry.getKey() instanceof GroupResourceId) {
                oldGroupConfigs.putIfAbsent((GroupResourceId) entry.getKey(),
                        (ExternalGroupConfig) entry.getValue());
            }
        }

        for (Map.Entry<GroupResourceId, ExternalGroupConfig> entry : oldGroupConfigs.entrySet()) {
            oldConfigs.remove(entry.getKey());
        }

        // handle group config
        updateGroupConfig(oldGroupConfigs, newConfigs);

        // update value of "*" config
        updateMatchAllConfig(oldConfigs, newConfigs);

        // handle moat config
        updateMoatConfig(oldConfigs, newConfigs);
        cache.updateConfigs(configs);
    }

    private void updateGroupConfig(final Map<GroupResourceId, ExternalGroupConfig> oldGroupConfigs,
                                   final Map<ResourceId, ExternalConfig> newConfigs) {
        final Set<ResourceId> groupIdsToRemove = new HashSet<>(8);
        for (Map.Entry<ResourceId, ExternalConfig> entry : newConfigs.entrySet()) {
            ResourceId resourceId = entry.getKey();
            ExternalConfig newConfig = entry.getValue();
            if (!(resourceId instanceof GroupResourceId)) {
                continue;
            }
            ExternalConfig oldConfig = oldGroupConfigs.remove(resourceId);
            if (!isDynamicEquals(newConfig, oldConfig)) {
                logger.info("The {}'s group dynamic config has updated, the newest value: {}", resourceId.getName(),
                        getDynamicString(newConfig));
                doUpdate(resourceId, newConfig);
            }
            groupIdsToRemove.add(resourceId);
        }

        // Remove all groupConfigs from newConfigsMap
        groupIdsToRemove.forEach(newConfigs::remove);

        // All ExternalConfigs which has been deleted
        oldGroupConfigs.forEach((key, value) -> {
            logger.info("The " + key.getName() + "'s group dynamic config has updated, the newest value: null");
            doUpdate(key, null);
        });
    }

    private void updateMatchAllConfig(final Map<ResourceId, ExternalConfig> oldMoatConfigs,
                                      final Map<ResourceId, ExternalConfig> newConfigs) {
        for (Map.Entry<ResourceId, ExternalConfig> entry : newConfigs.entrySet()) {
            final ResourceId resourceId = entry.getKey();
            if (!isMatchAllArgId(entry.getKey())) {
                continue;
            }

            final ExternalConfig newConfig = entry.getValue();
            final ExternalConfig oldConfig = oldMoatConfigs.remove(resourceId);
            if (!isDynamicEquals(newConfig, oldConfig)) {
                logger.info("The {}'s dynamic config has updated, the newest value: {}", resourceId.getName(),
                        getDynamicString(newConfig));
                doUpdateMatchAllConfig((ArgResourceId) resourceId, entry.getValue(), newConfigs);
            }
        }

        oldMoatConfigs.forEach((key, value) -> {
            if (isMatchAllArgId(key)) {
                logger.info("The " + key.getName() + "'s dynamic config has updated, the newest value: null");
                doUpdateMatchAllConfig((ArgResourceId) key, null, newConfigs);
            }
        });
    }

    private void updateMoatConfig(final Map<ResourceId, ExternalConfig> oldMoatConfigs,
                                  final Map<ResourceId, ExternalConfig> newConfigs) {
        for (Map.Entry<ResourceId, ExternalConfig> entry : newConfigs.entrySet()) {
            final ResourceId resourceId = entry.getKey();
            final ExternalConfig newConfig = entry.getValue();
            final ExternalConfig oldConfig = oldMoatConfigs.remove(resourceId);
            if (!isDynamicEquals(newConfig, oldConfig)) {
                logger.info("The {}'s dynamic config has updated, the newest value: {}", resourceId.getName(),
                        getDynamicString(newConfig));
                doUpdate(resourceId, newConfig);
            }
        }

        // All ExternalConfigs which has been deleted
        oldMoatConfigs.forEach((key, value) -> {
            logger.info("The " + key.getName() + "'s dynamic config has updated, the newest value: null");
            doUpdate(key, null);
        });
    }

    private boolean isMatchAllArgId(final ResourceId id) {
        return id instanceof ArgResourceId
                && MoatLimitConfigSource.VALUE_MATCH_ALL.equals(((ArgResourceId) id).getArgValue());
    }

    private void doUpdateMatchAllConfig(final ArgResourceId argId,
                                        final ExternalConfig matchAllConfig,
                                        final Map<ResourceId, ExternalConfig> newConfigs) {
        final Map<Object, ExternalConfig> valueToConfigs = new HashMap<>(8);
        newConfigs.forEach((id, config) -> {
            if (id instanceof ArgResourceId) {
                final ArgResourceId id0 = (ArgResourceId) id;
                if (id0.getMethodAndArgId().equals(argId.getMethodAndArgId())) {
                    valueToConfigs.putIfAbsent(id0.getArgValue(), config);
                }
            }
        });

        updater.updateMatchAllConfig(argId.getMethodAndArgId(), matchAllConfig, valueToConfigs.keySet());
    }

    /**
     * Do update with specified {@link ResourceId} and {@link ExternalConfig}.
     *
     * @param resourceId resourceId
     * @param config     config
     */
    protected void doUpdate(final ResourceId resourceId, final ExternalConfig config) {
        updater.update(resourceId, config);
    }

    /**
     * Obtains {@link ConfigCache} to use.
     *
     * @return cache
     */
    protected ConfigCache getCache() {
        return cache;
    }
}
