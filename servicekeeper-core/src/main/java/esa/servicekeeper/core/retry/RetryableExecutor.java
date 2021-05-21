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
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.executionchain.Executor;

import java.util.StringJoiner;

public class RetryableExecutor implements Executor {

    private final RetryOperations operations;

    public RetryableExecutor(RetryOperations operations) {
        Checks.checkNotNull(operations, "operations");
        this.operations = operations;
    }

    /**
     * Executes runnable with retry if there contains some recover methods, and the attempts times used up,
     * the recover method will be invoke
     *
     * @param context  the serviceKeeper internal
     * @param invocation original invocation
     * @param runnable runnable to run
     */
    @Override
    public void doExecute(Context context, OriginalInvocation invocation, Runnable runnable)
            throws Throwable {
        operations.execute(buildContext(context, invocation), () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Executes executable with retry
     * if there contains some recover methods, and the attempts times used up
     * the recover method will be invoke
     *
     * @param context    internal
     * @param invocation original invocation
     * @param executable executable to execute
     * @param <R>        type parameter
     * @return return the origin result if no Exception happens in the origin method
     * if there is something wrong in the origin method and attempts used up, and got a recover, then return the
     * recovery result.
     * @throws Throwable any throwable
     */
    @Override
    public <R> R doExecute(Context context, OriginalInvocation invocation, Executable<R> executable)
            throws Throwable {
        return operations.execute(buildContext(context, invocation), executable);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetryableExecutor.class.getSimpleName() + "[", "]")
                .add("operations=" + operations.toString())
                .toString();
    }

    /**
     * Get retry operations of this executor.
     *
     * @return retry operations
     */
    public RetryOperations getOperations() {
        return this.operations;
    }

    private RetryContext buildContext(final Context context, final OriginalInvocation invocation) {
        return new RetryContext(context, invocation);
    }
}
