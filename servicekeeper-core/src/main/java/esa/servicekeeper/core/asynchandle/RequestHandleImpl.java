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

import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.Context;

import java.util.concurrent.atomic.AtomicReference;

public class RequestHandleImpl implements RequestHandle {

    private static final IllegalStateException REPEAT_END_EXCEPTION =
            new IllegalStateException("The request has ended!");

    private final Context ctx;
    private final AtomicReference<AsyncExecutionChain> executionChain;

    public RequestHandleImpl(AsyncExecutionChain executionChain, Context ctx) {
        this.executionChain = new AtomicReference<>(executionChain);
        this.ctx = ctx;
    }

    @Override
    public boolean isAllowed() {
        return true;
    }

    @Override
    public Object getFallbackResult() {
        throw ILLEGAL_FALLBACK_EXCEPTION;
    }

    @Override
    public ServiceKeeperNotPermittedException getNotAllowedCause() {
        throw ILLEGAL_GET_NOT_ALLOWED_CAUSE_EXCEPTION;
    }

    @Override
    public boolean isFallbackSucceed() {
        throw ILLEGAL_FALLBACK_EXCEPTION;
    }

    @Override
    public Throwable getFallbackFailsCause() {
        throw ILLEGAL_FALLBACK_EXCEPTION;
    }

    @Override
    public void endWithSuccess() {
        AsyncExecutionChain chain = executionChain.getAndUpdate((pre) -> null);
        if (chain == null) {
            throw REPEAT_END_EXCEPTION;
        } else {
            chain.endWithSuccess(ctx);
        }
    }

    @Override
    public void endWithResult(final Object result) {
        AsyncExecutionChain chain = executionChain.getAndUpdate((pre) -> null);
        if (chain == null) {
            throw REPEAT_END_EXCEPTION;
        } else {
            chain.endWithResult(ctx, result);
        }
    }

    @Override
    public void endWithError(final Throwable throwable) {
        AsyncExecutionChain chain = executionChain.getAndUpdate((pre) -> null);

        if (chain == null) {
            throw REPEAT_END_EXCEPTION;
        } else {
            chain.endWithError(ctx, throwable);
        }
    }

    public Context getCtx() {
        return ctx;
    }
}
