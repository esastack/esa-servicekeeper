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
package esa.servicekeeper.core.utils;

import esa.commons.StringUtils;
import esa.servicekeeper.core.annotation.Alias;
import esa.servicekeeper.core.annotation.ArgsCircuitBreaker;
import esa.servicekeeper.core.annotation.ArgsConcurrentLimiter;
import esa.servicekeeper.core.annotation.ArgsRateLimiter;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.RateLimitConfig;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParameterUtils {

    private ParameterUtils() {
    }

    static String getParamAlias(Parameter parameter, int index) {
        Alias alias = parameter.getAnnotation(Alias.class);
        if (alias != null && StringUtils.isNotBlank(alias.value())) {
            return alias.value();
        }
        return defaultName(index);
    }

    static boolean hasParamAnnotation(Parameter parameter) {
        return parameter.getAnnotation(ArgsConcurrentLimiter.class) != null
                || parameter.getAnnotation(ArgsRateLimiter.class) != null
                || parameter.getAnnotation(ArgsCircuitBreaker.class) != null
                || parameter.getAnnotation(Alias.class) != null;
    }

    public static String defaultName(int index) {
        return "arg" + index;
    }

    static RateLimitConfig getParamRateLimitConfig(Parameter parameter) {
        final ArgsRateLimiter argsRateLimiter = parameter.getAnnotation(ArgsRateLimiter.class);

        RateLimitConfig rateLimitConfig = null;
        if (argsRateLimiter != null) {
            rateLimitConfig = RateLimitConfig.builder()
                    .limitRefreshPeriod(DurationUtils.parse(argsRateLimiter.limitRefreshPeriod()))
                    .build();
        }

        return rateLimitConfig;
    }

    static Map<Object, Integer> getLimitForPeriodMap(Parameter parameter) {
        final ArgsRateLimiter argsRateLimiter = parameter.getAnnotation(ArgsRateLimiter.class);
        String limitForPeriodMapString;
        if (argsRateLimiter == null ||
                StringUtils.isEmpty(limitForPeriodMapString = argsRateLimiter.limitForPeriodMap())) {
            return Collections.emptyMap();
        }
        return getLimitForPeriodMapFromString(limitForPeriodMapString);
    }

    static Map<Object, Integer> getMaxConcurrentLimitMap(Parameter parameter) {
        final ArgsConcurrentLimiter argsConcurrentLimiter = parameter.getAnnotation(ArgsConcurrentLimiter.class);
        String thresholdMap;
        if (argsConcurrentLimiter == null || StringUtils.isEmpty(thresholdMap = argsConcurrentLimiter.thresholdMap())) {
            return Collections.emptyMap();
        }
        return getMaxConcurrentLimitMapFromString(thresholdMap);
    }

    static CircuitBreakerConfig getParamCircuitBreakerConfig(Parameter parameter) {
        final ArgsCircuitBreaker argsCircuitBreaker = parameter.getAnnotation(ArgsCircuitBreaker.class);

        CircuitBreakerConfig circuitBreakerConfig = null;
        if (argsCircuitBreaker != null) {
            circuitBreakerConfig = CircuitBreakerConfig.builder()
                    .ringBufferSizeInClosedState(argsCircuitBreaker.ringBufferSizeInClosedState())
                    .ringBufferSizeInHalfOpenState(argsCircuitBreaker.ringBufferSizeInHalfOpenState())
                    .ignoreExceptions(argsCircuitBreaker.ignoreExceptions())
                    .waitDurationInOpenState(DurationUtils.parse(argsCircuitBreaker.waitDurationInOpenState()))
                    .maxSpendTimeMs(argsCircuitBreaker.maxSpendTimeMs())
                    .predicateStrategy(argsCircuitBreaker.predicateStrategy())
                    .build();
        }

        return circuitBreakerConfig;
    }

    static Map<Object, Float> getFailureRateThresholdMap(Parameter parameter) {
        final ArgsCircuitBreaker argsCircuitBreaker = parameter.getAnnotation(ArgsCircuitBreaker.class);
        String failureRateThreshold;
        if (argsCircuitBreaker == null
                || StringUtils.isEmpty(failureRateThreshold = argsCircuitBreaker.failureRateThresholdMap())) {
            return Collections.emptyMap();
        }
        return getFailureRateThresholdMapFromString(failureRateThreshold);
    }

    static Integer getMaxRateLimitValueSize(Parameter parameter) {
        final ArgsRateLimiter argsRateLimiter = parameter.getAnnotation(ArgsRateLimiter.class);
        return argsRateLimiter == null ? null : argsRateLimiter.maxValueSize();
    }

    static Integer getMaxConcurrentLimitValueSize(Parameter parameter) {
        final ArgsConcurrentLimiter argsConcurrentLimiter = parameter.getAnnotation(ArgsConcurrentLimiter.class);
        return argsConcurrentLimiter == null ? null : argsConcurrentLimiter.maxValueSize();
    }

    static Integer getMaxCircuitBreakerValueSize(Parameter parameter) {
        final ArgsCircuitBreaker argsCircuitBreaker = parameter.getAnnotation(ArgsCircuitBreaker.class);
        return argsCircuitBreaker == null ? null : argsCircuitBreaker.maxValueSize();
    }

    private static Map<Object, Integer> getMaxConcurrentLimitMapFromString(final String mapString) {
        return getLimitForPeriodMapFromString(mapString);
    }

    private static Map<Object, Integer> getLimitForPeriodMapFromString(final String mapString) {
        if (StringUtils.isEmpty(mapString)) {
            return Collections.emptyMap();
        }
        String[] limitForPeriodItems = getSplit(mapString);
        final Map<Object, Integer> limitForPeriodMap = new LinkedHashMap<>(limitForPeriodItems.length);
        for (String item : limitForPeriodItems) {
            String key = item.substring(0, item.indexOf(":")).trim();
            String value = item.substring(item.indexOf(":") + 1).trim();
            limitForPeriodMap.putIfAbsent(key, Integer.valueOf(value));
        }
        return limitForPeriodMap;
    }

    private static Map<Object, Float> getFailureRateThresholdMapFromString(final String mapString) {
        if (StringUtils.isEmpty(mapString)) {
            return Collections.emptyMap();
        }
        final String[] failureRateThresholdItems = getSplit(mapString);
        final Map<Object, Float> failureRateThresholdMap = new LinkedHashMap<>(failureRateThresholdItems.length);
        for (String item : failureRateThresholdItems) {
            String key = item.substring(0, item.indexOf(":")).trim();
            String value = item.substring(item.indexOf(":") + 1).trim();
            failureRateThresholdMap.putIfAbsent(key, Float.valueOf(value));
        }
        return failureRateThresholdMap;
    }

    private static String[] getSplit(final String origin) {
        return origin.substring(1, origin.length() - 1).split(",");
    }
}
