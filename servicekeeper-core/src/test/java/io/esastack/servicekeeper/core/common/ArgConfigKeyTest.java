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

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class ArgConfigKeyTest {

    @Test
    void testToMaxSizeLimitKey() {
        final ResourceId methodId = ResourceId.from("demoMethod");
        final String argName = "arg0";

        then(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER).toMaxSizeLimitKey())
                .isEqualTo("demoMethod.arg0." + MAX_CIRCUIT_BREAKER_VALUE_SIZE);

        then(new ArgConfigKey(methodId, argName, RATE_LIMIT).toMaxSizeLimitKey())
                .isEqualTo("demoMethod.arg0." + MAX_RATE_LIMIT_VALUE_SIZE);

        then(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT).toMaxSizeLimitKey())
                .isEqualTo("demoMethod.arg0." + MAX_CONCURRENT_LIMIT_VALUE_SIZE);
    }

}
