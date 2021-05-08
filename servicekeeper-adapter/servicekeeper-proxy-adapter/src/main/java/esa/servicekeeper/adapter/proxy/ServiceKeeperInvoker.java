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

import esa.servicekeeper.core.Bootstrap;
import esa.servicekeeper.core.BootstrapContext;
import esa.servicekeeper.core.annotation.CircuitBreaker;
import esa.servicekeeper.core.annotation.ConcurrentLimiter;
import esa.servicekeeper.core.annotation.RateLimiter;
import esa.servicekeeper.core.annotation.Retryable;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import esa.servicekeeper.core.utils.RequestHandleUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Try to get permission of accessing given resource and execute it if get the permission successfully, and then
 * return value will be returned.
 */
public final class ServiceKeeperInvoker {

    private ServiceKeeperInvoker() {
    }

    /**
     * Try to invoke the original method. Get the immutable config from method's annotations,
     * eg {@link ConcurrentLimiter}, {@link RateLimiter}, {@link CircuitBreaker} and {@link Retryable}.
     *
     * @param method   method
     * @param delegate target object
     * @param args     args
     * @return original return value or fallback result.
     * @throws Throwable any throwable
     */
    public static Object invoke(Method method, Object delegate, Object[] args) throws Throwable {
        return Bootstrap.entry().invoke(method, delegate, args);
    }

    /**
     * Try to execute the original method.
     *
     * @param aliasName alias name
     * @param method    method
     * @param delegate  target object
     * @param args      args
     * @return original return value or fallback result.
     * @throws Throwable any throwable
     */
    public static Object invoke(String aliasName, Method method, Object delegate, Object[] args) throws Throwable {
        return Bootstrap.entry().invoke(aliasName, method, delegate, args);
    }

    /**
     * Try to execute the original method.
     *
     * @param name            name
     * @param immutableConfig immutable config
     * @param callable        callable
     * @param args            args
     * @param <T>             T
     * @return original return value or fallback result.
     * @throws Throwable any throwable
     */
    public static <T> T call(String name, CompositeServiceKeeperConfig immutableConfig,
                             Callable<T> callable, Object[] args) throws Throwable {
        return Bootstrap.entry().call(name, immutableConfig, callable, args);
    }

    /**
     * Note: This interface referenced by dubbo, you must modify carefully.
     *
     * @param name     resourceId
     * @param callable callable
     * @param args     args
     * @param <T>      generic type
     * @return result
     * @throws Throwable any throwable
     */
    public static <T> T call(String name, Callable<T> callable, Object[] args) throws Throwable {
        return Bootstrap.entry().call(name, callable, args);
    }

    /**
     * Try to call the original callable with {@link CompositeServiceKeeperConfig} immutable config and
     * {@link OriginalInvocation}. The original invocation is useful while locating fallback method.
     *
     * @param name                   name
     * @param immutableConfig        immutable config
     * @param originalInvocation original invocation
     * @param callable               callable
     * @param args                   args
     * @param <T>                    T
     * @return original return value or fallback result
     * @throws Throwable any throwable
     */
    public static <T> T call(String name, CompositeServiceKeeperConfig immutableConfig,
                             OriginalInvocation originalInvocation, Callable<T> callable,
                             Object[] args) throws Throwable {
        return Bootstrap.entry().call(name, immutableConfig, originalInvocation,
                callable, args);
    }

    /**
     * Note: this method is useful for spring application.
     *
     * @param name                           name
     * @param immutableConfigSupplier        supplier to get immutable config
     * @param originalInvocation             supplier to get original invocation
     * @param callable                       callable
     * @param args                           args
     * @param <T>                            T
     * @return original return value or fallback result.
     * @throws Throwable any throwable
     */
    public static <T> T call(String name, Supplier<CompositeServiceKeeperConfig> immutableConfigSupplier,
                             Supplier<OriginalInvocation> originalInvocation, Callable<T> callable,
                             Object[] args) throws Throwable {
        return Bootstrap.entry().call(name, immutableConfigSupplier, originalInvocation,
                callable, args);
    }

    /**
     * Run original runnable.
     *
     * @param name                      name
     * @param runnable                  runnable
     * @param args                      args
     * @throws Throwable                any throwable
     */
    public static void run(String name, Runnable runnable, Object[] args) throws Throwable {
        Bootstrap.entry().run(name, runnable, args);
    }

    /**
     * Run original runnable with immutable config.
     *
     * @param name                      name
     * @param immutableConfig           immutable config
     * @param runnable                  runnable
     * @param args                      args
     * @throws Throwable                any throwable
     */
    public static void run(String name, CompositeServiceKeeperConfig immutableConfig,
                           Runnable runnable, Object[] args) throws Throwable {
        Bootstrap.entry().run(name, immutableConfig, runnable, args);
    }

    /**
     * Invoke this method only when you want to custom async result handlers. It' not necessary for you
     * to invoke this method manually. If you don't do this before you firstly tryAsyncInvoke(), the default
     * async result handlers will be empty.
     *
     * @param asyncResultHandlers asyncResultHandlers
     * @see AsyncResultHandler
     */
    public static void init(List<AsyncResultHandler<?>> asyncResultHandlers) {
        Bootstrap.init(BootstrapContext.singleton(asyncResultHandlers));
    }

    /**
     * Handle the requestHandle when not allowed.
     *
     * @param requestHandle request handle
     * @return fallback object
     * @throws Throwable throwable
     */
    public static Object handleWhenNotAllowed(final RequestHandle requestHandle) throws Throwable {
        return RequestHandleUtils.handle(requestHandle);
    }
}
