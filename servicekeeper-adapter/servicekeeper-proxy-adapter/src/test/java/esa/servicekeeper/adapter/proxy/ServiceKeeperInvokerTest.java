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
package esa.servicekeeper.adapter.proxy;

import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.CompletableStageHandler;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceKeeperInvokerTest {

    @BeforeAll
    static void setUp() {
        System.setProperty("servicekeeper.configurators.disable", "true");
        List<AsyncResultHandler<?>> handlers = new ArrayList<>(1);
        handlers.add(new CompletableStageHandler<>());
        ServiceKeeperInvoker.init(handlers);
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testInvoke() throws Throwable {

        HelloService helloService = new HelloServiceHasInterface();
        Method helloMethod = HelloService.class.getDeclaredMethod("helloV1");
        then(ServiceKeeperInvoker.invoke(helloMethod,
                helloService, null)).isEqualTo(HelloServiceHasInterface.HELLO);

        assertThrows(RateLimitOverflowException.class, () ->
                ServiceKeeperInvoker.invoke(helloMethod,
                        helloService, null));

        String alias = "alias";
        then(ServiceKeeperInvoker.invoke(alias, helloMethod,
                helloService, null)).isEqualTo(HelloServiceHasInterface.HELLO);
        assertThrows(RateLimitOverflowException.class, () ->
                ServiceKeeperInvoker.invoke(alias, helloMethod,
                        helloService, null));
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testCall() throws Throwable {

        String name = "call";
        Callable<String> callable = () -> HelloServiceHasInterface.HELLO;
        then(ServiceKeeperInvoker.call(name,
                callable, null)).isEqualTo(HelloServiceHasInterface.HELLO);

        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperInvoker.call(name,
                callable, null));

        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperInvoker.call(name,
                null, callable, null));

        assertThrows(RateLimitOverflowException.class, () ->
                ServiceKeeperInvoker.call(name, () -> null,
                        null, callable, null));

        assertThrows(RateLimitOverflowException.class, () ->
                ServiceKeeperInvoker.call(name, CompositeServiceKeeperConfig.builder().build(),
                        new OriginalInvocation(String.class, new Class[]{String.class}), callable, null));
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testRun() {

        String name = "run";
        Runnable runnable = () -> {
        };
        assertDoesNotThrow(() -> ServiceKeeperInvoker.run(name,
                runnable, null));

        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperInvoker.run(name,
                runnable, null));

        String name2 = "run2";
        assertDoesNotThrow(() -> ServiceKeeperInvoker.run(name2,
                null, runnable, null));

        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperInvoker.run(name2,
                null, runnable, null));
    }

}
