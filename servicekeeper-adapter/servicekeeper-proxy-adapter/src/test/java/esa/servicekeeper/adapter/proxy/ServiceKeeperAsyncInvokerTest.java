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

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.OriginalInvocationInfo;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceKeeperAsyncInvokerTest {
    static final String TEST_TRY_ASYNC_INVOKE_WITH_CONFIG = "testTryAsyncInvokeWithConfig";
    static final String TEST_TRY_ASYNC_INVOKE_WITH_ALL = "testTryAsyncInvokeWithAll";

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testTryAsyncInvoke() {
        String resourceId = "testTryAsyncInvoke";
        RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(resourceId, (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(resourceId, (Object) null);
        then(handle.isAllowed()).isFalse();
        then(handle.getNotAllowedCause()).isInstanceOf(RateLimitOverflowException.class);
        RequestHandle finalHandle = handle;
        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperAsyncInvoker.handleWhenNotAllowed(finalHandle));
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testTryAsyncInvokeWithOriginalInvocation() {
        String resourceId = "testTryAsyncInvokeWithOriginalInvocation";
        RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithOriginalInvocation(resourceId,
                new OriginalInvocation(String.class, new Class[0]), (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithOriginalInvocation(resourceId,
                new OriginalInvocation(String.class, new Class[0]), (Object) null);
        then(handle.isAllowed()).isFalse();
        then(handle.getNotAllowedCause()).isInstanceOf(RateLimitOverflowException.class);
        RequestHandle finalHandle = handle;
        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperAsyncInvoker.handleWhenNotAllowed(finalHandle));
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testTryAsyncInvokeWithOriginalInfo() {
        String resourceId = "testTryAsyncInvokeWithOriginalInfo";
        RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithOriginalInfo(resourceId,
                new OriginalInvocationInfo(String.class, new Class[0]), (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithOriginalInfo(resourceId,
                new OriginalInvocationInfo(String.class, new Class[0]), (Object) null);
        then(handle.isAllowed()).isFalse();
        then(handle.getNotAllowedCause()).isInstanceOf(RateLimitOverflowException.class);
        RequestHandle finalHandle = handle;
        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperAsyncInvoker.handleWhenNotAllowed(finalHandle));
    }

    @Test
    void testTryAsyncInvokeWithConfig() {
        RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithConfig(TEST_TRY_ASYNC_INVOKE_WITH_CONFIG,
                CompositeServiceKeeperConfig.builder().methodConfig(
                        ServiceKeeperConfig.builder().rateLimiterConfig(
                                RateLimitConfig.builder().limitForPeriod(2).build())
                                .build())
                        .build(), (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(TEST_TRY_ASYNC_INVOKE_WITH_CONFIG, (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(TEST_TRY_ASYNC_INVOKE_WITH_CONFIG, (Object) null);
        then(handle.isAllowed()).isFalse();
        then(handle.getNotAllowedCause()).isInstanceOf(RateLimitOverflowException.class);
        RequestHandle finalHandle = handle;
        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperAsyncInvoker.handleWhenNotAllowed(finalHandle));
    }

    @Test
    void testTryAsyncInvokeWithAll() {
        RequestHandle handle = ServiceKeeperAsyncInvoker.tryAsyncInvokeWithAll(TEST_TRY_ASYNC_INVOKE_WITH_ALL,
                CompositeServiceKeeperConfig.builder().methodConfig(
                        ServiceKeeperConfig.builder().rateLimiterConfig(
                                RateLimitConfig.builder().limitForPeriod(2).build())
                                .build())
                        .build(), new OriginalInvocation(String.class, new Class[0]), (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(TEST_TRY_ASYNC_INVOKE_WITH_ALL, (Object) null);
        then(handle.isAllowed()).isTrue();
        handle = ServiceKeeperAsyncInvoker.tryAsyncInvoke(TEST_TRY_ASYNC_INVOKE_WITH_ALL, (Object) null);
        then(handle.isAllowed()).isFalse();
        then(handle.getNotAllowedCause()).isInstanceOf(RateLimitOverflowException.class);
        RequestHandle finalHandle = handle;
        assertThrows(RateLimitOverflowException.class, () -> ServiceKeeperAsyncInvoker.handleWhenNotAllowed(finalHandle));
    }

}
