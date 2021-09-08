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
package io.esastack.servicekeeper.adapter.spring;

import io.esastack.servicekeeper.adapter.spring.aop.DefaultServiceKeeperAop;
import io.esastack.servicekeeper.adapter.spring.aop.WebAutoSupportAop;
import io.esastack.servicekeeper.core.annotation.CircuitBreaker;
import io.esastack.servicekeeper.core.annotation.ConcurrentLimiter;
import io.esastack.servicekeeper.core.annotation.Fallback;
import io.esastack.servicekeeper.core.annotation.RateLimiter;
import io.esastack.servicekeeper.core.annotation.Retryable;
import io.esastack.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import io.esastack.servicekeeper.core.exception.ConcurrentOverflowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.exception.ServiceRetryException;
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
        System.setProperty("servicekeeper.configurators.disable", "true");
        ctx = new AnnotationConfigApplicationContext();
        ctx.register(DefaultServiceKeeperAopTest.class);

        ctx.refresh();
    }

    @Test
    void test() {
        then(ctx.getBean(DefaultServiceKeeperAop.class)).isNotNull();
        assertThrows(NoSuchBeanDefinitionException.class, () -> ctx.getBean(WebAutoSupportAop.class));

        //RateLimiter
        final HelloService service = ctx.getBean(HelloService.class);
        then(service.testRateLimiter()).isEqualTo("testRateLimiter");
        assertThrows(RateLimitOverflowException.class, service::testRateLimiter);

        //ConcurrentLimiter
        new Thread(service::testConcurrentLimiter).start();
        try {
            Thread.sleep(20);
        } catch (InterruptedException ignored) {
        }
        assertThrows(ConcurrentOverflowException.class, service::testConcurrentLimiter);

        //CircuitBreaker
        assertThrows(RuntimeException.class, service::testCircuitBreaker);
        assertThrows(CircuitBreakerNotPermittedException.class, service::testCircuitBreaker);

        //Retry
        assertThrows(ServiceRetryException.class, service::testRetry);

        //FallbackWithoutApplyToBizException
        assertThrows(RuntimeException.class, service::testFallbackWithoutApplyToBizException);
        then(service.testFallbackWithoutApplyToBizException()).isEqualTo("fallback value");

        //FallbackWithApplyToBizException
        //BizException
        then(service.testFallbackWithApplyToBizException()).isEqualTo("fallback value");
        //RateLimitOverflowException
        then(service.testFallbackWithApplyToBizException()).isEqualTo("fallback value");
    }

    public static class HelloService {

        @RateLimiter(1)
        public String testRateLimiter() {
            return "testRateLimiter";
        }

        @ConcurrentLimiter(1)
        public void testConcurrentLimiter() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        @CircuitBreaker(ringBufferSizeInClosedState = 1,
                ringBufferSizeInHalfOpenState = 1,
                waitDurationInOpenState = "10s"
        )
        public void testCircuitBreaker() {
            throw new RuntimeException();
        }

        @Retryable(2)
        public void testRetry() {
            throw new RuntimeException();
        }

        @RateLimiter(1)
        @Fallback(fallbackValue = "fallback value")
        public String testFallbackWithoutApplyToBizException() {
            throw new RuntimeException("error occur");
        }

        @RateLimiter(1)
        @Fallback(fallbackValue = "fallback value", alsoApplyToBizException = true)
        public String testFallbackWithApplyToBizException() {
            throw new RuntimeException("error occur");
        }
    }
}
