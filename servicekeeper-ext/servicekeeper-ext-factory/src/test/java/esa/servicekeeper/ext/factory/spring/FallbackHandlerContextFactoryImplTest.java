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

import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.fallback.FallbackToException;
import esa.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@Configuration
class FallbackHandlerContextFactoryImplTest {

    private static AnnotationConfigApplicationContext ctx;

    private static FallbackHandlerContextFactoryImpl factory;

    @Bean
    public SpringContextUtils contextUtils() {
        return new SpringContextUtils();
    }

    @Bean
    public Exception exception() {
        return new RuntimeException();
    }

    @Bean
    public AsyncResultHandler<?> asyncResultHandler() {
        return new AsyncResultHandler<Object>() {
            @Override
            public boolean supports(Class<?> returnType) {
                return false;
            }

            @Override
            public Object handle0(Object returnValue, RequestHandle requestHandle) {
                return null;
            }
        };
    }

    @BeforeAll
    static void setUp() {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(FallbackHandlerContextFactoryImplTest.class);
        ctx.refresh();

        factory = new FallbackHandlerContextFactoryImpl();
    }

    @Test
    void testDoCreate() {
        FallbackToException fallback = factory.doCreate(Exception.class, mock(FallbackConfig.class));
        assertThrows(RuntimeException.class, () -> fallback.handle(mock(Context.class)));
    }

    @Test
    void testNewInstance() {
        then(factory.newInstance(Exception.class)).isInstanceOf(RuntimeException.class);
        then(factory.newInstance(AsyncResultHandler.class)).isNotNull();
    }

}
