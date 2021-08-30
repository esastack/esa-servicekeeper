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
package io.esastack.servicekeeper.core.retry.internal;

import io.esastack.servicekeeper.core.config.BackoffConfig;
import io.esastack.servicekeeper.core.exception.BackOffInterruptedException;
import io.esastack.servicekeeper.core.retry.RetryContext;
import io.esastack.servicekeeper.core.retry.internal.impl.ExponentialBackOffPolicy;

/**
 * Usually, it's not a good idea to retry continuously, due that the original resource needs some time to back to
 * normal and too many retries one by one may increase system load, so a back off policy is recommended.
 */
public interface BackOffPolicy {

    /**
     * Back off, such as sleep a while.
     *
     * @param context context
     * @throws BackOffInterruptedException interrupted exception
     */
    void backOff(RetryContext context) throws BackOffInterruptedException;

    /**
     * Constructs a {@link BackOffPolicy} instance using {@link BackoffConfig}.
     *
     * @param config config
     * @return back off policy
     */
    static BackOffPolicy newInstance(BackoffConfig config) {
        if (config == null) {
            return new ExponentialBackOffPolicy(-1L, -1L, -1.0d);
        }
        return new ExponentialBackOffPolicy(config.getDelay(), config.getMaxDelay(), config.getMultiplier());
    }

}
