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

import esa.servicekeeper.core.exception.*;
import esa.servicekeeper.core.executionchain.Context;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackToFunctionTest0 {

    @Test
    void testHandle() throws Throwable {
        final Object foo = new Foo();
        final Set<FallbackMethod> methods0 = new HashSet<>(7);
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method0",
                CircuitBreakerNotPermittedException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method1",
                RateLimitOverflowException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method2",
                ConcurrentOverFlowException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method3",
                ServiceKeeperNotPermittedException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method4", String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method5",
                ServiceRetryException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method6",
                ServiceKeeperException.class, String.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method7",
                CircuitBreakerNotPermittedException.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method8",
                RateLimitOverflowException.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method9",
                ConcurrentOverFlowException.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method10",
                ServiceKeeperNotPermittedException.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method11",
                ServiceRetryException.class)));
        methods0.add(new FallbackMethod(Foo.class.getDeclaredMethod("method12",
                ServiceKeeperException.class)));

        final FallbackToFunction<String> fallback0 = new FallbackToFunction<>(foo, methods0, false);
        final Context context0 = mock(Context.class);
        when(context0.getThroughFailsCause()).thenReturn(new CircuitBreakerNotPermittedException(null, null, null));
        when(context0.getArgs()).thenReturn(new String[]{"LiMing"});

        then(fallback0.handle(context0)).isEqualTo("method0");

        when(context0.getThroughFailsCause()).thenReturn(new RateLimitOverflowException(null, null, null));
        then(fallback0.handle(context0)).isEqualTo("method1");

        when(context0.getThroughFailsCause()).thenReturn(new ConcurrentOverFlowException(null, null, null));
        then(fallback0.handle(context0)).isEqualTo("method2");

        when(context0.getThroughFailsCause()).thenReturn(new ServiceKeeperNotPermittedException(null, null));
        then(fallback0.handle(context0)).isEqualTo("method3");

        when(context0.getThroughFailsCause()).thenReturn(new ServiceRetryException(null, null));
        then(fallback0.handle(context0)).isEqualTo("method5");

        when(context0.getThroughFailsCause()).thenReturn(new ServiceKeeperException(null, null));
        then(fallback0.handle(context0)).isEqualTo("method6");

        final Set<FallbackMethod> methods1 = new HashSet<>(3);
        methods1.add(new FallbackMethod(Foo.class.getDeclaredMethod("method0",
                CircuitBreakerNotPermittedException.class, String.class)));
        methods1.add(new FallbackMethod(Foo.class.getDeclaredMethod("method3",
                ServiceKeeperNotPermittedException.class, String.class)));
        methods1.add(new FallbackMethod(Foo.class.getDeclaredMethod("method4", String.class)));

        final FallbackToFunction<String> fallback1 = new FallbackToFunction<>(foo, methods1, false);
        final Context context1 = mock(Context.class);
        when(context1.getThroughFailsCause()).thenReturn(new CircuitBreakerNotPermittedException(null, null, null));
        when(context1.getArgs()).thenReturn(new String[]{"LiMing"});
        then(fallback1.handle(context1)).isEqualTo("method0");

        when(context1.getThroughFailsCause()).thenReturn(new RateLimitOverflowException(null, null, null));
        then(fallback1.handle(context1)).isEqualTo("method3");

        when(context1.getThroughFailsCause()).thenReturn(new ConcurrentOverFlowException(null, null, null));
        then(fallback1.handle(context1)).isEqualTo("method3");

        when(context1.getThroughFailsCause()).thenReturn(new ServiceKeeperNotPermittedException(null, null));
        then(fallback1.handle(context1)).isEqualTo("method3");


        final Set<FallbackMethod> methods2 = new HashSet<>(2);
        methods2.add(new FallbackMethod(Foo.class.getDeclaredMethod("method0",
                CircuitBreakerNotPermittedException.class, String.class)));
        methods2.add(new FallbackMethod(Foo.class.getDeclaredMethod("method4", String.class)));

        final FallbackToFunction<String> fallback2 = new FallbackToFunction<>(foo, methods2, false);
        final Context context2 = mock(Context.class);
        when(context2.getThroughFailsCause()).thenReturn(new CircuitBreakerNotPermittedException(null, null, null));
        when(context2.getArgs()).thenReturn(new String[]{"LiMing"});
        then(fallback2.handle(context2)).isEqualTo("method0");

        when(context2.getThroughFailsCause()).thenReturn(new RateLimitOverflowException(null, null, null));
        then(fallback2.handle(context2)).isEqualTo("method4");

        when(context2.getThroughFailsCause()).thenReturn(new ConcurrentOverFlowException(null, null, null));
        then(fallback2.handle(context2)).isEqualTo("method4");

        when(context2.getThroughFailsCause()).thenReturn(new ServiceKeeperNotPermittedException(null, null));
        then(fallback2.handle(context2)).isEqualTo("method4");
    }

    private static class Foo {

        private String method0(CircuitBreakerNotPermittedException ex, String name) {
            return "method0";
        }

        private String method1(RateLimitOverflowException ex, String name) {
            return "method1";
        }

        private String method2(ConcurrentOverFlowException ex, String name) {
            return "method2";
        }

        private String method3(ServiceKeeperNotPermittedException ex, String name) {
            return "method3";
        }

        private String method4(String name) {
            return "method4";
        }

        private String method5(ServiceRetryException ex, String name) {
            return "method5";
        }

        private String method6(ServiceKeeperException ex, String name) {
            return "method6";
        }

        private String method7(CircuitBreakerNotPermittedException ex) {
            return "method7";
        }

        private String method8(RateLimitOverflowException ex) {
            return "method8";
        }

        private String method9(ConcurrentOverFlowException ex) {
            return "method9";
        }

        private String method10(ServiceKeeperNotPermittedException ex) {
            return "method10";
        }

        private String method11(ServiceRetryException ex) {
            return "method11";
        }

        private String method12(ServiceKeeperException ex) {
            return "method12";
        }
    }
}
