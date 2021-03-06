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
package io.esastack.servicekeeper.adapter.proxy;

import io.esastack.servicekeeper.core.Bootstrap;
import io.esastack.servicekeeper.core.BootstrapContext;
import io.esastack.servicekeeper.core.asynchandle.AsyncResultHandler;
import io.esastack.servicekeeper.core.asynchandle.RequestHandle;
import io.esastack.servicekeeper.core.common.OriginalInvocation;
import io.esastack.servicekeeper.core.common.OriginalInvocationInfo;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;

import java.util.Collections;
import java.util.List;

/**
 * The class is designed to try getting a permission of accessing given resource. Whether the permission is allowed
 * or not will be wrapped in {@link RequestHandle}. Be careful of that you must end the {@link RequestHandle} manually.
 */
public final class ServiceKeeperAsyncInvoker {

    private ServiceKeeperAsyncInvoker() {
    }

    /**
     * Try to get permission to access the given {@link String} resourceId, and the {@link RequestHandle} will be
     * returned. If the {@link RequestHandle#isAllowed()} is true, you can do your business invocation continuously,
     * otherwise you can get the reason of not allowed to fallback result by {@link RequestHandle#getNotAllowedCause()}
     * and {@link RequestHandle#fallback(Throwable)} ()}. Last but most importantly, you must callback the
     * {@link RequestHandle#endWithSuccess()} or {@link RequestHandle#endWithResult(Object)} ()} explicitly when the
     * execution is successful.Otherwise, you must callback the {@link RequestHandle#endWithError(Throwable)} or
     * {@link RequestHandle#fallback(Throwable)} ()} to end the current {@link RequestHandle}.
     * <p>
     * eg:
     *
     * <pre>
     *     {@code
     *          RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke("abc", new Object[0]);
     *          if (handle.isAllowed()) {
     *              // Do your business
     *              try {
     *                  doSomething();
     *                  handle.endWithSuccess();      // endWithResult(Object) is also allowed.
     *              } catch(Throwable throwable) {
     *                  handle.fallback(throwable);  //or handle.endWithError(throwable)
     *              }
     *          } else {
     *              handle.fallback(handle.getNotAllowedCause());
     *              //or ServiceKeeperAsyncInvoker.handleWhenNotAllowed(requestHandle)
     *          }
     *     }
     * </pre>
     *
     * @param resourceId resourceId
     * @param args       args
     * @return RequestHandle
     */
    public static RequestHandle tryAsyncInvoke(String resourceId, Object... args) {
        return Bootstrap.asyncEntry()
                .tryAsyncExecute(resourceId, null, null, args);
    }

    /**
     * Try to get permission to access the given {@link String} resourceId with {@link OriginalInvocation},
     * Which will be used while locating fallback method. The fallback method's return type must assign from
     * {@link OriginalInvocation#getReturnType()} and fallback method's parameter types should as same as
     * original method's parameter types.
     *
     * @param resourceId         resourceId
     * @param originalInvocation original invocation. It's helpful to locate fallback method.
     * @param args               args
     * @return Request handle
     */
    public static RequestHandle tryAsyncInvokeWithOriginalInvocation(String resourceId,
                                                                     OriginalInvocation originalInvocation,
                                                                     Object... args) {
        return Bootstrap.asyncEntry()
                .tryAsyncExecute(resourceId, originalInvocation, args);
    }

    /**
     * Try to get permission to access the given {@link String} resourceId, {@link OriginalInvocation} and args.
     *
     * @param resourceId             resourceId
     * @param originalInvocationInfo original invocation.
     * @param args                   args
     * @return request handle
     * @see #tryAsyncInvokeWithOriginalInvocation
     * @deprecated since 1.4.0
     */
    @Deprecated
    public static RequestHandle tryAsyncInvokeWithOriginalInfo(String resourceId,
                                                               OriginalInvocationInfo originalInvocationInfo,
                                                               Object... args) {
        return Bootstrap.asyncEntry()
                .tryAsyncExecute(resourceId, originalInvocationInfo, args);
    }

    /**
     * Try to get permission to access the given {@link String} resourceId with {@link CompositeServiceKeeperConfig}.
     *
     * @param resourceId      resourceId
     * @param immutableConfig immutable config
     * @param args            args
     * @return Request handle
     */
    public static RequestHandle tryAsyncInvokeWithConfig(String resourceId,
                                                         CompositeServiceKeeperConfig immutableConfig,
                                                         Object... args) {
        return Bootstrap.asyncEntry()
                .tryAsyncExecute(resourceId, immutableConfig, null, args);
    }

    /**
     * Try to get permission to access the given resourceId with {@link CompositeServiceKeeperConfig} and
     * {@link OriginalInvocation}.
     *
     * @param resourceId         resourceId
     * @param immutableConfig    immutable config
     * @param originalInvocation original invocation
     * @param args               args
     * @return Request handle
     * @see #tryAsyncInvokeWithConfig(String, CompositeServiceKeeperConfig, Object...)
     * @see #tryAsyncInvokeWithOriginalInvocation(String, OriginalInvocation, Object...)
     */
    public static RequestHandle tryAsyncInvokeWithAll(String resourceId,
                                                      CompositeServiceKeeperConfig immutableConfig,
                                                      OriginalInvocation originalInvocation,
                                                      Object... args) {
        return Bootstrap.asyncEntry()
                .tryAsyncExecute(resourceId, immutableConfig, originalInvocation, args);
    }

    /**
     * Add a listener to service-keeper.properties when the method is invoked,
     * If you don't do this explicitly, the listener only will be added
     * when such as {@link #tryAsyncInvoke(String, Object...)},
     * {@link #tryAsyncInvokeWithAll(String, CompositeServiceKeeperConfig, OriginalInvocation, Object...)}
     * is firstly invoked. It's highly recommended to invoke this method as before as possible.
     */
    public static void init() {
        Bootstrap.init(BootstrapContext.singleton(Collections.emptyList()));
    }

    /**
     * Initializes the {@link Bootstrap} with supplied {@link AsyncResultHandler}s.
     *
     * @param asyncResultHandlers asyncResultHandlers
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
        return requestHandle.fallback(requestHandle.getNotAllowedCause());
    }
}
