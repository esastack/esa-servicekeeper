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

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamCheckUtilsTest {

    @Test
    void testLegalFailureThreshold() {
        ParamCheckUtils.legalFailureThreshold(0f, "");
        ParamCheckUtils.legalFailureThreshold(100f, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils
                .legalFailureThreshold(-1.0f, ""));
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils
                .legalFailureThreshold(101f, ""));
    }

    @Test
    void testPositiveInt() {
        ParamCheckUtils.positiveInt(1, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.positiveInt(0, ""));
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.positiveInt(-1, ""));
    }

    @Test
    void testPositiveLong() {
        ParamCheckUtils.positiveLong(1L, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.positiveLong(0L, ""));
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.positiveLong(-1L, ""));
    }

    @Test
    void testNotNegativeInt() {
        ParamCheckUtils.notNegativeInt(0, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.notNegativeInt(-1, ""));
    }

    @Test
    void testNotNegativeLong() {
        ParamCheckUtils.notNegativeLong(0L, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.notNegativeLong(-1L, ""));
    }

    @Test
    void testNotNegativeDouble() {
        ParamCheckUtils.notNegativeDouble(0d, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.notNegativeDouble(-1d,
                ""));
    }

    @Test
    void testIsTrue() {
        ParamCheckUtils.isTrue(true, "");
        assertThrows(IllegalArgumentException.class, () -> ParamCheckUtils.isTrue(false, ""));
    }

}
