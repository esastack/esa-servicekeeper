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

import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.moats.MoatType;

import static esa.servicekeeper.configsource.constant.Constants.PERIOD_EN;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;
import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;

final class MaxSizeLimitUtils {

    private MaxSizeLimitUtils() {
    }

    /**
     * Convert propName to {@link ArgConfigKey}, eg: demoMethod.arg0.maxConcurrentLimitValueSize ->
     * {methodId: demoMethod, argName: arg0, type: RateLimit} and so on.
     *
     * @param propName propName
     * @return meta data
     */
    static ArgConfigKey toKey(final String propName) {
        if (StringUtils.isEmpty(propName)) {
            return null;
        }
        final String typeName = propName.substring(propName.lastIndexOf(PERIOD_EN) + 1);
        MoatType type = null;
        if (MAX_CONCURRENT_LIMIT_VALUE_SIZE.equals(typeName)) {
            type = CONCURRENT_LIMIT;
        }
        if (MAX_RATE_LIMIT_VALUE_SIZE.equals(typeName)) {
            type = RATE_LIMIT;
        }
        if (MAX_CIRCUIT_BREAKER_VALUE_SIZE.equals(typeName)) {
            type = CIRCUIT_BREAKER;
        }

        final String fullyArgName = propName.substring(0, propName.lastIndexOf(PERIOD_EN));
        final String argName = propName.substring(fullyArgName.lastIndexOf(PERIOD_EN) + 1,
                fullyArgName.length());

        final ResourceId methodId = ResourceId.from(fullyArgName.substring(0, fullyArgName.lastIndexOf(PERIOD_EN)));

        return new ArgConfigKey(methodId, argName, type);
    }

}
