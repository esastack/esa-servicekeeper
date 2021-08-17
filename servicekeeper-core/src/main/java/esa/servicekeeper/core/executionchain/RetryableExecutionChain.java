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
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.retry.RetryableExecutor;

import java.util.List;
import java.util.function.Supplier;

public class RetryableExecutionChain extends SyncExecutionChainImpl {

    private final RetryableExecutor executor;

    public RetryableExecutionChain(List<Moat<?>> moats,
                                   FallbackHandler<?> fallbackHandler, RetryableExecutor executor) {
        super(moats, fallbackHandler);
        Checks.checkNotNull(executor, "executor");
        this.executor = executor;
    }

    @Override
    protected void doExecute(Context context, Supplier<OriginalInvocation> originalInvocation,
                             Runnable runnable, boolean isAsync) throws Throwable {
        if (isAsync) {
            super.doExecute(context, originalInvocation, runnable, true);
        } else {
            executor.doExecute(context,
                    originalInvocation == null ? null : originalInvocation.get(),
                    () -> {
                        runnable.run();
                        return null;
                    });
        }
    }

    @Override
    protected <R> R doExecute(Context context, Supplier<OriginalInvocation> originalInvocation,
                              Executable<R> executable, boolean isAsync) throws Throwable {
        if (isAsync) {
            return super.doExecute(context, originalInvocation, executable, true);
        } else {
            return executor.doExecute(context, originalInvocation == null
                    ? null : originalInvocation.get(), executable);
        }
    }

}
