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
package esa.servicekeeper.configsource.file.utils;

import esa.servicekeeper.core.common.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;

class GroupItemUtilsTest {

    @Test
    void testParseToItems() {
        String groupItems = "[]";
        then(GroupItemUtils.parseToItems(groupItems)).isEmpty();

        groupItems = "[demoA, demoB, demoC ]";
        Set<ResourceId> resourceIds = GroupItemUtils.parseToItems(groupItems);
        then(resourceIds.size()).isEqualTo(3);
        then(resourceIds.contains(ResourceId.from("demoA"))).isTrue();
        then(resourceIds.contains(ResourceId.from("demoB"))).isTrue();
        then(resourceIds.contains(ResourceId.from("demoC"))).isTrue();
    }
}
