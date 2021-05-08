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
package esa.servicekeeper.core.executionchain;

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.moats.Moat;

/**
 * The execution chain is designed to ensure that you can access the resource securely, which is made up with many
 * {@link Moat}s which are constructed to protect original resource.
 */
public interface ExecutionChain {

    IllegalStateException REQUEST_NOT_START_EXCEPTION =
            new IllegalStateException("Can't end a request which hasn't started");

    /**
     * Whether access the original resource is allowed?
     * <p>
     * Note: if the fallback logic is triggered take effect, the fallback result can got by
     * {@link Context#getResult()}.
     * Note: When the result is false, you needn't call {@link #endWithError(Context, Throwable)} manually,
     * but if you do as it, nothing will happen.
     *
     * @param ctx Context
     * @return The request handle.
     */
    RequestHandle tryToExecute(Context ctx);

    /**
     * End and clean Async execute.
     *
     * @param ctx ctx.
     */
    void endWithSuccess(Context ctx);

    /**
     * End the invocation with result.
     *
     * @param ctx    ctx
     * @param result result
     */
    void endWithResult(Context ctx, Object result);

    /**
     * End execute with error.
     *
     * @param ctx       ctx
     * @param throwable throwable
     */
    void endWithError(Context ctx, Throwable throwable);
}
