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

import java.util.function.Supplier;

public interface SyncExecutionChain extends ExecutionChain {

    /**
     * Just execute the executable when the {@link #tryToExecute(Context)} return true.
     *
     * @param ctx        Context
     * @param invocation the supplier to get original invocation
     * @param executable executable
     * @param <R>        R
     * @return R
     * @throws Throwable any throwable
     */
    <R> R execute(Context ctx, Supplier<OriginalInvocation> invocation, Executable<R> executable)
            throws Throwable;

    /**
     * Just execute the runnable when the {@link #tryToExecute(Context)} return true.
     *
     * @param ctx        Context
     * @param invocation the supplier to get original invocation
     * @param runnable   runnable
     * @throws Throwable any throwable
     */
    void execute(Context ctx, Supplier<OriginalInvocation> invocation, Runnable runnable)
            throws Throwable;
}

