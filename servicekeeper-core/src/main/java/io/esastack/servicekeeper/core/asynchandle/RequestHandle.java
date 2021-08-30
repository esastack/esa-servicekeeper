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
package io.esastack.servicekeeper.core.asynchandle;

import io.esastack.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

/**
 * The {@link RequestHandle} holds a handler of a request, and you can get the status and result of the corresponding
 * request. Also, you can end the request by this instance. Be aware that one request has one but only handler.
 */
public interface RequestHandle {

    IllegalStateException ILLEGAL_FALLBACK_EXCEPTION =
            new IllegalStateException("The request is allowed, so the fallback doesn't take effect!");

    RequestHandle DEFAULT_PASS_WITHOUT_OBSTACLE = new RequestHandle() {

        @Override
        public boolean isAllowed() {
            return true;
        }

        @Override
        public ServiceKeeperNotPermittedException getNotAllowedCause() {
            return null;
        }

        @Override
        public void endWithSuccess() {
            // Do nothing
        }

        @Override
        public void endWithError(Throwable throwable) {
            // Do nothing
        }

        @Override
        public Object fallback(Throwable throwable) {
            throw ILLEGAL_FALLBACK_EXCEPTION;
        }

        @Override
        public void endWithResult(Object result) {
            // Do nothing
        }

    };

    /**
     * Whether the request is allowed.
     *
     * @return true or false
     */
    boolean isAllowed();

    /**
     * Try to get the reason of the request is not allowed.
     *
     * @return throwable
     */
    ServiceKeeperNotPermittedException getNotAllowedCause();

    /**
     * End the invocation with success and record the result.
     * Note: The result if for further use, eg: To custom {@link PredicateStrategy}
     *
     * @param result result
     * @see PredicateStrategy#isSuccess(Context)
     */
    void endWithResult(final Object result);

    /**
     * End the invocation with success.
     */
    void endWithSuccess();

    /**
     * End the invocation with error
     *
     * @param throwable throwable
     */
    void endWithError(final Throwable throwable);

    /**
     * End the invocation with error and fallback
     *
     * @throws Throwable error
     */
    Object fallback(final Throwable throwable) throws Throwable;
}
