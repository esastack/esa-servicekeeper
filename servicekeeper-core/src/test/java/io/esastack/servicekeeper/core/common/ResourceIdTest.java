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
package io.esastack.servicekeeper.core.common;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

class ResourceIdTest {

    @Test
    void testEquals() {
        ResourceId resourceId1 = ResourceId.from("abc");
        ResourceId resourceId2 = ResourceId.from("abc");
        then(resourceId1).isEqualTo(resourceId2);
        then(resourceId1.hashCode()).isEqualTo(resourceId2.hashCode());

        ResourceId resourceId3 = ResourceId.from("abc.def.xyz");
        ResourceId resourceId4 = new ArgResourceId(ResourceId.from("abc"), "def", "xyz");
        then(resourceId3).isEqualTo(resourceId4);
        then(resourceId3.hashCode()).isEqualTo(resourceId4.hashCode());

        ResourceId resourceId5 = ResourceId.from("abc", false);
        then(resourceId1).isEqualTo(resourceId5);
        ResourceId resourceId6 = ResourceId.from("abc", true);
        then(resourceId6).isNotEqualTo(resourceId1);
    }

    @Test
    void testArgConstructor() {
        ArgResourceId argResourceId = new ArgResourceId("abc.def", "xyz");
        then(argResourceId.getArgName()).isEqualTo("def");
        then(argResourceId.getMethodId()).isEqualTo(ResourceId.from("abc"));
        then(argResourceId.getArgValue()).isEqualTo("xyz");
    }

    @Test
    void testHashCode() {
        Map<ResourceId, String> map = new LinkedHashMap<>(4);
        map.putIfAbsent(ResourceId.from("a.b.c"), "abc");
        then(map.get(new ArgResourceId(ResourceId.from("a"), "b", "c"))).isEqualTo("abc");
    }
}
