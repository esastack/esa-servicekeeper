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
package io.esastack.servicekeeper.core.configsource;

public class ExternalConfigUtils {

    public static boolean isDynamicEquals(final ExternalConfig config0, final ExternalConfig config1) {
        if (config0 == null) {
            return config1 == null;
        }

        return config0.equals(config1);
    }

    public static String getDynamicString(final ExternalConfig config) {
        return config == null ? "null" : config.toString();
    }

    public static boolean hasBootstrapConcurrent(final ExternalConfig config) {
        return config != null && config.getMaxConcurrentLimit() != null;
    }

    public static boolean hasBootstrapDynamic(final ExternalConfig config) {
        return hasBootstrapConcurrent(config) || hasBootstrapRate(config) ||
                hasBootstrapCircuitBreaker(config) || hasBootstrapRetry(config);
    }

    public static boolean hasBootstrapRate(final ExternalConfig config) {
        return config != null && config.getLimitForPeriod() != null;
    }

    public static boolean hasBootstrapCircuitBreaker(final ExternalConfig config) {
        return config != null && (config.getForcedDisabled() != null ||
                config.getForcedOpen() != null ||
                config.getFailureRateThreshold() != null);
    }

    public static boolean hasBootstrapRetry(final ExternalConfig config) {
        return config != null && (config.getMaxAttempts() != null ||
                config.getIncludeExceptions() != null ||
                config.getExcludeExceptions() != null);
    }

    public static boolean hasConcurrent(final ExternalConfig config) {
        return config != null && config.getMaxConcurrentLimit() != null;
    }

    public static boolean hasRate(final ExternalConfig config) {
        return config != null && (config.getLimitRefreshPeriod() != null
                || config.getLimitForPeriod() != null);
    }

    public static boolean hasCircuitBreaker(final ExternalConfig config) {
        return config != null &&
                (config.getForcedOpen() != null ||
                        config.getForcedDisabled() != null ||
                        config.getRingBufferSizeInClosedState() != null ||
                        config.getRingBufferSizeInHalfOpenState() != null ||
                        config.getFailureRateThreshold() != null ||
                        config.getWaitDurationInOpenState() != null);
    }

    public static boolean hasFallback(final ExternalConfig config) {
        return config != null && (config.getFallbackClass() != null ||
                config.getFallbackMethodName() != null ||
                config.getFallbackValue() != null ||
                config.getFallbackExceptionClass() != null ||
                config.getAlsoApplyFallbackToBizException() != null);
    }


    public static boolean isEmpty(ExternalConfig config) {
        if (config == null) {
            return true;
        }

        return config.isAllEmpty();
    }
}
