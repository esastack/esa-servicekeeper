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

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.moats.MoatType;
import org.junit.jupiter.api.Test;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoatLimitConfigSourceImplTest {

    private final ImmutableConfigs configs = mock(ImmutableConfigs.class);
    private final MoatLimitConfigSource source0 = mock(MoatLimitConfigSource.class);
    private final MoatLimitConfigSource source = new MoatLimitConfigSourceImpl(source0, configs);

    @Test
    void testMaxSizeLimit() {
        final ResourceId resourceId = ResourceId.from("testMaxSizeLimit");
        final String argName = "arg0";
        final MoatType type = CIRCUIT_BREAKER;

        final ArgConfigKey key = new ArgConfigKey(resourceId, argName, type);
        when(configs.getMaxSizeLimit(resourceId, argName, type)).thenReturn(null);
        when(source0.maxSizeLimit(key)).thenReturn(1);
        then(source.maxSizeLimit(key)).isEqualTo(1);

        when(source0.maxSizeLimit(key)).thenReturn(null);
        when(configs.getMaxSizeLimit(resourceId, argName, type)).thenReturn(11);
        then(source.maxSizeLimit(key)).isEqualTo(11);

        when(source0.maxSizeLimit(key)).thenReturn(null);
        when(configs.getMaxSizeLimit(resourceId, argName, type)).thenReturn(null);
        then(source.maxSizeLimit(key)).isEqualTo(100);
    }

}
