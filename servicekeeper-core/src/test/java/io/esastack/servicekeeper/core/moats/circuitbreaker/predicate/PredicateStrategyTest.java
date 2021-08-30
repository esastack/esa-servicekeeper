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
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.executionchain.Executable;
import io.esastack.servicekeeper.core.executionchain.SyncContext;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChain;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChainImpl;
import io.esastack.servicekeeper.core.fallback.FallbackToValue;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.BDDAssertions.then;

class PredicateStrategyTest {

    @Test
    void testIsSuccess() throws Throwable {
        final ResourceId resourceId = ResourceId.from("testIsSuccess");

        final Executable<String> executable = () -> {
            throw new IllegalStateException();
        };

        final SyncExecutionChain chain = new SyncExecutionChainImpl(Collections.singletonList(
                new CircuitBreakerMoat(new MoatConfig(resourceId),
                        CircuitBreakerConfig.ofDefault(), CircuitBreakerConfig.ofDefault(),
                        (ctx) -> true)), new FallbackToValue("Fallback", false));

        for (int i = 0; i < 100; i++) {
            final Context ctx = new SyncContext(resourceId.getName());
            try {
                chain.execute(ctx, null, executable);
            } catch (Exception ex) {
                // Ignore
            } finally {
                then(ctx.getBizException()).isInstanceOf(IllegalStateException.class);
                then(ctx.getResult()).isNull();
            }
        }
    }
}
