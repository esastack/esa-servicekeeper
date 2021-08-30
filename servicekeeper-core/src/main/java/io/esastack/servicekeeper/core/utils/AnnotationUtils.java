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
package io.esastack.servicekeeper.core.utils;

import esa.commons.ObjectUtils;

public final class AnnotationUtils {

    private AnnotationUtils() {
    }

    public static <T> T resolve(String attributeDesc,
                                T value,
                                T aliasValue,
                                T defaultValue) {
        //when value and aliasValue are equal
        if (ObjectUtils.safeEquals(value, aliasValue)) {
            return value;
        }

        //when value not set
        if (ObjectUtils.safeEquals(value, defaultValue)) {
            return aliasValue;
        }

        //when aliasValue not set
        if (ObjectUtils.safeEquals(aliasValue, defaultValue)) {
            return value;
        }

        //when aliasValue and value both set and have different value
        throw new IllegalArgumentException(
                attributeDesc + ",s value and aliasValue both set and have different values.Value is:"
                        + value + ",aliasValue is:" + aliasValue);
    }

}
