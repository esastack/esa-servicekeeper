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

import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.retry.RetryContext;
import io.esastack.servicekeeper.core.retry.internal.impl.ExceptionCausePredicate;

import java.util.HashMap;
import java.util.Map;

public interface RetryablePredicate {

    /**
     * Judge whether current context can retry.
     *
     * @param context context
     * @return true or false
     */
    boolean canRetry(RetryContext context);

    /**
     * Constructs a {@link RetryablePredicate} using {@link RetryConfig}.
     *
     * @param config config
     * @return predicate
     */
    static RetryablePredicate newInstance(RetryConfig config) {
        final Map<Class<? extends Throwable>, Boolean> exs =
                new HashMap<>(2);

        // Add includes
        if (config.getIncludeExceptions() == null || config.getIncludeExceptions().length == 0) {
            exs.put(Exception.class, true);
        } else {
            for (Class<? extends Throwable> throwable : config.getIncludeExceptions()) {
                exs.put(throwable, true);
            }
        }

        // Add excludes
        if (config.getExcludeExceptions() != null && config.getExcludeExceptions().length > 0) {
            for (Class<? extends Throwable> throwable : config.getExcludeExceptions()) {
                exs.put(throwable, false);
            }
        }

        return new ExceptionCausePredicate(config.getMaxAttempts() == null ? 0 : config.getMaxAttempts(),
                exs, false, true, 0);
    }
}
