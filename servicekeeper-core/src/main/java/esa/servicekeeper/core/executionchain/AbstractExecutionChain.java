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
import esa.servicekeeper.core.asynchandle.NotAllowedRequestHandle;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.asynchandle.RequestHandleImpl;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.exception.FallbackToExceptionWrapper;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.utils.RequestHandleUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static esa.servicekeeper.core.asynchandle.RequestHandle.FALLBACK_NOT_CONFIGURED_EXCEPTION;

public abstract class AbstractExecutionChain implements SyncExecutionChain, AsyncExecutionChain {

    private final List<Moat<?>> moats;

    AbstractExecutionChain(List<Moat<?>> moats) {
        Checks.checkNotNull(moats, "moats");
        this.moats = Collections.unmodifiableList(moats);
    }

    @Override
    public RequestHandle tryToExecute(Context ctx) {
        int index = 0;
        for (int i = 0, size = moats.size(); i < size; i++, index++) {
            if (!moats.get(i).tryThrough(ctx)) {
                setCurrentIndex(i - 1);
                // Ends and clean the moats.
                endAndClean(ctx);
                return getNotAllowedRequestHandle(moats.get(i), ctx);
            }
        }
        setCurrentIndex(index - 1);
        recordStartTime();
        return new RequestHandleImpl(this, ctx);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R asyncExecute(AsyncContext ctx, Supplier<OriginalInvocation> invocation,
                              Executable<R> executable, AsyncResultHandler handler) throws Throwable {
        final RequestHandle requestHandle = tryToExecute(ctx);
        if (requestHandle.isAllowed()) {
            try {
                recordStartTime();
                R result = doExecute(ctx, invocation, executable, true);
                // Note: If the original call execute successfully, clean the internal later.
                return (R) handler.handle(result, requestHandle);
            } catch (Throwable throwable) {
                // Note: If any throwable caught, clean the internal timely.
                requestHandle.endWithError(throwable);
            }
            throw ctx.getBizException();
        } else {
            // The fallback result.
            return (R) RequestHandleUtils.handle(ctx.getResourceId(), requestHandle);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R execute(Context ctx, Supplier<OriginalInvocation> invocation,
                         Executable<R> executable) throws Throwable {
        final RequestHandle requestHandle = tryToExecute(ctx);
        if (requestHandle.isAllowed()) {
            try {
                recordStartTime();
                R result = doExecute(ctx, invocation, executable, false);
                ctx.setResult(result);
                return result;
            } catch (Throwable throwable) {
                ctx.setBizException(throwable);
            } finally {
                // Note: Clean the ctx timely.
                recordEndTime();
                endAndClean(ctx);
            }
            throw ctx.getBizException();
        } else {
            return (R) RequestHandleUtils.handle(ctx.getResourceId(), requestHandle);
        }
    }

    @Override
    public void execute(Context ctx, Supplier<OriginalInvocation> invocation,
                        Runnable runnable) throws Throwable {
        final RequestHandle requestHandle = tryToExecute(ctx);
        if (requestHandle.isAllowed()) {
            try {
                recordStartTime();
                doExecute(ctx, invocation, runnable, false);
            } finally {
                recordEndTime();
                endAndClean(ctx);
            }
        } else {
            RequestHandleUtils.handle(ctx.getResourceId(), requestHandle);
        }
    }

    @Override
    public void endWithSuccess(Context ctx) {
        if (getStartTime() > 0L) {
            endAndClean(ctx);
        } else {
            throw REQUEST_NOT_START_EXCEPTION;
        }
    }

    @Override
    public void endWithResult(Context ctx, Object result) {
        if (getStartTime() > 0L) {
            ctx.setResult(result);
            endAndClean(ctx);
        } else {
            throw REQUEST_NOT_START_EXCEPTION;
        }
    }

    @Override
    public void endWithError(Context ctx, Throwable throwable) {
        if (getStartTime() > 0L) {
            ctx.setBizException(throwable);
            endAndClean(ctx);
        } else {
            throw REQUEST_NOT_START_EXCEPTION;
        }
    }

    private void endAndClean(Context ctx) {
        //Note: If the ctx has already end, current end will be ignored.
        if (getEndTime() <= 0L) {
            recordEndTime();
        }

        ctx.setSpendTimeMs(getSpendTimeMs());

        for (int i = getCurrentIndex(); i >= 0; i--) {
            moats.get(i).exit(ctx);
        }
        setCurrentIndex(-1);
    }

    /**
     * Run runnable internal, for subClass to override for retry or else.
     *
     * @param context                        internal
     * @param originalInvocation             the supplier to get original invocation
     * @param runnable                       runnable
     * @param isAsync                        original runnable is async or not
     * @throws Throwable throwable
     */
    protected void doExecute(Context context, Supplier<OriginalInvocation> originalInvocation,
                             Runnable runnable, boolean isAsync) throws Throwable {
        runnable.run();
    }

    /**
     * Run executable internal, for subClass to override for retry or else.
     *
     * @param context                        internal
     * @param originalInvocation             the supplier to get original invocation
     * @param executable                     executable
     * @param isAsync                        original runnable is async or not
     * @param <R>                            R
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

    /**
     * Get a not allowed requestHandle from the moat.
     *
     * @param moat moat
     * @param ctx  ctx
     * @return NotAllowedRequestHandle
     */
    private NotAllowedRequestHandle getNotAllowedRequestHandle(final Moat moat, final Context ctx) {
        // Save the fallback's result.
        Object fallbackResult;

        ctx.setThroughFailsCause(moat.defaultFallbackToException(ctx));
        try {
            fallbackResult = moat.fallback(ctx);
        } catch (Throwable throwable) {
            // Note: If the original method fallback to exception, wrap and return it.
            if (moat.fallbackType().equals(FallbackHandler.FallbackType.FALLBACK_TO_EXCEPTION)) {
                if (moat.hasCustomFallbackHandler()) {
                    return new NotAllowedRequestHandle(new FallbackToExceptionWrapper(throwable),
                            ctx.getThroughFailsCause(),
                            true, null);
                } else {
                    return new NotAllowedRequestHandle(new FallbackToExceptionWrapper(throwable),
                            ctx.getThroughFailsCause(),
                            false, FALLBACK_NOT_CONFIGURED_EXCEPTION);
                }
            } else {
                return new NotAllowedRequestHandle(null, ctx.getThroughFailsCause(),
                        false, throwable);
            }
        }
        return new NotAllowedRequestHandle(fallbackResult, ctx.getThroughFailsCause(),
                true, null);
    }
}

