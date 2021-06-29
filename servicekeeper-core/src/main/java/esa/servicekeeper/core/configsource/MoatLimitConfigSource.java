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

import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.moats.MoatType;

import static esa.servicekeeper.core.utils.SystemConfigUtils.getFromEnvAndProp;

public interface MoatLimitConfigSource extends ConfigSource {

    String MAX_SIZE_LIMIT_KEY = "esa.servicekeeper.maxSizeLimit";

    String MAX_CONCURRENT_LIMIT_VALUE_SIZE = "maxConcurrentLimitValueSize";
    String MAX_RATE_LIMIT_VALUE_SIZE = "maxRateLimitValueSize";
    String MAX_CIRCUIT_BREAKER_VALUE_SIZE = "maxCircuitBreakerValueSize";

    String VALUE_MATCH_ALL = "*";

    /**
     * Get the max size limit of the governed values by fullyArgName.
     *
     * @param key fullyArgName which built from methodId + "." + argName + {@link MoatType#getValue()}
     * @return the max size limit
     */
    Integer maxSizeLimit(ArgConfigKey key);

    /**
     * Obtains max size limit of specified {@link ArgConfigKey}s.
     *
     * @param key key
     * @return max size limit
     */
    static int getMaxSizeLimit(final ArgConfigKey key) {
        String maxSizeLimitValue = getFromEnvAndProp(key.toMaxSizeLimitKey());

        if (StringUtils.isNotEmpty(maxSizeLimitValue)) {
            try {
                return Integer.parseInt(maxSizeLimitValue);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        maxSizeLimitValue = getFromEnvAndProp(MAX_SIZE_LIMIT_KEY);
        if (StringUtils.isNotEmpty(maxSizeLimitValue)) {
            try {
                return Integer.parseInt(maxSizeLimitValue);
            } catch (NumberFormatException ex) {
                // ignore
            }
        }

        return 100;
    }

    /**
     * Obtains default max size limit.
     *
     * @return max size limit
     */
    static int getMaxSizeLimit() {
        return 100;
    }

}

