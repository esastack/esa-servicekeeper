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
package io.esastack.servicekeeper.core.configsource;

import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupConfigSourceImplTest {

    @Test
    void testGetAllGroups() {
        GroupConfigSource groupConfigSource;

        final Map<GroupResourceId, ExternalConfig> configMap = new HashMap<>(3);
        configMap.putIfAbsent(GroupResourceId.from("demoGroupA"), new ExternalConfig());
        configMap.putIfAbsent(GroupResourceId.from("demoGroupB"), new ExternalConfig());

        final GroupConfigSource configSource = mock(GroupConfigSource.class);
        when(configSource.allGroups()).thenReturn(null).thenReturn(Collections.emptyMap()).thenReturn(configMap);
        groupConfigSource = new GroupConfigSourceImpl(configSource, new ImmutableConfigsImpl());

        then(groupConfigSource.allGroups()).isEmpty();
        then(groupConfigSource.allGroups()).isEmpty();
        then(groupConfigSource.allGroups().size()).isEqualTo(2);
    }

    @Test
    void testGetGroupConfig() {
        GroupConfigSource groupConfigSource;

        final GroupConfigSource configSource = mock(GroupConfigSource.class);
        when(configSource.config(GroupResourceId.from("demoGroupA"))).thenReturn(null)
                .thenReturn(new ExternalConfig());
        when(configSource.config(GroupResourceId.from("demoGroupB"))).thenReturn(null)
                .thenReturn(new ExternalConfig());

        groupConfigSource = new GroupConfigSourceImpl(configSource, new ImmutableConfigsImpl());

        then(groupConfigSource.config(GroupResourceId.from("demoGroupA"))).isNull();
        then(groupConfigSource.config(GroupResourceId.from("demoGroupB"))).isNull();

        then(groupConfigSource.config(GroupResourceId.from("demoGroupA"))).isNotNull();
        then(groupConfigSource.config(GroupResourceId.from("demoGroupB"))).isNotNull();
    }

    @Test
    void testGetMappingGroupId() {
        final ResourceId resourceId = ResourceId.from("testGetMappingGroupId1");
        final ImmutableConfigs configs = new ImmutableConfigsImpl();
        configs.getOrCompute(resourceId, () -> CompositeServiceKeeperConfig
                .builder().group(GroupResourceId.from("demoA")).build());

        final GroupConfigSource configSource = mock(GroupConfigSource.class);
        when(configSource.mappingGroupId(resourceId)).thenReturn(null).thenReturn(GroupResourceId.from("demoC"));

        GroupConfigSource groupConfigSource = new GroupConfigSourceImpl(configSource, configs);
        then(groupConfigSource.mappingGroupId(resourceId)).isEqualTo(GroupResourceId.from("demoA"));
        then(groupConfigSource.mappingGroupId(resourceId)).isEqualTo(GroupResourceId.from("demoC"));
    }

    @Test
    void testGetMappingResourceIds() {
        final GroupResourceId groupId = GroupResourceId.from("demoA");
        final ResourceId resourceId1 = ResourceId.from("testGetMappingGroupId1");

        final ImmutableConfigs configs = new ImmutableConfigsImpl();
        configs.getOrCompute(resourceId1, () -> CompositeServiceKeeperConfig
                .builder().group(groupId).build());

        final ResourceId resourceId2 = ResourceId.from("testGetMappingGroupId2");
        configs.getOrCompute(resourceId2, () -> CompositeServiceKeeperConfig
                .builder().group(groupId).methodConfig(null).build());

        final GroupConfigSource configSource = mock(GroupConfigSource.class);
        final Set<ResourceId> resourceIds = new HashSet<>(2);
        resourceIds.add(ResourceId.from("testGetMappingGroupId3"));
        resourceIds.add(ResourceId.from("testGetMappingGroupId4"));

        when(configSource.mappingResourceIds(groupId)).thenReturn(null)
                .thenReturn(new HashSet<>()).thenReturn(resourceIds);
        GroupConfigSource groupConfigSource = new GroupConfigSourceImpl(configSource, configs);

        then(groupConfigSource.mappingResourceIds(groupId).size()).isEqualTo(2);
        then(groupConfigSource.mappingResourceIds(groupId).size()).isEqualTo(2);
        then(groupConfigSource.mappingResourceIds(groupId).size()).isEqualTo(4);
    }

}
