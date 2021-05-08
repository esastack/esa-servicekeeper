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
package esa.servicekeeper.core.retry;

import esa.commons.Checks;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.exception.ServiceKeeperWrapException;
import esa.servicekeeper.core.executionchain.Context;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class RetryContext {

    private final Context context;
    private final OriginalInvocation invocation;

    private Throwable lastThrowable;
    private volatile int retriedCount;

    private static final AtomicIntegerFieldUpdater<RetryContext> RETRIED_COUNT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(RetryContext.class, "retriedCount");

    RetryContext(Context context, OriginalInvocation invocation) {
        Checks.checkNotNull(context, "Context must not be null");
        this.context = context;
        this.invocation = invocation;
    }

    public Context getContext() {
        return context;
    }

    public OriginalInvocation getInvocation() {
        return invocation;
    }

    public Throwable getLastThrowable() {
        return lastThrowable;
    }

    public int getRetriedCount() {
        return retriedCount;
    }

    void registerThrowable(Throwable th) {
        RETRIED_COUNT_UPDATER.addAndGet(this, 1);
        this.lastThrowable = th instanceof ServiceKeeperWrapException ? th.getCause() : th;
    }
}
