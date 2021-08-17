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
package esa.servicekeeper.core.factory;

import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.fallback.FallbackHandlerConfig;
import esa.servicekeeper.core.fallback.FallbackToException;
import esa.servicekeeper.core.fallback.FallbackToFunction;
import esa.servicekeeper.core.fallback.FallbackToValue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackHandlerFactoryImplTest {

    private final FallbackHandlerFactory factory = new FallbackHandlerFactoryImpl();
    private final Context ctx = mock(Context.class);

    @Test
    void testFallbackToValue() {
        final String value = "Hello World!";

        // Case1: Return value is super class of String
        FallbackConfig fallbackConfig = FallbackConfig.builder().specifiedValue(value).build();
        OriginalInvocation invocation = new OriginalInvocation(Object.class, new Class[0]);
        FallbackHandlerConfig config = new FallbackHandlerConfig(fallbackConfig, invocation);
        FallbackToValue fallbackToValue = (FallbackToValue) factory.get(config);
        then(fallbackToValue.handle(ctx)).isEqualTo(value);

        // Case2: Return value is String
        invocation = new OriginalInvocation(String.class, new Class[0]);
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        fallbackToValue = (FallbackToValue) factory.get(config);
        then(fallbackToValue.handle(ctx)).isEqualTo(value);

        // Case2: Can't assign String.class from Return value
        invocation = new OriginalInvocation(int.class, new Class[0]);
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        then(factory.get(config)).isNull();
    }

    @Test
    void testFallbackToFunction() throws Throwable {
        // Case1: return value type doesn't match
        FallbackConfig fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass.class)
                .methodName("method4").build();
        OriginalInvocation invocation = new OriginalInvocation(int.class, new Class[0]);
        FallbackHandlerConfig config = new FallbackHandlerConfig(fallbackConfig, invocation);
        FallbackToFunction<?> fallbackToFunction = null;
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }
        then(fallbackToFunction).isEqualTo(null);


        // Case2: ParameterTypes[] doesn't match
        fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass.class)
                .methodName("method6").build();
        invocation = new OriginalInvocation(int.class, new Class[]{int.class});
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }
        then(fallbackToFunction).isEqualTo(null);


        // Case3: method's name of current class
        fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass.class)
                .methodName("method6").build();
        invocation = new OriginalInvocation(int.class, new Class[]{String.class});
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }

        when(ctx.getArgs()).thenReturn(new Object[]{"LiMing"});
        assert fallbackToFunction != null;
        then(fallbackToFunction.handle(ctx)).isEqualTo(0);


        // Case4: method of current class's super class
        fallbackConfig = FallbackConfig.builder()
                .targetClass(SupClass.class)
                .methodName("method2").build();
        invocation = new OriginalInvocation(int.class, new Class[]{String.class});
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }

        when(ctx.getArgs()).thenReturn(new Object[]{"LiMing"});
        then(fallbackToFunction.handle(ctx)).isEqualTo(10);


        // Case5: static method
        fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass.class)
                .methodName("method8").build();
        invocation = new OriginalInvocation(int.class, new Class[]{int.class});
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }

        when(ctx.getArgs()).thenReturn(new Object[]{0});
        then(fallbackToFunction.handle(ctx)).isEqualTo(99);


        // Case6: biz exception
        fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass.class)
                .methodName("method9").build();
        invocation = new OriginalInvocation(int.class, new Class[]{int.class});
        config = new FallbackHandlerConfig(fallbackConfig, invocation);
        try {
            fallbackToFunction = (FallbackToFunction<?>) factory.get(config);
        } catch (Throwable throwable) {
            // Do nothing
        }

        when(ctx.getArgs()).thenReturn(new Object[]{2});
        when(ctx.getBizException()).thenReturn(new RuntimeException());
        then(fallbackToFunction.handle(ctx)).isEqualTo(202);
    }

    @Test
    void testMatchFallbackToFunctionByCause() throws Throwable {
        FallbackConfig fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass1.class)
                .methodName("fallbackMethod").build();

        final OriginalInvocation invocation = new OriginalInvocation(String.class, new Class[0]);
        final FallbackToFunction<?> fallbackToFunction = (FallbackToFunction<?>)
                factory.get(new FallbackHandlerConfig(fallbackConfig, invocation));

        when(ctx.getArgs()).thenReturn(new Object[0]);
        when(ctx.getEnterFailsCause()).thenReturn(new ServiceKeeperNotPermittedException(ctx));
        then(fallbackToFunction.handle(ctx)).isEqualTo("ServiceKeeperNotPermittedException");

        when(ctx.getEnterFailsCause()).thenReturn(new CircuitBreakerNotPermittedException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("CircuitBreakerNotPermittedException");

        when(ctx.getEnterFailsCause()).thenReturn(new ConcurrentOverFlowException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("ConcurrentOverFlowException");

        when(ctx.getEnterFailsCause()).thenReturn(new RateLimitOverflowException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("RateLimitOverflowException");
    }

    @Test
    void testFallbackToException() {
        FallbackConfig fallbackConfig = FallbackConfig.builder()
                .specifiedException(IllegalArgumentException.class).build();
        OriginalInvocation invocation = new OriginalInvocation(Object.class, new Class[0]);
        FallbackHandlerConfig config = new FallbackHandlerConfig(fallbackConfig, invocation);
        FallbackToException fallbackToException = (FallbackToException) factory.get(config);
        assertThrows(IllegalArgumentException.class, () -> fallbackToException.handle(ctx));

        then(factory.get(new FallbackHandlerConfig(FallbackConfig.builder()
                .specifiedException(ServiceKeeperNotPermittedException.class).build(),
                invocation))).isNull();
    }

    @Test
    void testFallbackToFunctionNoArgs() throws Throwable {
        FallbackConfig fallbackConfig = FallbackConfig.builder()
                .targetClass(SubClass1.class)
                .methodName("fallbackMethod").build();

        // Original invocation's return type is new Class[]{String.class, String class} which not matches fallback
        // method's parameter types.
        final OriginalInvocation invocation = new OriginalInvocation(String.class,
                new Class[]{String.class, Object.class});

        final FallbackToFunction<?> fallbackToFunction = (FallbackToFunction<?>)
                factory.get(new FallbackHandlerConfig(fallbackConfig, invocation));

        when(ctx.getArgs()).thenReturn(new Object[0]);
        when(ctx.getEnterFailsCause()).thenReturn(new ServiceKeeperNotPermittedException(ctx));
        then(fallbackToFunction.handle(ctx)).isEqualTo("ServiceKeeperNotPermittedException");

        when(ctx.getEnterFailsCause()).thenReturn(new CircuitBreakerNotPermittedException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("CircuitBreakerNotPermittedException");

        when(ctx.getEnterFailsCause()).thenReturn(new ConcurrentOverFlowException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("ConcurrentOverFlowException");

        when(ctx.getEnterFailsCause()).thenReturn(new RateLimitOverflowException(null, ctx, null));
        then(fallbackToFunction.handle(ctx)).isEqualTo("RateLimitOverflowException");
    }

    @Test
    void testNoFallbackMethod() {
        final FallbackConfig fallbackConfig = FallbackConfig.builder()
                .targetClass(SupClass.class)
                .methodName("method").build();

        then(factory.get(new FallbackHandlerConfig(fallbackConfig, null))).isNull();
    }

    @Test
    void testInstantiateFallbackClassFails() {
        final FallbackConfig fallbackConfig = FallbackConfig.builder()
                .targetClass(SupClass0.class)
                .methodName("method").build();

        then(factory.get(new FallbackHandlerConfig(fallbackConfig, null))).isNull();
    }

    @Test
    void testInstantiateFallbackExceptionFails() {
        final FallbackConfig fallbackConfig = FallbackConfig.builder()
                .specifiedException(InstantiateException.class)
                .build();

        then(factory.get(new FallbackHandlerConfig(fallbackConfig, null))).isNull();
    }

    public static class SupClass {

        public String method0() {
            return "";
        }

        public int method1() {
            return 0;
        }

        public int method2(String name) {
            return 10;
        }

        public int method3(int age) {
            return 0;
        }
    }

    public static class SubClass extends SupClass {
        public int method9(Throwable bizError, int i) {
            return i + 200;
        }

        public static int method8(int age) {
            return 99;
        }

        public String method4() {
            return "";
        }

        public int method5() {
            return 0;
        }

        public int method6(String name) {
            return 0;
        }

        public int method7(int age) {
            return 0;
        }
    }

    public static class SubClass1 {

        private static String fallbackMethod() {
            return "";
        }

        private String fallbackMethod(ServiceKeeperNotPermittedException ex) {
            return "ServiceKeeperNotPermittedException";
        }

        private String fallbackMethod(CircuitBreakerNotPermittedException ex) {
            return "CircuitBreakerNotPermittedException";
        }

        private String fallbackMethod(RateLimitOverflowException ex) {
            return "RateLimitOverflowException";
        }

        private String fallbackMethod(ConcurrentOverFlowException ex) {
            return "ConcurrentOverFlowException";
        }
    }

    public static class SupClass0 {
        private SupClass0() {
        }

        public int method() {
            return 0;
        }
    }

    public static class InstantiateException extends RuntimeException {
        public InstantiateException(String msg) {
            super(msg);
        }
    }
}
