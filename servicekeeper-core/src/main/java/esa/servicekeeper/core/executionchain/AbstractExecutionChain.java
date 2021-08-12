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

import esa.commons.Checks;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.asynchandle.RequestHandleImpl;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.moats.Moat;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractExecutionChain implements SyncExecutionChain, AsyncExecutionChain {

    private final List<Moat<?>> moats;
    private final FallbackHandler<?> fallbackHandler;

    AbstractExecutionChain(List<Moat<?>> moats, FallbackHandler<?> fallbackHandler) {
        Checks.checkNotNull(moats, "moats");
        this.moats = Collections.unmodifiableList(moats);
        this.fallbackHandler = fallbackHandler;
    }

    @Override
    public RequestHandle tryToExecute(Context ctx) {
        try {
            doTryToExecute(ctx);
            recordStartTime();
        } catch (ServiceKeeperNotPermittedException e) {
            return RequestHandleImpl.createNotAllowHandle(this,
                    ctx, fallbackHandler, e);
        }

        return RequestHandleImpl.createAllowHandle(this,
                ctx, fallbackHandler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R asyncExecute(AsyncContext ctx, Supplier<OriginalInvocation> invocation,
                              Executable<R> executable, AsyncResultHandler handler) throws Throwable {
        RequestHandle handle = tryToExecute(ctx);
        Throwable notAllowCause = handle.getNotAllowedCause();
        if (notAllowCause != null) {
            return (R) handle.fallback(notAllowCause);
        }

        try {
            R result = doExecute(ctx, invocation, executable, true);
            // Note: If the original call execute successfully, clean the internal later.
            return (R) handler.handle(result, handle);
        } catch (Throwable throwable) {
            // Note: If any throwable caught, clean the internal timely.
            return (R) handle.fallback(throwable);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(Context ctx, Supplier<OriginalInvocation> invocation,
                         Executable<R> executable) throws Throwable {
        RequestHandle handle = tryToExecute(ctx);
        Throwable notAllowCause = handle.getNotAllowedCause();
        if (notAllowCause != null) {
            return (R) handle.fallback(notAllowCause);
        }

        try {
            R result = doExecute(ctx, invocation, executable, false);
            ctx.setResult(result);
            handle.endWithResult(result);
            return result;
        } catch (Throwable throwable) {
            return (R) handle.fallback(throwable);
        }
    }

    @Override
    public void execute(Context ctx, Supplier<OriginalInvocation> invocation,
                        Runnable runnable) throws Throwable {
        RequestHandle handle = tryToExecute(ctx);
        Throwable notAllowCause = handle.getNotAllowedCause();
        if (notAllowCause != null) {
            handle.fallback(notAllowCause);
            return;
        }

        try {
            doExecute(ctx, invocation, runnable, false);
            handle.endWithSuccess();
        } catch (Throwable throwable) {
            handle.fallback(throwable);
        }
    }

    @Override
    public void endWithSuccess(Context ctx) {
        if (getStartTime() > 0L) {
            endAndExitMoats(ctx);
        } else {
            //if getStartTime() <= 0L,it declare the context is not start,so it can't be end
            throw REQUEST_NOT_START_EXCEPTION;
        }
    }

    @Override
    public void endWithResult(Context ctx, Object result) {
        if (getStartTime() > 0L) {
            ctx.setResult(result);
            endAndExitMoats(ctx);
        } else {
            //if getStartTime() <= 0L,it declare the context is not start,so it can't be end
            throw REQUEST_NOT_START_EXCEPTION;
        }
    }

    @Override
    public void endWithError(Context ctx, Throwable throwable) {
        if (getStartTime() > 0L) {
            ctx.setBizException(throwable);
            endAndExitMoats(ctx);
        } else {
            //if getStartTime() <= 0L,it declare the context is not start,
            // so the throwable is caused by serviceKeeper'logic
            exitMoats(ctx);
        }
    }

    /**
     * Run runnable internal, for subClass to override for retry or else.
     *
     * @param context            internal
     * @param originalInvocation the supplier to get original invocation
     * @param runnable           runnable
     * @param isAsync            original runnable is async or not
     * @throws Throwable throwable
     */
    protected void doExecute(Context context, Supplier<OriginalInvocation> originalInvocation,
                             Runnable runnable, boolean isAsync) throws Throwable {
        runnable.run();
    }

    /**
     * Run executable internal, for subClass to override for retry or else.
     *
     * @param context            internal
     * @param originalInvocation the supplier to get original invocation
     * @param executable         executable
     * @param isAsync            original runnable is async or not
     * @param <R>                R
     * @return result
     * @throws Throwable any throwable
     */
    protected <R> R doExecute(Context context, Supplier<OriginalInvocation> originalInvocation,
                              Executable<R> executable, boolean isAsync) throws Throwable {
        return executable.execute();
    }

    /**
     * Record startTime of current invocation.
     */
    protected abstract void recordStartTime();

    /**
     * Record endTime of current invocation.
     */
    protected abstract void recordEndTime();

    /**
     * Get startTime of current invocation.
     *
     * @return startTimeNs
     */
    protected abstract long getStartTime();

    /**
     * Get endTime of current invocation.
     *
     * @return endTimeMs
     */
    protected abstract long getEndTime();

    /**
     * Get spendTimeMs of current invocation.
     *
     * @return spendTimeMs
     */
    protected abstract long getSpendTimeMs();

    /**
     * Get current index.
     *
     * @return index
     */
    protected abstract int getCurrentIndex();

    /**
     * Record current index.
     *
     * @param index index
     */
    protected abstract void setCurrentIndex(int index);

    private void endAndExitMoats(Context ctx) {
        //Note: If the ctx has already end, current end will be ignored.
        if (getEndTime() <= 0L) {
            recordEndTime();
        }

        ctx.setSpendTimeMs(getSpendTimeMs());
        exitMoats(ctx);
    }

    private void exitMoats(Context ctx) {
        for (int i = getCurrentIndex(); i >= 0; i--) {
            moats.get(i).exit(ctx);
        }
        setCurrentIndex(-1);
    }

    private void doTryToExecute(Context ctx) throws ServiceKeeperNotPermittedException {
        int index = 0;
        for (int i = 0, size = moats.size(); i < size; i++, index++) {
            try {
                moats.get(i).enter(ctx);
            } catch (ServiceKeeperNotPermittedException e) {
                setCurrentIndex(i - 1);
                throw e;
            }
        }
        setCurrentIndex(index - 1);
    }
}

