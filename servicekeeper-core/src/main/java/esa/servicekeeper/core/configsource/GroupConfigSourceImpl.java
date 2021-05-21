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
package esa.servicekeeper.core.configsource;

import esa.commons.Checks;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.internal.ImmutableConfigs;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;

public class GroupConfigSourceImpl implements GroupConfigSource {

    private final GroupConfigSource groupConfigSource;
    private final ImmutableConfigs immutableConfigs;

    public GroupConfigSourceImpl(GroupConfigSource groupConfigSource, ImmutableConfigs immutableConfigs) {
        Checks.checkNotNull(immutableConfigs, "immutableConfigs");
        this.groupConfigSource = groupConfigSource;
        this.immutableConfigs = immutableConfigs;
    }

    @Override
    public Map<GroupResourceId, ExternalConfig> allGroups() {
        if (groupConfigSource == null) {
            return emptyMap();
        }

        Map<GroupResourceId, ExternalConfig> configs = groupConfigSource.allGroups();
        return configs == null ? emptyMap() : unmodifiableMap(configs);
    }

    @Override
    public ExternalConfig config(GroupResourceId groupId) {
        return groupConfigSource == null ? null : groupConfigSource.config(groupId);
    }

    @Override
    public GroupResourceId mappingGroupId(ResourceId methodId) {
        GroupResourceId groupId = groupConfigSource == null
                ? null : groupConfigSource.mappingGroupId(methodId);
        if (groupId != null) {
            return groupId;
        }
        return getImmutableGroupId(methodId);
    }

    @Override
    public Set<ResourceId> mappingResourceIds(GroupResourceId groupId) {
        Set<ResourceId> resourceIds = groupConfigSource == null
                ? null : groupConfigSource.mappingResourceIds(groupId);
        if (resourceIds == null) {
            return unmodifiableSet(getImmutableMappingResourceIds(groupId));
        }

        Set<ResourceId> immutableResourceIds = getImmutableMappingResourceIds(groupId);

        final Set<ResourceId> resourceIds0 = new HashSet<>(resourceIds);
        resourceIds0.addAll(immutableResourceIds);
        return unmodifiableSet(resourceIds0);
    }

    private GroupResourceId getImmutableGroupId(ResourceId methodId) {
        return immutableConfigs.getGroupId(methodId);
    }

    private Set<ResourceId> getImmutableMappingResourceIds(GroupResourceId groupId) {
        return unmodifiableSet(immutableConfigs.getGroupItems(groupId));
    }
}
