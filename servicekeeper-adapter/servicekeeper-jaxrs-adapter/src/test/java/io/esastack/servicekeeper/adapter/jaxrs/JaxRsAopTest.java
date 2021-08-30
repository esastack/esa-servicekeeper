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
package io.esastack.servicekeeper.adapter.jaxrs;

import io.esastack.servicekeeper.adapter.spring.aop.DefaultServiceKeeperAop;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import javax.ws.rs.Path;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({JaxRsAutoSupportConfigurator.class})
public class JaxRsAopTest {

    private static AnnotationConfigApplicationContext ctx;

    @Bean
    public HelloService helloService() {
        return new HelloService();
    }

    @BeforeAll
    static void setUp() {
        System.setProperty("servicekeeper.configurators.disable", "true");
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(JaxRsAopTest.class);

        ctx.refresh();
    }

    @Test
    void test() {
        then(ctx.getBean(JaxRsAutoSupportAop.class)).isNotNull();
        assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(DefaultServiceKeeperAop.class));

        final HelloService service = ctx.getBean(HelloService.class);
        then(service.testPath()).isEqualTo("Path");
        assertThrows(RateLimitOverflowException.class, service::testPath);
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    public static class HelloService {
        @Path("aaa")
        public String testPath() {
            return "Path";
        }
    }
}
