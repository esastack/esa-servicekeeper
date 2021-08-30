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
package io.esastack.servicekeeper.metrics.actuator.endpoints;

import io.esastack.servicekeeper.core.config.BackoffConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;

class RetryConfigPojo {

    private final Class<? extends Throwable>[] includeExceptions;
    private final Class<? extends Throwable>[] excludeExceptions;
    private final Integer maxAttempts;
    private final BackoffConfig backoffConfig;

    private RetryConfigPojo(Class<? extends Throwable>[] includeExceptions,
                            Class<? extends Throwable>[] excludeExceptions,
                            Integer maxAttempts, BackoffConfig backoffConfig) {
        this.includeExceptions = includeExceptions;
        this.excludeExceptions = excludeExceptions;
        this.maxAttempts = maxAttempts;
        this.backoffConfig = backoffConfig;
    }

    static RetryConfigPojo from(RetryConfig config) {
        return new RetryConfigPojo(config.getIncludeExceptions(), config.getExcludeExceptions(),
                config.getMaxAttempts(), config.getBackoffConfig());
    }

    public Class<? extends Throwable>[] getIncludeExceptions() {
        return includeExceptions;
    }

    public Class<? extends Throwable>[] getExcludeExceptions() {
        return excludeExceptions;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public BackoffConfig getBackoffConfig() {
        return backoffConfig;
    }
}
