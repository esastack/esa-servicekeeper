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
package esa.servicekeeper.core.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.BDDAssertions.then;

class LogUtilsTest {

    @Test
    void testConcatListValue() {
        String rst = LogUtils.concatValue((List<?>) null);
        then(rst).isEqualTo("[]");

        rst = LogUtils.concatValue(Collections.emptyList());
        then(rst).isEqualTo("[]");

        rst = LogUtils.concatValue(Collections.singletonList("ABC"));
        then(rst).isEqualTo("[" + lineSeparator() + "ABC]");

        List<String> values = new ArrayList<>(2);
        values.add("ABC");
        values.add("DEF");
        rst = LogUtils.concatValue(values);
        then(rst).isEqualTo("[" + lineSeparator() + "ABC" + lineSeparator() + "DEF]");
    }

    @Test
    void testConcatMapValue() {
        String rst = LogUtils.concatValue((Map<?, ?>) null);
        then(rst).isEqualTo("[]");

        rst = LogUtils.concatValue(Collections.emptyMap());
        then(rst).isEqualTo("[]");

        rst = LogUtils.concatValue(Collections.singletonMap("ABC", "DEF"));
        then(rst).isEqualTo("[" + lineSeparator() + "ABC : DEF]");

        Map<String, String> values = new HashMap<>(2);
        values.put("ABC", "DEF");
        values.put("XYZ", "DEF");
        rst = LogUtils.concatValue(values);
        then(rst).isEqualTo("[" + lineSeparator() + "XYZ : DEF" + lineSeparator() + "ABC : DEF]");
    }
}
