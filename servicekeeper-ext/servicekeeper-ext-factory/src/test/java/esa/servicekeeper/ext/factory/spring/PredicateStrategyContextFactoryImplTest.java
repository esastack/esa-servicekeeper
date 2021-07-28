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
package esa.servicekeeper.ext.factory.spring;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.factory.PredicateStrategyConfig;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

@Configuration
class PredicateStrategyContextFactoryImplTest {

    private static PredicateStrategyContextFactoryImpl factory;

    @Bean
    public SpringContextUtils contextUtils() {
        return new SpringContextUtils();
    }

    @Bean
    public PredicateStrategy predicateStrategy() {
        return new CustomPredicateStrategy();
    }

    @BeforeAll
    static void setUp() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(PredicateStrategyContextFactoryImplTest.class);
        ctx.refresh();

        factory = new PredicateStrategyContextFactoryImpl();
    }

    @Test
    void testDoCreate0() {
        then(factory.doCreate0(PredicateStrategyConfig.from(ResourceId.from("abc"), CircuitBreakerConfig.builder()
                .predicateStrategy(CustomPredicateStrategy.class).build(), CircuitBreakerConfig.ofDefault())))
                .isNotNull();
    }

    private static class CustomPredicateStrategy implements PredicateStrategy {
        @Override
        public boolean isSuccess(Context ctx) {
            return false;
        }
    }

}
