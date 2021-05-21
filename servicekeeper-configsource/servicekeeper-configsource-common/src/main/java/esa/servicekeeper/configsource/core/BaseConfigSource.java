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
package esa.servicekeeper.configsource.core;

import esa.commons.Checks;
import esa.servicekeeper.configsource.cache.ConfigCache;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public abstract class BaseConfigSource implements CompositeConfigSource {

    private final ConfigCache configCache;

    public BaseConfigSource(ConfigCache configCache) {
        Checks.checkNotNull(configCache, "configCache");
        this.configCache = configCache;
    }

    @Override
    public ExternalConfig config(ResourceId id) {
        return configCache.configOf(id);
    }

    @Override
    public Map<ResourceId, ExternalConfig> all() {
        return unmodifiableMap(configCache.configs());
    }

    @Override
    public Map<GroupResourceId, ExternalConfig> allGroups() {
        final Map<ResourceId, ExternalConfig> configMap = all();
        if (configMap == null) {
            return Collections.emptyMap();
        }
        Map<GroupResourceId, ExternalConfig> groupConfigMap = new LinkedHashMap<>(configMap.size());
        configMap.forEach((key, value) -> {
            if (key instanceof GroupResourceId) {
                groupConfigMap.putIfAbsent((GroupResourceId) key, value);
            }
        });

        return unmodifiableMap(groupConfigMap);
    }

    @Override
    public ExternalConfig config(GroupResourceId groupId) {
        final Map<GroupResourceId, ExternalConfig> groupConfigMap = allGroups();
        return groupConfigMap == null ? null : groupConfigMap.get(groupId);
    }

    @Override
    public Integer maxSizeLimit(ArgConfigKey key) {
        return configCache.maxSizeLimitOf(key);
    }

    @Override
    public GroupResourceId mappingGroupId(ResourceId methodId) {
        final Map<GroupResourceId, ExternalConfig> groupConfigMap = allGroups();
        if (groupConfigMap == null) {
            return null;
        }
        for (Map.Entry<GroupResourceId, ExternalConfig> entry : groupConfigMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (entry.getValue() instanceof ExternalGroupConfig) {
                ExternalGroupConfig config = (ExternalGroupConfig) entry.getValue();
                Set<ResourceId> methodIds;
                if ((methodIds = config.getItems()) == null) {
                    continue;
                }
                for (ResourceId resourceId : methodIds) {
                    if (resourceId.equals(methodId)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Set<ResourceId> mappingResourceIds(GroupResourceId groupId) {
        final ExternalConfig config = config(groupId);
        return config == null ? emptySet() : unmodifiableSet(((ExternalGroupConfig) config).getItems());
    }

}
