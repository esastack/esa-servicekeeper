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

import esa.commons.Checks;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.AsyncExecutionChain;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.fallback.FallbackHandler;

import java.util.concurrent.atomic.AtomicReference;

public class RequestHandleImpl implements RequestHandle {

    private static final IllegalStateException REPEAT_END_EXCEPTION =
            new IllegalStateException("The request has ended!");

    private final Context ctx;
    private final AtomicReference<AsyncExecutionChain> executionChain;
    private final FallbackHandler<?> fallbackHandler;
    private final boolean isAllow;
    private final ServiceKeeperNotPermittedException notAllowCause;

    private RequestHandleImpl(AsyncExecutionChain executionChain, Context ctx,
                              FallbackHandler<?> fallbackHandler, boolean isAllow,
                              ServiceKeeperNotPermittedException notAllowCause) {
        this.executionChain = new AtomicReference<>(executionChain);
        this.ctx = ctx;
        this.fallbackHandler = fallbackHandler;
        this.isAllow = isAllow;
        this.notAllowCause = notAllowCause;
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
    public boolean isAllowed() {
        return isAllow;
    }

    @Override
    public ServiceKeeperNotPermittedException getNotAllowedCause() {
        return notAllowCause;
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

    public void endWithError(final Throwable throwable) {
        Checks.checkNotNull(throwable, "throwable");

        AsyncExecutionChain chain = executionChain.getAndUpdate((pre) -> null);
        if (chain == null) {
            throw REPEAT_END_EXCEPTION;
        } else {
            chain.endWithError(ctx, throwable);
        }
    }

    @Override
    public Object fallback(Throwable cause) throws Throwable {
        endWithError(cause);
        if (fallbackHandler == null) {
            throw cause;
        }
        if (fallbackHandler.alsoApplyToBizException()
                || (cause instanceof ServiceKeeperNotPermittedException)
        ) {
            return fallbackHandler.handle(ctx);
        }
        throw cause;
    }

    public Context getCtx() {
        return ctx;
    }

    public static RequestHandleImpl createAllowHandle(AsyncExecutionChain executionChain, Context ctx,
                                                      FallbackHandler<?> fallbackHandler) {
        return new RequestHandleImpl(executionChain, ctx, fallbackHandler, true, null);
    }

    public static RequestHandleImpl createNotAllowHandle(AsyncExecutionChain executionChain, Context ctx,
                                                         FallbackHandler<?> fallbackHandler,
                                                         ServiceKeeperNotPermittedException notAllowCause) {
        return new RequestHandleImpl(executionChain, ctx, fallbackHandler, false, notAllowCause);
    }
}
