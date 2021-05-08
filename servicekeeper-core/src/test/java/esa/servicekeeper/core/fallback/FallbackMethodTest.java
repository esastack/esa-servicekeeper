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
package esa.servicekeeper.core.fallback;

import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.exception.RateLimitOverFlowException;
import esa.servicekeeper.core.exception.ServiceKeeperException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.exception.ServiceRetryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class FallbackMethodTest {

    @Test
    void testCommon() throws NoSuchMethodException {
        final FallbackMethod method0 = new FallbackMethod(Foo.class.getDeclaredMethod("method0",
                CircuitBreakerNotPermittedException.class));

        then(method0.isStatic()).isFalse();
        then(method0.isCauseAtFirst()).isTrue();
        then(method0.isMatchFullArgs()).isFalse();
    }

    @Test
    void testCanApplyTo() throws NoSuchMethodException {
        final FallbackMethod method0 = new FallbackMethod(Foo.class.getDeclaredMethod("method0",
                CircuitBreakerNotPermittedException.class));
        then(method0.canApplyTo(CauseType.CIRCUIT_BREAKER)).isTrue();
        then(method0.canApplyTo(CauseType.RATE_LIMIT)).isFalse();
        then(method0.canApplyTo(CauseType.CONCURRENT_LIMIT)).isFalse();
        then(method0.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isFalse();
        then(method0.canApplyTo(CauseType.RETRY)).isFalse();

        final FallbackMethod method1 = new FallbackMethod(Foo.class.getDeclaredMethod("method1",
                RateLimitOverFlowException.class));
        then(method1.canApplyTo(CauseType.CIRCUIT_BREAKER)).isFalse();
        then(method1.canApplyTo(CauseType.RATE_LIMIT)).isTrue();
        then(method1.canApplyTo(CauseType.CONCURRENT_LIMIT)).isFalse();
        then(method1.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isFalse();
        then(method1.canApplyTo(CauseType.RETRY)).isFalse();

        final FallbackMethod method2 = new FallbackMethod(Foo.class.getDeclaredMethod("method2",
                ConcurrentOverFlowException.class));
        then(method2.canApplyTo(CauseType.CIRCUIT_BREAKER)).isFalse();
        then(method2.canApplyTo(CauseType.RATE_LIMIT)).isFalse();
        then(method2.canApplyTo(CauseType.CONCURRENT_LIMIT)).isTrue();
        then(method2.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isFalse();
        then(method2.canApplyTo(CauseType.RETRY)).isFalse();

        final FallbackMethod method3 = new FallbackMethod(Foo.class.getDeclaredMethod("method3",
                ServiceKeeperNotPermittedException.class));
        then(method3.canApplyTo(CauseType.CIRCUIT_BREAKER)).isTrue();
        then(method3.canApplyTo(CauseType.RATE_LIMIT)).isTrue();
        then(method3.canApplyTo(CauseType.CONCURRENT_LIMIT)).isTrue();
        then(method3.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isTrue();
        then(method3.canApplyTo(CauseType.RETRY)).isFalse();

        final FallbackMethod method4 = new FallbackMethod(Foo.class.getDeclaredMethod("method4"));
        then(method4.canApplyTo(CauseType.CIRCUIT_BREAKER)).isTrue();
        then(method4.canApplyTo(CauseType.RATE_LIMIT)).isTrue();
        then(method4.canApplyTo(CauseType.CONCURRENT_LIMIT)).isTrue();
        then(method4.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isTrue();
        then(method4.canApplyTo(CauseType.RETRY)).isTrue();

        final FallbackMethod method5 = new FallbackMethod(Foo.class.getDeclaredMethod("method5",
                ServiceRetryException.class));
        then(method5.canApplyTo(CauseType.CIRCUIT_BREAKER)).isFalse();
        then(method5.canApplyTo(CauseType.RATE_LIMIT)).isFalse();
        then(method5.canApplyTo(CauseType.CONCURRENT_LIMIT)).isFalse();
        then(method5.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isFalse();
        then(method5.canApplyTo(CauseType.RETRY)).isTrue();

        final FallbackMethod method6 = new FallbackMethod(Foo.class.getDeclaredMethod("method6",
                ServiceKeeperException.class));
        then(method6.canApplyTo(CauseType.CIRCUIT_BREAKER)).isTrue();
        then(method6.canApplyTo(CauseType.RATE_LIMIT)).isTrue();
        then(method6.canApplyTo(CauseType.CONCURRENT_LIMIT)).isTrue();
        then(method6.canApplyTo(CauseType.SERVICE_KEEPER_NOT_PERMIT)).isTrue();
        then(method6.canApplyTo(CauseType.RETRY)).isTrue();
    }

    private static class Foo {

        private void method0(CircuitBreakerNotPermittedException ex) {

        }

        private void method1(RateLimitOverFlowException ex) {

        }

        private void method2(ConcurrentOverFlowException ex) {

        }

        private void method3(ServiceKeeperNotPermittedException ex) {

        }

        private void method4() {

        }

        private void method5(ServiceRetryException ex) {

        }

        private void method6(ServiceKeeperException ex) {

        }

    }

}
