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
package esa.servicekeeper.adapter.springboot;

import esa.servicekeeper.adapter.spring.aop.DefaultServiceKeeperAop;
import esa.servicekeeper.adapter.spring.aop.WebAutoSupportAop;
import esa.servicekeeper.core.annotation.RateLimiter;
import esa.servicekeeper.core.exception.RateLimitOverFlowException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({ServiceKeeperConfigurator.class})
class DefaultServiceKeeperAopTest {

    private static AnnotationConfigApplicationContext ctx;

    @Bean
    public HelloService helloService() {
        return new HelloService();
    }

    @BeforeAll
    static void setUp() {
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(DefaultServiceKeeperAopTest.class);

        ctx.refresh();
    }

    @Test
    void test() {
        then(ctx.getBean(DefaultServiceKeeperAop.class)).isNotNull();
        assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(WebAutoSupportAop.class));

        final HelloService service = ctx.getBean(HelloService.class);
        then(service.testRequest()).isEqualTo("Request");
        assertThrows(RateLimitOverFlowException.class, service::testRequest);

        then(service.testGet()).isEqualTo("Get");
        assertThrows(RateLimitOverFlowException.class, service::testGet);

        then(service.testPost()).isEqualTo("Post");
        assertThrows(RateLimitOverFlowException.class, service::testPost);

        then(service.testPut()).isEqualTo("Put");
        assertThrows(RateLimitOverFlowException.class, service::testPut);

        then(service.testDelete()).isEqualTo("Delete");
        assertThrows(RateLimitOverFlowException.class, service::testDelete);

        then(service.testPatch()).isEqualTo("Patch");
        assertThrows(RateLimitOverFlowException.class, service::testPatch);
    }

    public static class HelloService {

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testRequest() {
            return "Request";
        }

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testGet() {
            return "Get";
        }

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testPost() {
            return "Post";
        }

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testPut() {
            return "Put";
        }

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testDelete() {
            return "Delete";
        }

        @RateLimiter(limitForPeriod = 1, limitRefreshPeriod = "10s")
        public String testPatch() {
            return "Patch";
        }

    }
}