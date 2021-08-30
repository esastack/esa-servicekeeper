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
package io.esastack.servicekeeper.configsource.file.utils;

import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ResourceId;
import org.junit.jupiter.api.Test;

import static io.esastack.servicekeeper.configsource.file.utils.MaxSizeLimitUtils.toKey;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class MaxSizeLimitUtilsTest {

    @Test
    void testToKey() {
        String propName = "demoMethod.arg0." + MAX_CIRCUIT_BREAKER_VALUE_SIZE;
        ArgConfigKey argConfigKey = toKey(propName);
        assert argConfigKey != null;
        then(argConfigKey.getMethodId()).isEqualTo(ResourceId.from("demoMethod"));
        then(argConfigKey.getArgName()).isEqualTo("arg0");
        then(argConfigKey.getType()).isEqualTo(CIRCUIT_BREAKER);

        propName = "demoMethod.arg0." + MAX_CONCURRENT_LIMIT_VALUE_SIZE;
        argConfigKey = toKey(propName);
        assert argConfigKey != null;
        then(argConfigKey.getMethodId()).isEqualTo(ResourceId.from("demoMethod"));
        then(argConfigKey.getArgName()).isEqualTo("arg0");
        then(argConfigKey.getType()).isEqualTo(CONCURRENT_LIMIT);

        propName = "demoMethod.arg0." + MAX_RATE_LIMIT_VALUE_SIZE;
        argConfigKey = toKey(propName);
        assert argConfigKey != null;
        then(argConfigKey.getMethodId()).isEqualTo(ResourceId.from("demoMethod"));
        then(argConfigKey.getArgName()).isEqualTo("arg0");
        then(argConfigKey.getType()).isEqualTo(RATE_LIMIT);
    }

}
