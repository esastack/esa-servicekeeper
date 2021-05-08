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
package esa.servicekeeper.core.internal.impl;

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.LimitableKey;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.MoatLimitConfigSource;
import esa.servicekeeper.core.internal.MoatCreationLimit;
import esa.servicekeeper.core.moats.MoatStatistics;
import esa.servicekeeper.core.moats.MoatStatisticsImpl;
import org.junit.jupiter.api.Test;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoatCreationLimitImplTest {

    private final MoatStatistics statistics = mock(MoatStatisticsImpl.class);
    private final MoatLimitConfigSource config = mock(MoatLimitConfigSource.class);

    private final MoatCreationLimit limit = new MoatCreationLimitImpl(statistics, config);

    @Test
    void testCanCreate() {
        final ResourceId methodId = ResourceId.from("abc");
        then(limit.canCreate(new LimitableKey(methodId, null))).isTrue();

        final ArgConfigKey key = new ArgConfigKey(methodId, "arg0", CIRCUIT_BREAKER);
        when(statistics.countOf(key)).thenReturn(-100);
        when(config.maxSizeLimit(key)).thenReturn(-10);

        then(limit.canCreate(new LimitableKey(new ArgResourceId(key.getMethodId(), key.getArgName(), "foo"),
                key.getType()))).isFalse();

        when(config.maxSizeLimit(key)).thenReturn(0);
        when(statistics.countOf(key)).thenReturn(0);
        then(limit.canCreate(new LimitableKey(new ArgResourceId(key.getMethodId(), key.getArgName(), "foo1"),
                key.getType()))).isFalse();

        when(config.maxSizeLimit(key)).thenReturn(9);
        when(statistics.countOf(key)).thenReturn(10);
        then(limit.canCreate(new LimitableKey(new ArgResourceId(key.getMethodId(), key.getArgName(), "foo2"),
                key.getType()))).isFalse();

        when(config.maxSizeLimit(key)).thenReturn(10);
        when(statistics.countOf(key)).thenReturn(10);
        then(limit.canCreate(new LimitableKey(new ArgResourceId(key.getMethodId(), key.getArgName(), "foo3"),
                key.getType()))).isTrue();

        when(config.maxSizeLimit(key)).thenReturn(100);
        when(statistics.countOf(key)).thenReturn(99);
        then(limit.canCreate(new LimitableKey(new ArgResourceId(key.getMethodId(), key.getArgName(), "foo3"),
                key.getType()))).isTrue();
    }
}
