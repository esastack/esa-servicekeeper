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
package esa.servicekeeper.core.retry.internal.impl;

import esa.servicekeeper.core.retry.RetryContext;
import esa.servicekeeper.core.retry.internal.RetryablePredicate;

public class ExceptionPredicate implements RetryablePredicate {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final int maxAttempts;

    public ExceptionPredicate(int maxAttempts) {
        this.maxAttempts = maxAttempts > 0 ? maxAttempts : DEFAULT_MAX_ATTEMPTS;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        final Throwable th = context.getLastThrowable();
        return context.getRetriedCount() < this.maxAttempts && canRetry0(th);
    }

    /**
     * Predicate whether current {@link Throwable} can retry again.
     *
     * @param th throwable
     * @return true or false
     */
    protected boolean canRetry0(Throwable th) {
        return th != null;
    }

}
