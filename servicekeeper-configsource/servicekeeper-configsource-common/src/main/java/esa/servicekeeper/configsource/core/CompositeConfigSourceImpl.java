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
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.utils.OrderedComparator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

/**
 * The core class to implement {@link PlainConfigSource}s through proxies, as this class implements from
 * {@link GroupConfigSource} and {@link PlainConfigSource}, you can invoke any methods defined in those interfaces,
 * but be different from other common implementations, it holds many implementations which instantiated by spi and then
 * sorted them by {@link OrderedComparator}. When you invoke any methods defined in the class it will
 * proxy to the inner instances orderly, see that {@link #config(ResourceId)}, {@link #all()} and so on.
 */
public class CompositeConfigSourceImpl implements CompositeConfigSource {

    private final List<BaseConfigSource> sources;

    CompositeConfigSourceImpl(List<BaseConfigSource> sources) {
        Checks.checkNotNull(sources, "sources");
        OrderedComparator.sort(sources);
        this.sources = Collections.unmodifiableList(sources);
    }

    @Override
    public ExternalConfig config(ResourceId id) {
        ExternalConfig config;
        for (PlainConfigSource source : sources) {
            config = source.config(id);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    @Override
    public Map<GroupResourceId, ExternalConfig> allGroups() {
        Set<GroupResourceId> markedId = new HashSet<>(64);
        Map<GroupResourceId, ExternalConfig> compositeMap = new HashMap<>(64);

        Map<GroupResourceId, ExternalConfig> configMap;
        for (CompositeConfigSource source : sources) {
            configMap = source.allGroups();
            if (configMap == null || configMap.isEmpty()) {
                continue;
            }
            configMap.forEach((key, value) -> {
                if (markedId.add(key)) {
                    compositeMap.putIfAbsent(key, value);
                }
            });
        }

        return unmodifiableMap(compositeMap);
    }

    @Override
    public ExternalConfig config(GroupResourceId groupId) {
        ExternalConfig config;
        for (GroupConfigSource source : sources) {
            config = source.config(groupId);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    @Override
    public Integer maxSizeLimit(ArgConfigKey key) {
        Integer maxSizeLimit;
        for (CompositeConfigSource source : sources) {
            maxSizeLimit = source.maxSizeLimit(key);
            if (maxSizeLimit != null) {
                return maxSizeLimit;
            }
        }

        return null;
    }

    @Override
    public GroupResourceId mappingGroupId(ResourceId methodId) {
        GroupResourceId groupId;
        for (GroupConfigSource source : sources) {
            groupId = source.mappingGroupId(methodId);
            if (groupId != null) {
                return groupId;
            }
        }
        return null;
    }

    @Override
    public Set<ResourceId> mappingResourceIds(GroupResourceId groupId) {
        Set<ResourceId> resourceIds = new LinkedHashSet<>(8);

        Set<ResourceId> tempResourceIds;
        for (CompositeConfigSource source : sources) {
            tempResourceIds = source.mappingResourceIds(groupId);
            if (tempResourceIds == null || tempResourceIds.isEmpty()) {
                continue;
            }
            resourceIds.addAll(tempResourceIds);
        }
        return unmodifiableSet(resourceIds);
    }

    @Override
    public Map<ResourceId, ExternalConfig> all() {
        Set<ResourceId> markedId = new HashSet<>(64);
        Map<ResourceId, ExternalConfig> compositeMap = new HashMap<>(64);

        for (PlainConfigSource source : sources) {
            Map<ResourceId, ExternalConfig> configMap = source.all();
            if (configMap == null || configMap.isEmpty()) {
                continue;
            }
            for (Map.Entry<ResourceId, ExternalConfig> entry : configMap.entrySet()) {
                if (markedId.add(entry.getKey())) {
                    compositeMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }

        return unmodifiableMap(compositeMap);
    }
}
