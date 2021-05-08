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
package esa.servicekeeper.configsource.utils;

import esa.servicekeeper.core.common.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

class ResourceIdUtilsTest {

    @Test
    void testParseWithSuffix() {
        then(ResourceIdUtils.parseWithSuffix(null).getName()).isEqualTo("");
        then(ResourceIdUtils.parseWithSuffix(null).isRegex()).isFalse();

        then(ResourceIdUtils.parseWithSuffix(" ").getName()).isEqualTo("");
        then(ResourceIdUtils.parseWithSuffix(" ").isRegex()).isFalse();

        final String name0 = " a ";
        then(ResourceIdUtils.parseWithSuffix(name0).getName()).isEqualTo("a");
        then(ResourceIdUtils.parseWithSuffix(name0).isRegex()).isFalse();

        final String name1 = " a.xxx ";
        then(ResourceIdUtils.parseWithSuffix(name1).getName()).isEqualTo("a");
        then(ResourceIdUtils.parseWithSuffix(name1).isRegex()).isFalse();

        final String name2 = "regex: abc ";
        then(ResourceIdUtils.parseWithSuffix(name2).getName()).isEqualTo("abc");
        then(ResourceIdUtils.parseWithSuffix(name2).isRegex()).isTrue();

        final String name3 = "regex: abc.xxx";
        then(ResourceIdUtils.parseWithSuffix(name3).getName()).isEqualTo("abc");
        then(ResourceIdUtils.parseWithSuffix(name3).isRegex()).isTrue();
    }

    @Test
    void testComplexParse() {
        then(ResourceIdUtils.complexParse(null)).isEmpty();
        then(ResourceIdUtils.complexParse("")).isEmpty();

        final String name = "regex: abc ";
        final List<ResourceId> ids = ResourceIdUtils.complexParse(name);
        then(ids.size()).isEqualTo(1);
        then(ids.get(0).isRegex()).isTrue();
        then(ids.get(0).getName()).isEqualTo("abc");

        final String name1 = "@   ";
        final List<ResourceId> ids1 = ResourceIdUtils.complexParse(name1);
        then(ids1.size()).isEqualTo(1);
        then(ids1.get(0)).isInstanceOf(ResourceId.class);
        then(ids1.get(0).getName()).isEqualTo("");

        final String name2 = "esa.arg0@a, b,c, d ,xyz.*";
        final List<ResourceId> ids2 = ResourceIdUtils.complexParse(name2);
        then(ids2.size()).isEqualTo(5);
        then(ids2.get(0).getName()).isEqualTo("esa.arg0.a");
        then(ids2.get(0).isRegex()).isFalse();

        then(ids2.get(1).getName()).isEqualTo("esa.arg0.b");
        then(ids2.get(1).isRegex()).isFalse();

        then(ids2.get(2).getName()).isEqualTo("esa.arg0.c");
        then(ids2.get(2).isRegex()).isFalse();

        then(ids2.get(3).getName()).isEqualTo("esa.arg0.d");
        then(ids2.get(3).isRegex()).isFalse();

        then(ids2.get(4).getName()).isEqualTo("esa.arg0.xyz.*");
        then(ids2.get(4).isRegex()).isFalse();

        final String name3 = "regex:demo.*";
        final List<ResourceId> ids3 = ResourceIdUtils.complexParse(name3);
        then(ids3.size()).isEqualTo(1);
        then(ids3.get(0).getName()).isEqualTo("demo.*");
        then(ids3.get(0).isRegex()).isTrue();
    }
}
