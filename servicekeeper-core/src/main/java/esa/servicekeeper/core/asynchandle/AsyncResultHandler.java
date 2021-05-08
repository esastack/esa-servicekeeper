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
package esa.servicekeeper.core.asynchandle;

import esa.servicekeeper.core.exception.ServiceKeeperException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.executionchain.ExecutionChain;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

/**
 * The class is designed to handle async result value. In fact, the implementation must invoke
 * {@link RequestHandle}'s ends methods manually and you can use {@link #supports(Class)} to judge whether
 * apply current handler to specified return value type.
 */
public interface AsyncResultHandler<T> {

    ServiceKeeperException ASYNC_RETURN_VALUE_IS_NULL = new ServiceKeeperException("Async return value is null");

    /**
     * Whether current handler supports the returnType.
     *
     * @param returnType returnType
     * @return true or false
     */
    boolean supports(Class<?> returnType);

    /**
     * Handles the real return value and return.
     *
     * <p>
     * Note: you must clean and release the resource occupied by current invocation by {@link RequestHandle}. If you
     * don't do it, a major error will occur, because that there is no another way to release the resource of
     * concurrent counter and record current result. Generally, there are three ways to release the resource, eg:
     *
     * 1. {@link RequestHandle#endWithSuccess()} ends the invocation successfully.
     * 2. {@link RequestHandle#endWithError(Throwable)} ends the invocation with error.
     * 3. {@link RequestHandle#endWithResult(Object)} ends the invocation with specified result. It's only useful
     *    when you want to custom {@link PredicateStrategy} and needs to implements
     *    {@link PredicateStrategy#isSuccess(Context)} by {@link Context#getResult()}.
     *
     * @param returnValue   return value
     * @param requestHandle RequestHandle
     * @return the result value after handing, which is the result to return.
     * @see ExecutionChain#endWithSuccess(Context)
     */
    default T handle(T returnValue, RequestHandle requestHandle) {
        if (returnValue == null) {
            requestHandle.endWithError(ASYNC_RETURN_VALUE_IS_NULL);
            return null;
        }

        return handle0(returnValue, requestHandle);
    }

    /**
     * Handle the real return value and return, the return value is impossible to be null.
     *
     * @param returnValue   return value
     * @param requestHandle request handle
     * @return value
     */
    T handle0(T returnValue, RequestHandle requestHandle);
}
