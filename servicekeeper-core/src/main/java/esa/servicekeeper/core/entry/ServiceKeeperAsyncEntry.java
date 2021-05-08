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

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

/**
 * The core entry to try to get the permission of accessing original resource and the result are all wrapped in the
 * return value designed as {@link RequestHandle}. You should use this way carefully, because that you must end the
 * request manually by {@link RequestHandle#endWithError(Throwable)} if any exceptions has thrown,
 * {@link RequestHandle#endWithResult(Object)} if your custom {@link PredicateStrategy} needs the original result,
 * or just {@link RequestHandle#endWithSuccess()}. In fact, if you end the request by
 * {@link RequestHandle#endWithResult(Object)} then the result will be saved in {@link Context} and you can get it by
 * {@link Context#getResult()} for further use. There is another similar entry designed
 * as {@link ServiceKeeperAsyncEntry} and you can get more information about it and the difference between the two by
 * it directly.
 */
public interface ServiceKeeperAsyncEntry {
    /**
     * Try to execute a async invocation.
     *
     * @param resourceId resourceId
     * @param originalInvocation original invocation
     * @param args args
     * @return The RequestHandle
     */
    RequestHandle tryAsyncExecute(String resourceId, OriginalInvocation originalInvocation,
                                  Object... args);

    /**
     * Try to execute a async invocation.
     *
     * @param resourceId resourceId
     * @param originalInvocation             original invocation
     * @param immutableConfig                original invocation
     * @param args args
     * @return The RequestHandle
     */
    RequestHandle tryAsyncExecute(String resourceId, CompositeServiceKeeperConfig immutableConfig,
                                  OriginalInvocation originalInvocation, Object... args);
}
