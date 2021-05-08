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
package esa.servicekeeper.core.execution;

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.executionchain.ExecutionChain;

/**
 * AsyncResultHandler is designed to handle async result value.
 *
 * @see esa.servicekeeper.core.asynchandle.AsyncResultHandler
 * @deprecated since 1.4.0
 */
@Deprecated
public interface AsyncResultHandler<T> {

    /**
     * Whether current handler supports the returnType.
     *
     * @param returnType returnType
     * @return true or false
     */
    boolean supports(Class<?> returnType);

    /**
     * Real return value.
     * <p>
     * Note: you need to clean the moats and moatChain manually.
     * eg: {@link AsyncExecutionChain#endWithSuccess(Context)}
     *
     * @param returnValue   return value
     * @param requestHandle RequestHandle
     * @return the result value after handing, which is the result to return.
     * @see ExecutionChain#endWithSuccess(Context)
     */
    T handle(T returnValue, RequestHandle requestHandle);
}
