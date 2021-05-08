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
package esa.servicekeeper.core.entry;

import esa.servicekeeper.core.common.OriginalInvocation;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * The core entry to execute the target method or callable or runnable and then returns the result which may be real
 * invocation result or fallback result or throws exception. There is another entry designed as
 * {@link ServiceKeeperAsyncEntry} which only try to get the permission to original invocation and then you can
 * get the result of trying permission and ends the request manually.
 */
public interface ServiceKeeperEntry {
    /**
     * Obtain the result of the original method by reflection.
     *
     * @param method original method
     * @param delegate the target object
     * @param args args
     * @return result
     * @throws Throwable any throwable
     */
    Object invoke(Method method, Object delegate, Object[] args) throws Throwable;

    /**
     * Obtain the result of the original method by reflection.
     *
     * @param aliasName     aliasName
     * @param method   method
     * @param delegate delegate object
     * @param args     args
     * @return object
     * @throws Throwable any throwable
     */
    Object invoke(String aliasName, Method method, Object delegate, Object[] args) throws Throwable;

    /**
     * To run a callable and corresponding moats are invoked one by one.
     *
     * @param resourceId            the resourceId of protected resource
     * @param immutableConfig       composite config
     * @param callable        callable
     * @param args            args
     * @param <T>             T
     * @return T
     * @throws Throwable any throwable
     */
    <T> T call(String resourceId, CompositeServiceKeeperConfig immutableConfig,
               Callable<T> callable, Object[] args) throws Throwable;

    /**
     * Try to run a callable around with service keeper's moats.
     *
     * @param resourceId                     resource
     * @param immutableConfig                immutable config
     * @param originalInvocation             original invocation
     * @param callable                       callable
     * @param args                           args
     * @param <T>                            return value type
     * @return return value
     * @throws Throwable throwable
     */
    <T> T call(String resourceId, CompositeServiceKeeperConfig immutableConfig,
               OriginalInvocation originalInvocation, Callable<T> callable, Object[] args)
            throws Throwable;

    /**
     * Try to run a callable around with service keeper's moats.
     *
     * @param resourceId resource
     * @param immutableConfigSupplier immutable config
     * @param originalInvocation  original invocation
     * @param callable callable
     * @param args args
     * @param <T> return value type
     * @return return value
     * @throws Throwable throwable
     */
    <T> T call(String resourceId, Supplier<CompositeServiceKeeperConfig> immutableConfigSupplier,
               Supplier<OriginalInvocation> originalInvocation, Callable<T> callable, Object[] args)
            throws Throwable;

    /**
     * To run a callable and corresponding interceptors are invoked one by one.
     *
     * @param resourceId     resourceId
     * @param callable callable
     * @param args     args
     * @param <T>      T
     * @return T
     * @throws Throwable any throwable
     */
    <T> T call(String resourceId, Callable<T> callable, Object[] args) throws Throwable;

    /**
     * To run a runnable and corresponding interceptors are invoked one by one.
     *
     * @param resourceId            resourceId
     * @param immutableConfig       composite config
     * @param runnable        runnable
     * @param args            args
     * @throws Throwable any throwable
     */
    void run(String resourceId, CompositeServiceKeeperConfig immutableConfig,
             Runnable runnable, Object[] args) throws Throwable;

    /**
     * To run a runnable and corresponding interceptors are invoked one by one.
     *
     * @param resourceId     resourceId
     * @param runnable runnable
     * @param args     args
     * @throws Throwable any throwable
     */
    void run(String resourceId, Runnable runnable, Object[] args) throws Throwable;
}
