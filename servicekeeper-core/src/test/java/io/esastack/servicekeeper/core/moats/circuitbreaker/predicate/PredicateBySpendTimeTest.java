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
package io.esastack.servicekeeper.core.moats.circuitbreaker.predicate;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateBySpendTimeTest {

    private final long maxSpendTimeMs = RandomUtils.randomLong();
    private final ResourceId resourceId = ResourceId.from("PredicateBySpendTimeTest");

    @Test
    void testIsSuccess() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        final Context ctx = mock(Context.class);
        when(ctx.getSpendTimeMs()).thenReturn(maxSpendTimeMs - 1).thenReturn(maxSpendTimeMs + 1);
        then(predicateBySpendTime.isSuccess(ctx)).isTrue();
        then(predicateBySpendTime.isSuccess(ctx)).isFalse();
    }

    @Test
    void testGetConcernedConfig() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        then(predicateBySpendTime.getFond(null)).isNull();
        then(predicateBySpendTime.getFond(new ExternalConfig())).isNull();

        final long newestMaxSpendTimeMs = RandomUtils.randomLong();
        ExternalConfig config = new ExternalConfig();
        config.setMaxSpendTimeMs(newestMaxSpendTimeMs);
        then(predicateBySpendTime.getFond(config).floatValue()).isEqualTo(newestMaxSpendTimeMs);
    }

    @Test
    void testUpdateWithNewestConfig() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        final long newestMaxSpendTime = RandomUtils.randomLong();

        ExternalConfig config = new ExternalConfig();
        config.setMaxSpendTimeMs(newestMaxSpendTime);
        predicateBySpendTime.updateWithNewestConfig(predicateBySpendTime.getFond(config));
        final Context ctx = mock(Context.class);
        when(ctx.getSpendTimeMs()).thenReturn(newestMaxSpendTime - 1).thenReturn(newestMaxSpendTime + 1);
        then(predicateBySpendTime.isSuccess(ctx)).isTrue();
        then(predicateBySpendTime.isSuccess(ctx)).isFalse();
    }

    @Test
    void testUpdateWhenNewestConfigIsNull() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        predicateBySpendTime.updateWithNewestConfig(500L);

        predicateBySpendTime.updateWhenNewestConfigIsNull();
        final Context ctx = mock(Context.class);
        when(ctx.getSpendTimeMs()).thenReturn(maxSpendTimeMs - 1).thenReturn(maxSpendTimeMs + 1);
        then(predicateBySpendTime.isSuccess(ctx)).isTrue();
        then(predicateBySpendTime.isSuccess(ctx)).isFalse();
    }

    @Test
    void testIsConfigEquals() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        then(predicateBySpendTime.isConfigEquals(maxSpendTimeMs)).isTrue();
        then(predicateBySpendTime.isConfigEquals(maxSpendTimeMs + 1)).isFalse();
    }

    @Test
    void testUpdate() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        ExternalConfig config = new ExternalConfig();
        final long newestMaxSpendTimeMs = RandomUtils.randomLong();
        config.setMaxSpendTimeMs(newestMaxSpendTimeMs);
        predicateBySpendTime.onUpdate(config);

        final Context ctx = mock(Context.class);
        when(ctx.getSpendTimeMs()).thenReturn(newestMaxSpendTimeMs - 1).thenReturn(newestMaxSpendTimeMs + 1);
        then(predicateBySpendTime.isSuccess(ctx)).isTrue();
        then(predicateBySpendTime.isSuccess(ctx)).isFalse();

        predicateBySpendTime.onUpdate(null);
        when(ctx.getSpendTimeMs()).thenReturn(maxSpendTimeMs - 1).thenReturn(maxSpendTimeMs + 1);
        then(predicateBySpendTime.isSuccess(ctx)).isTrue();
        then(predicateBySpendTime.isSuccess(ctx)).isFalse();
    }

    @Test
    void testGetListeningKey() {
        PredicateBySpendTime predicateBySpendTime = new PredicateBySpendTime(maxSpendTimeMs,
                maxSpendTimeMs, resourceId);
        BDDAssertions.then(predicateBySpendTime.listeningKey()).isEqualTo(resourceId);
    }
}
