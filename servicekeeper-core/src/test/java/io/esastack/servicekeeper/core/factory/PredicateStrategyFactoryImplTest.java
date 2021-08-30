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
package io.esastack.servicekeeper.core.factory;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import io.esastack.servicekeeper.core.utils.ClassCastUtils;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PredicateStrategyFactoryImplTest {

    private final MoatConfig config = new MoatConfig(ResourceId.from("PredicateStrategyFactoryImplTest"));

    private final PredicateStrategyFactoryImpl factory = new PredicateStrategyFactoryImpl();

    @Test
    void testPredicateByExceptionStrategy() {
        // Case1: IgnoreExceptions is null
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateByException.class).build();
        PredicateStrategyConfig config =
                PredicateStrategyConfig.from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isInstanceOf(PredicateByException.class);

        // Case2: IgnoreExceptions is not null
        breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateByException.class)
                .ignoreExceptions(ClassCastUtils.cast(new Class[]{IllegalArgumentException.class})).build();
        config = PredicateStrategyConfig.from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isInstanceOf(PredicateByException.class);
    }

    @Test
    void testPredicateBySpendTimeStrategy() {
        // Case1: maxSpendTimeMs is positive number
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateBySpendTime.class)
                .maxSpendTimeMs(RandomUtils.randomLong()).build();
        PredicateStrategyConfig config = PredicateStrategyConfig
                .from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isInstanceOf(PredicateBySpendTime.class);

        // Case2: maxSpendTimeMs isn't positive number
        breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateBySpendTime.class).build();
        config = PredicateStrategyConfig
                .from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isNotNull();
    }

    @Test
    void testPredicateByExceptionSpendTimeStrategy() {
        // Case1: maxSpendTimeMs is positive number
        CircuitBreakerConfig breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateByExceptionAndSpendTime.class)
                .maxSpendTimeMs(RandomUtils.randomLong()).build();
        PredicateStrategyConfig config = PredicateStrategyConfig
                .from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isInstanceOf(PredicateByExceptionAndSpendTime.class);

        // Case2: maxSpendTimeMs isn't positive number
        breakerConfig = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateByExceptionAndSpendTime.class).build();
        config = PredicateStrategyConfig
                .from(this.config.getResourceId(), breakerConfig, null);
        BDDAssertions.then(factory.get(config)).isNotNull();
    }

    @Test
    void testDoCreate0() {
        BDDAssertions.then(factory.doCreate0(PredicateStrategyConfig.from(
                ResourceId.from("abc"), CircuitBreakerConfig.ofDefault(),
                CircuitBreakerConfig.ofDefault()))).isNotNull();
        assertThrows(RuntimeException.class, () -> factory.doCreate0(
                PredicateStrategyConfig.from(ResourceId.from("xyz"), CircuitBreakerConfig.builder()
                                .predicateStrategy(PredicateByExceptionAndSpendTime.class).build(),
                        CircuitBreakerConfig.ofDefault())));
    }
}
