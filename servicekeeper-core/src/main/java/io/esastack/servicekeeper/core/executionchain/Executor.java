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
package io.esastack.servicekeeper.core.executionchain;

import io.esastack.servicekeeper.core.common.OriginalInvocation;

/**
 * Executor to real execute final method,Only supports Retry Now
 */
public interface Executor {

    /**
     * Like the Aop around,do some work before and after execute
     *
     * @param ctx        internal
     * @param invocation original invocation
     * @param executable executable
     * @return the executable result
     * @throws Throwable Throwable
     */
    <R> R doExecute(Context ctx, OriginalInvocation invocation, Executable<R> executable) throws Throwable;

    /**
     * Like the Aop around,do some work before and after run
     *
     * @param ctx        internal
     * @param invocation original invocation
     * @param runnable   runnable
     * @throws Throwable Throwable
     */
    void doExecute(Context ctx, OriginalInvocation invocation, Runnable runnable) throws Throwable;
}

