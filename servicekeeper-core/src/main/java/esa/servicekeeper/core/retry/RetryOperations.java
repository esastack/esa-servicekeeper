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

import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.metrics.RetryMetrics;

public interface RetryOperations {

    /**
     * Executes {@link Executable} with retry.
     *
     * @param context           context
     * @param executable        executable
     * @param <T>               generic type
     * @return result
     * @throws Throwable        any throwable
     */
    <T> T execute(RetryContext context, Executable<T> executable) throws Throwable;

    /**
     * Obtains current config
     *
     * @return retry config
     */
    RetryConfig getConfig();

    /**
     * Obtains current {@link RetryMetrics}.
     *
     * @return metrics
     */
    RetryMetrics getMetrics();
}
