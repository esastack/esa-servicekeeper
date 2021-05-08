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
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;

/**
 * Handler which is used when the access to original resource is not allowed.
 */
public class NotAllowedRequestHandle implements RequestHandle {

    private static final IllegalStateException ILLEGAL_END_EXCEPTION =
            new IllegalStateException("The request hasn't begin, and the end operation is illegal!");

    private final Object fallbackResult;
    private final ServiceKeeperException notAllowedCause;
    private final boolean isFallbackSucceed;
    private final Throwable fallbackFailsCause;

    public NotAllowedRequestHandle(Object fallbackResult, ServiceKeeperException notAllowedCause,
                                   boolean isFallbackSucceed, Throwable fallbackFailsCause) {
        this.fallbackResult = fallbackResult;
        this.notAllowedCause = notAllowedCause;
        this.isFallbackSucceed = isFallbackSucceed;
        this.fallbackFailsCause = fallbackFailsCause;
    }

    @Override
    public boolean isAllowed() {
        return false;
    }

    @Override
    public Object getFallbackResult() {
        return fallbackResult;
    }

    @Override
    public ServiceKeeperNotPermittedException getNotAllowedCause() {
        if (notAllowedCause instanceof ServiceKeeperNotPermittedException) {
            return (ServiceKeeperNotPermittedException) notAllowedCause;
        }
        throw new IllegalStateException("Unmatched exception type", notAllowedCause);
    }

    @Override
    public void endWithSuccess() {
        throw ILLEGAL_END_EXCEPTION;
    }

    @Override
    public void endWithResult(Object result) {
        throw ILLEGAL_END_EXCEPTION;
    }

    @Override
    public void endWithError(Throwable throwable) {
        throw ILLEGAL_END_EXCEPTION;
    }

    @Override
    public boolean isFallbackSucceed() {
        return isFallbackSucceed;
    }

    @Override
    public Throwable getFallbackFailsCause() {
        return fallbackFailsCause;
    }
}
