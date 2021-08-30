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
package io.esastack.servicekeeper.ext.factory.spring;

import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerSateTransitionProcessor;
import io.esastack.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

@Configuration
class SateTransitionProcessorContextFactoryImplTest {

    private static AnnotationConfigApplicationContext ctx;
    private static SateTransitionProcessorContextFactoryImpl factory;

    @Bean
    public SpringContextUtils contextUtils() {
        return new SpringContextUtils();
    }

    @Bean
    public CircuitBreakerSateTransitionProcessor processor0() {
        return (name, event) -> {

        };
    }

    @Bean
    public CircuitBreakerSateTransitionProcessor processor1() {
        return (name, event) -> {

        };
    }

    @BeforeAll
    static void setUp() {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(SateTransitionProcessorContextFactoryImplTest.class);
        ctx.refresh();

        factory = new SateTransitionProcessorContextFactoryImpl();
    }

    @Test
    void testDoCreate() {
        then(factory.doCreate().size()).isEqualTo(2);
    }


}
