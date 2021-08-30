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
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.utils.ClassCastUtils;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PredicateByExceptionTest {

    @Test
    void testIsSuccess() {
        final Context ctx = mock(Context.class);

        // Case1: IgnoreExceptions[] is null
        PredicateByException predicateByException = new PredicateByException();

        when(ctx.getBizException()).thenReturn(new RuntimeException());
        then(predicateByException.isSuccess(ctx)).isFalse();

        when(ctx.getBizException()).thenReturn(null);
        then(predicateByException.isSuccess(ctx)).isTrue();

        // Case2: The bizException is instanceOf IgnoreException
        predicateByException = new PredicateByException(ClassCastUtils.cast(new Class[]{RuntimeException.class}),
                ClassCastUtils.cast(new Class[]{RuntimeException.class}), ResourceId.from("none"));
        when(ctx.getBizException()).thenReturn(new IllegalArgumentException());
        then(predicateByException.isSuccess(ctx)).isTrue();

        when(ctx.getBizException()).thenReturn(new RuntimeException());
        then(predicateByException.isSuccess(ctx)).isTrue();

        // Case3: The bizException is not instanceOf IgnoreException
        when(ctx.getBizException()).thenReturn(new Exception());
        then(predicateByException.isSuccess(ctx)).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFond() {
        final PredicateByException predicate = new PredicateByException();
        then(predicate.getFond(null).length).isEqualTo(0);

        final ExternalConfig config = new ExternalConfig();
        then(predicate.getFond(config).length).isEqualTo(0);

        config.setIgnoreExceptions(new Class[]{RuntimeException.class});
        then(Arrays.equals(new Class[]{RuntimeException.class}, predicate.getFond(config))).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateWithNewestConfig() {
        final PredicateByException predicate = new PredicateByException();
        then(predicate.isConfigEquals(new Class[0])).isTrue();

        predicate.updateWithNewestConfig(new Class[]{RuntimeException.class});
        then(predicate.isConfigEquals(new Class[0])).isFalse();

        predicate.updateWhenNewestConfigIsNull();
        then(predicate.isConfigEquals(new Class[0])).isTrue();
    }

    @Test
    void testListeningKey() {
        final PredicateByException predicate = new PredicateByException(ResourceId.from("ABC"));
        BDDAssertions.then(predicate.listeningKey()).isEqualTo(ResourceId.from("ABC"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFill() {
        final PredicateByException predicate = new PredicateByException(ResourceId.from("ABC"));
        predicate.updateWithNewestConfig(new Class[]{RuntimeException.class});

        final CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .ignoreExceptions(new Class[]{IllegalStateException.class}).build();

        predicate.fill(config);
        then(Arrays.equals(new Class[]{RuntimeException.class}, config.getIgnoreExceptions())).isTrue();
    }

}
