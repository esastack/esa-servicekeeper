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
package io.esastack.servicekeeper.core.moats.circuitbreaker;

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class CircuitBreakerRegistryTest {

    private final CircuitBreakerRegistry registry = CircuitBreakerRegistry.singleton();

    @Test
    void testGetOrCreate() {
        CircuitBreaker circuitBreaker = registry.getOrCreate("test", CircuitBreakerConfig.ofDefault(),
                null, null);
        then(registry.getOrCreate("test", null, null, null))
                .isSameAs(circuitBreaker);
    }

    @Test
    void testUnRegister() {
        CircuitBreaker circuitBreaker = registry.getOrCreate("test",
                CircuitBreakerConfig.ofDefault(), null, null);
        then(registry.getOrCreate("test", null, null, null))
                .isSameAs(circuitBreaker);
        registry.unRegister("test");
        then(registry.getOrCreate("test", CircuitBreakerConfig.ofDefault(), null,
                null)).isNotSameAs(circuitBreaker);
    }

}
