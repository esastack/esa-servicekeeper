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

import esa.commons.Checks;
import io.esastack.servicekeeper.core.exception.ServiceKeeperException;
import io.esastack.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import io.esastack.servicekeeper.core.executionchain.AsyncExecutionChain;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.executionchain.ExecutionChain;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;

import java.util.concurrent.atomic.AtomicReference;

public class RequestHandleImpl implements RequestHandle {

    private static final IllegalStateException REPEAT_END_EXCEPTION =
            new IllegalStateException("The request has ended!");

    private final Context ctx;
    private final AtomicReference<ExecutionChain> executionChain;
    private final FallbackHandler<?> fallbackHandler;
    private final ServiceKeeperNotPermittedException notAllowCause;

    private RequestHandleImpl(ExecutionChain executionChain,
                              Context ctx,
                              FallbackHandler<?> fallbackHandler,
                              ServiceKeeperNotPermittedException notAllowCause) {
        this.executionChain = new AtomicReference<>(executionChain);
        this.ctx = ctx;
        this.fallbackHandler = fallbackHandler;
        this.notAllowCause = notAllowCause;
    }

    @Override
    public void endWithSuccess() {
        ExecutionChain chain = tryGetAndUpdateChain();
        chain.endWithSuccess(ctx);
    }

    @Override
    public boolean isAllowed() {
        return notAllowCause == null;
    }

    @Override
    public ServiceKeeperNotPermittedException getNotAllowedCause() {
        return notAllowCause;
    }

    @Override
    public void endWithResult(Object result) {
        ExecutionChain chain = tryGetAndUpdateChain();
        chain.endWithResult(ctx, result);
    }

    @Override
    public void endWithError(Throwable throwable) {
        Checks.checkNotNull(throwable, "throwable");

        ExecutionChain chain = tryGetAndUpdateChain();
        chain.endWithError(ctx, throwable);
    }

    @Override
    public Object fallback(Throwable cause) throws Throwable {
        Checks.checkNotNull(cause, "throwable");
        endWithError(cause);
        if (fallbackHandler == null) {
            throw cause;
        }
        if ((cause instanceof ServiceKeeperException)
                || fallbackHandler.alsoApplyToBizException()) {
            return fallbackHandler.handle(ctx);
        }
        throw cause;
    }

    public Context getCtx() {
        return ctx;
    }

    public static RequestHandleImpl createAllowHandle(AsyncExecutionChain executionChain,
                                                      Context ctx,
                                                      FallbackHandler<?> fallbackHandler) {
        return new RequestHandleImpl(executionChain, ctx, fallbackHandler, null);
    }

    public static RequestHandleImpl createNotAllowHandle(AsyncExecutionChain executionChain,
                                                         Context ctx,
                                                         FallbackHandler<?> fallbackHandler,
                                                         ServiceKeeperNotPermittedException notAllowCause) {
        return new RequestHandleImpl(executionChain, ctx, fallbackHandler, notAllowCause);
    }

    private ExecutionChain tryGetAndUpdateChain() {
        ExecutionChain chain = executionChain.getAndUpdate((pre) -> null);
        if (chain == null) {
            throw REPEAT_END_EXCEPTION;
        }
        return chain;
    }
}
