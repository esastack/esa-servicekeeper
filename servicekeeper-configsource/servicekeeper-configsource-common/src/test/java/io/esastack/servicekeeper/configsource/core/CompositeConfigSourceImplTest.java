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
package io.esastack.servicekeeper.configsource.core;

import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeConfigSourceImplTest {

    private final BaseConfigSource source0 = mock(BaseConfigSource.class);
    private final BaseConfigSource source1 = mock(BaseConfigSource.class);

    private CompositeConfigSource source;

    @BeforeEach
    void setUp() {
        when(source0.getOrder()).thenReturn(-1);
        when(source1.getOrder()).thenReturn(0);
        List<BaseConfigSource> sources = new ArrayList<>(2);
        sources.add(source0);
        sources.add(source1);

        source = new CompositeConfigSourceImpl(sources);
    }

    @Test
    void testConfig() {
        final ResourceId id = ResourceId.from("testConfig");

        when(source0.config(id)).thenReturn(new ExternalConfig());
        when(source1.config(id)).thenReturn(null);
        then(source.config(id)).isNotNull();

        when(source0.config(id)).thenReturn(null);
        when(source1.config(id)).thenReturn(null);
        then(source.config(id)).isNull();

        when(source0.config(id)).thenReturn(null);
        when(source1.config(id)).thenReturn(new ExternalConfig());
        then(source.config(id)).isNotNull();
    }

    @Test
    void testAllGroups() {
        when(source0.allGroups()).thenReturn(null);
        when(source1.allGroups()).thenReturn(null);
        then(source.allGroups()).isEmpty();

        final Map<GroupResourceId, ExternalConfig> configs0 = new HashMap<>();
        configs0.put(GroupResourceId.from("a"), new ExternalConfig());

        final Map<GroupResourceId, ExternalConfig> configs1 = new HashMap<>();
        configs1.put(GroupResourceId.from("b"), new ExternalConfig());

        when(source0.allGroups()).thenReturn(configs0);
        when(source1.allGroups()).thenReturn(configs1);
        then(source.allGroups().size()).isEqualTo(2);

        then(source.allGroups().get(GroupResourceId.from("a"))).isNotNull();
        then(source.allGroups().get(GroupResourceId.from("b"))).isNotNull();
    }

    @Test
    void testGroupConfig() {
        final GroupResourceId id = GroupResourceId.from("testGroupConfig");

        when(source0.config(id)).thenReturn(new ExternalConfig());
        when(source1.config(id)).thenReturn(null);
        then(source.config(id)).isNotNull();

        when(source0.config(id)).thenReturn(null);
        when(source1.config(id)).thenReturn(null);
        then(source.config(id)).isNull();

        when(source0.config(id)).thenReturn(null);
        when(source1.config(id)).thenReturn(new ExternalConfig());
        then(source.config(id)).isNotNull();
    }

    @Test
    void testMaxSizeLimit() {
        final ArgConfigKey key = new ArgConfigKey(ResourceId.from("testMaxSizeLimit"),
                "arg0", CIRCUIT_BREAKER);

        when(source0.maxSizeLimit(key)).thenReturn(1);
        when(source1.maxSizeLimit(key)).thenReturn(null);
        then(source.maxSizeLimit(key)).isEqualTo(1);

        when(source0.maxSizeLimit(key)).thenReturn(null);
        when(source1.maxSizeLimit(key)).thenReturn(11);
        then(source.maxSizeLimit(key)).isEqualTo(11);

        when(source0.maxSizeLimit(key)).thenReturn(null);
        when(source1.maxSizeLimit(key)).thenReturn(null);
        then(source.maxSizeLimit(key)).isNull();
    }

    @Test
    void testMappingGroupId() {
        final ResourceId id = ResourceId.from("testMappingGroupId");

        when(source0.mappingGroupId(id)).thenReturn(GroupResourceId.from("a"));
        when(source1.mappingGroupId(id)).thenReturn(null);
        then(source.mappingGroupId(id)).isEqualTo(GroupResourceId.from("a"));

        when(source0.mappingGroupId(id)).thenReturn(null);
        when(source1.mappingGroupId(id)).thenReturn(null);
        then(source.mappingGroupId(id)).isNull();

        when(source0.mappingGroupId(id)).thenReturn(null);
        when(source1.mappingGroupId(id)).thenReturn(GroupResourceId.from("b"));
        then(source.mappingGroupId(id)).isEqualTo(GroupResourceId.from("b"));
    }

    @Test
    void testMappingResourceIds() {
        final GroupResourceId id = GroupResourceId.from("testMappingResourceIds");

        when(source0.mappingResourceIds(id)).thenReturn(null);
        when(source1.mappingResourceIds(id)).thenReturn(null);
        then(source.mappingResourceIds(id)).isEmpty();

        final Set<ResourceId> ids0 = new HashSet<>();
        ids0.add(ResourceId.from("a"));

        final Set<ResourceId> ids1 = new HashSet<>();
        ids1.add(ResourceId.from("b"));

        when(source0.mappingResourceIds(id)).thenReturn(ids0);
        when(source1.mappingResourceIds(id)).thenReturn(ids1);
        then(source.mappingResourceIds(id).size()).isEqualTo(2);

        then(source.mappingResourceIds(id).contains(ResourceId.from("a"))).isTrue();
        then(source.mappingResourceIds(id).contains(ResourceId.from("b"))).isTrue();
    }

    @Test
    void testAll() {
        when(source0.all()).thenReturn(null);
        when(source1.all()).thenReturn(null);
        then(source.all()).isEmpty();

        final Map<ResourceId, ExternalConfig> configs0 = new HashMap<>();
        configs0.put(ResourceId.from("a"), new ExternalConfig());

        final Map<ResourceId, ExternalConfig> configs1 = new HashMap<>();
        configs1.put(ResourceId.from("b"), new ExternalConfig());

        when(source0.all()).thenReturn(configs0);
        when(source1.all()).thenReturn(configs1);
        then(source.all().size()).isEqualTo(2);

        then(source.all().get(ResourceId.from("a"))).isNotNull();
        then(source.all().get(ResourceId.from("b"))).isNotNull();
    }
}
