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
package esa.servicekeeper.core.config;

import esa.commons.Checks;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

public class RetryConfig implements Serializable {

    private static final long serialVersionUID = -1587433817518327852L;

    private final Class<? extends Throwable>[] includeExceptions;
    private final Class<? extends Throwable>[] excludeExceptions;
    private final Integer maxAttempts;
    private final BackoffConfig backoffConfig;

    private RetryConfig(Class<? extends Throwable>[] includeExceptions,
                        Class<? extends Throwable>[] excludeExceptions,
                        Integer maxAttempts,
                        BackoffConfig backoffConfig) {
        this.includeExceptions = includeExceptions;
        this.excludeExceptions = excludeExceptions;
        this.maxAttempts = maxAttempts;
        this.backoffConfig = backoffConfig;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RetryConfig ofDefault() {
        return builder().build();
    }

    public static Builder copyFrom(RetryConfig retryConfig) {
        Checks.checkNotNull(retryConfig, "retryConfig");
        Builder newBuilder = builder();
        newBuilder.includeExceptions(retryConfig.getIncludeExceptions())
                .excludeExceptions(retryConfig.getExcludeExceptions())
                .maxAttempts(retryConfig.getMaxAttempts());

        BackoffConfig backoffConfig = retryConfig.getBackoffConfig();
        if (backoffConfig != null) {
            newBuilder.backoffConfig(BackoffConfig.builder()
                    .multiplier(backoffConfig.getMultiplier())
                    .delay(backoffConfig.getDelay())
                    .maxDelay(backoffConfig.getMaxDelay()).build());
        }

        return newBuilder;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RetryConfig that = (RetryConfig) o;
        return Arrays.equals(includeExceptions, that.includeExceptions) &&
                Arrays.equals(excludeExceptions, that.excludeExceptions) &&
                Objects.equals(maxAttempts, that.maxAttempts) &&
                Objects.equals(backoffConfig, that.backoffConfig);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(maxAttempts, backoffConfig);
        result = 31 * result + Arrays.hashCode(includeExceptions);
        result = 31 * result + Arrays.hashCode(excludeExceptions);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetryConfig.class.getSimpleName() + "[", "]")
                .add("includeExceptions=" + Arrays.toString(includeExceptions))
                .add("excludeExceptions=" + Arrays.toString(excludeExceptions))
                .add("maxAttempts=" + maxAttempts)
                .add("backoffConfig=" + backoffConfig)
                .toString();
    }

    public static class Builder {
        /**
         * Default includes, contains Exception and the SubExceptions
         */
        @SuppressWarnings("unchecked")
        private static final Class<? extends Throwable>[] DEFAULT_INCLUDES = new Class[]{Exception.class};
        /**
         * Default excludes, exclude nothing by default
         */
        @SuppressWarnings("unchecked")
        private static final Class<? extends Throwable>[] DEFAULT_EXCLUDES = new Class[0];
        private Class<? extends Throwable>[] includeExceptions = DEFAULT_INCLUDES;
        private Class<? extends Throwable>[] excludeExceptions = DEFAULT_EXCLUDES;

        /**
         * Default attempts times,include the first try case
         */
        private Integer maxAttempts = 3;

        /**
         * Default backOff is null
         */
        private BackoffConfig backoffConfig;

        Builder() {
        }

        public Builder includeExceptions(Class<? extends Throwable>[] includeExceptions) {
            this.includeExceptions = includeExceptions;
            return this;
        }

        public Builder excludeExceptions(Class<? extends Throwable>[] excludeExceptions) {
            this.excludeExceptions = excludeExceptions;
            return this;
        }

        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder backoffConfig(BackoffConfig backoffConfig) {
            this.backoffConfig = backoffConfig;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this.includeExceptions, this.excludeExceptions, this.maxAttempts,
                    this.backoffConfig);
        }
    }

}
