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
package io.esastack.servicekeeper.core.retry.internal.impl;

import io.esastack.servicekeeper.core.exception.BackOffInterruptedException;
import io.esastack.servicekeeper.core.retry.RetryContext;
import io.esastack.servicekeeper.core.retry.internal.BackOffPolicy;

public class ExponentialBackOffPolicy implements BackOffPolicy {

    private static final long DEFAULT_DELAY = 0L;
    private static final long DEFAULT_MAX_DELAY = 0L;
    private static final double DEFAULT_MULTIPLIER = 1.0d;

    private final long maxDelay;
    private final double multiplier;
    private final long delay;

    public ExponentialBackOffPolicy(long delay, long maxDelay, double multiplier) {
        this.delay = delay > 0 ? Math.min(delay, 30_000) : DEFAULT_DELAY;
        this.maxDelay = maxDelay > 0 ? Math.min(maxDelay, 30_000) : DEFAULT_MAX_DELAY;
        this.multiplier = multiplier > 0 ? Math.min(multiplier, 100d) : DEFAULT_MULTIPLIER;
    }

    @Override
    public void backOff(RetryContext context) throws BackOffInterruptedException {
        final long currentDelay = computeDelay(context);
        if (currentDelay < 0) {
            return;
        }

        try {
            Thread.sleep(currentDelay);
        } catch (InterruptedException e) {
            throw new BackOffInterruptedException("Thread was interrupted while back off sleeping", e);
        }
    }

    /**
     * Obtains current delay time, and the package accessible is only designed for junit test.
     *
     * @param context context
     * @return current delay time.
     */
    long computeDelay(RetryContext context) {
        return (long) Math.min(maxDelay, delay * Math.pow(multiplier, context.getRetriedCount() - 1));
    }

}
