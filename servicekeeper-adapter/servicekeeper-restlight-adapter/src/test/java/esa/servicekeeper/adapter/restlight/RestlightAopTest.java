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
package esa.servicekeeper.adapter.restlight;

import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.DeleteMapping;
import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.GetMapping;
import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PatchMapping;
import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PostMapping;
import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PutMapping;
import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.RequestMapping;
import esa.servicekeeper.adapter.restlight.aop.RestlightAutoSupportAop;
import esa.servicekeeper.adapter.spring.aop.DefaultServiceKeeperAop;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
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
@Import({RestlightAutoSupportConfigurator.class})
public class RestlightAopTest {

    private static AnnotationConfigApplicationContext ctx;

    @Bean
    public HelloService helloService() {
        return new HelloService();
    }

    @BeforeAll
    static void setUp() {
        System.setProperty("servicekeeper.configurators.disable", "true");
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(RestlightAopTest.class);

        ctx.refresh();
    }

    @Test
    void test() {
        then(ctx.getBean(RestlightAutoSupportAop.class)).isNotNull();
        assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(DefaultServiceKeeperAop.class));

        final HelloService service = ctx.getBean(HelloService.class);
        then(service.testRequest()).isEqualTo("Request");
        assertThrows(RateLimitOverflowException.class, service::testRequest);

        then(service.testGet()).isEqualTo("Get");
        assertThrows(RateLimitOverflowException.class, service::testGet);

        then(service.testPost()).isEqualTo("Post");
        assertThrows(RateLimitOverflowException.class, service::testPost);

        then(service.testPut()).isEqualTo("Put");
        assertThrows(RateLimitOverflowException.class, service::testPut);

        then(service.testDelete()).isEqualTo("Delete");
        assertThrows(RateLimitOverflowException.class, service::testDelete);

        then(service.testPatch()).isEqualTo("Patch");
        assertThrows(RateLimitOverflowException.class, service::testPatch);
    }

    public static class HelloService {

        @RequestMapping
        public String testRequest() {
            return "Request";
        }

        @GetMapping
        public String testGet() {
            return "Get";
        }

        @PostMapping
        public String testPost() {
            return "Post";
        }

        @PutMapping
        public String testPut() {
            return "Put";
        }

        @DeleteMapping
        public String testDelete() {
            return "Delete";
        }

        @PatchMapping
        public String testPatch() {
            return "Patch";
        }
    }
}
