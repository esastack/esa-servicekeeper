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
package io.esastack.servicekeeper.core.utils;

import esa.commons.StringUtils;
import io.esastack.servicekeeper.core.annotation.Alias;
import io.esastack.servicekeeper.core.annotation.Backoff;
import io.esastack.servicekeeper.core.annotation.CircuitBreaker;
import io.esastack.servicekeeper.core.annotation.ConcurrentLimiter;
import io.esastack.servicekeeper.core.annotation.Fallback;
import io.esastack.servicekeeper.core.annotation.Group;
import io.esastack.servicekeeper.core.annotation.RateLimiter;
import io.esastack.servicekeeper.core.annotation.Retryable;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.config.BackoffConfig;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public final class MethodUtils {

    private MethodUtils() {
    }

    public static CompositeServiceKeeperConfig getCompositeConfig(Method method) {
        boolean allIsNull = true;
        CompositeServiceKeeperConfig.CompositeServiceKeeperConfigBuilder builder =
                CompositeServiceKeeperConfig.builder();
        if (hasMethodAnnotation(method)) {
            builder.methodConfig(getAnnotatedConfig(method));
            allIsNull = false;
        }

        int index = 0;
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (ParameterUtils.hasParamAnnotation(parameter)) {
                final String aliasName = ParameterUtils.getParamAlias(parameter, index);
                builder.argRateLimitConfig(index, aliasName,
                        ParameterUtils.getParamRateLimitConfig(parameter),
                        ParameterUtils.getLimitForPeriodMap(parameter),
                        ParameterUtils.getMaxRateLimitValueSize(parameter));

                builder.argConcurrentLimit(index, aliasName,
                        ParameterUtils.getMaxConcurrentLimitMap(parameter),
                        ParameterUtils.getMaxConcurrentLimitValueSize(parameter));

                builder.argCircuitBreakerConfig(index, aliasName,
                        ParameterUtils.getParamCircuitBreakerConfig(parameter),
                        ParameterUtils.getFailureRateThresholdMap(parameter),
                        ParameterUtils.getMaxCircuitBreakerValueSize(parameter));

                allIsNull = false;
            }
            index++;
        }

        final GroupResourceId group = getGroup(method);
        if (allIsNull) {
            if (group == null) {
                return null;
            } else {
                return builder.group(group).build();
            }
        } else {
            return builder.group(group).build();
        }
    }

    public static String getMethodAlias(Method method) {
        if (method.getAnnotation(Alias.class) != null &&
                StringUtils.isNotBlank(method.getAnnotation(Alias.class).value())) {
            return method.getAnnotation(Alias.class).value();
        }
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    private static boolean hasMethodAnnotation(Method method) {
        return method.getAnnotation(ConcurrentLimiter.class) != null
                || method.getAnnotation(RateLimiter.class) != null
                || method.getAnnotation(CircuitBreaker.class) != null
                || method.getAnnotation(Fallback.class) != null
                || method.getAnnotation(Retryable.class) != null;
    }

    private static ServiceKeeperConfig getAnnotatedConfig(Method method) {
        ConcurrentLimitConfig concurrentConfig = null;
        RateLimitConfig rateLimitConfig = null;
        CircuitBreakerConfig circuitBreakerConfig = null;
        FallbackConfig fallbackConfig = null;
        RetryConfig retryConfig = null;

        final ConcurrentLimiter concurrentLimiter = method.getAnnotation(ConcurrentLimiter.class);
        if (concurrentLimiter != null) {
            concurrentConfig = ConcurrentLimitConfig.builder()
                    .threshold(AnnotationUtils.resolve("ConcurrentLimiter.threshold",
                            concurrentLimiter.threshold(),
                            concurrentLimiter.value(),
                            ConcurrentLimiter.DEFAULT_VALUE))
                    .build();
        }

        final RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        if (rateLimiter != null) {
            rateLimitConfig = RateLimitConfig.builder()
                    .limitForPeriod(AnnotationUtils.resolve("RateLimiter.limitForPeriod",
                            rateLimiter.limitForPeriod(),
                            rateLimiter.value(),
                            RateLimiter.DEFAULT_LIMIT_FOR_PERIOD))
                    .limitRefreshPeriod(DurationUtils.parse(rateLimiter.limitRefreshPeriod()))
                    .build();
        }

        final CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);
        if (circuitBreaker != null) {
            circuitBreakerConfig = CircuitBreakerConfig.builder()
                    .failureRateThreshold(AnnotationUtils.resolve("CircuitBreaker.failureRateThreshold",
                            circuitBreaker.failureRateThreshold(),
                            circuitBreaker.value(),
                            CircuitBreaker.DEFAULT_FAILURE_RATE_THRESHOLD))
                    .ignoreExceptions(circuitBreaker.ignoreExceptions())
                    .ringBufferSizeInClosedState(circuitBreaker.ringBufferSizeInClosedState())
                    .ringBufferSizeInHalfOpenState(circuitBreaker.ringBufferSizeInHalfOpenState())
                    .waitDurationInOpenState(DurationUtils.parse(circuitBreaker.waitDurationInOpenState()))
                    .maxSpendTimeMs(circuitBreaker.maxSpendTimeMs())
                    .predicateStrategy(circuitBreaker.predicateStrategy())
                    .build();
        }

        final Fallback fallback = method.getAnnotation(Fallback.class);
        if (fallback != null) {
            FallbackConfig.Builder builder = FallbackConfig.builder()
                    .specifiedException(fallback.fallbackExceptionClass())
                    .specifiedValue(fallback.fallbackValue())
                    .alsoApplyToBizException(fallback.alsoApplyToBizException());
            if (fallback.fallbackClass() == Void.class && StringUtils.isEmpty(fallback.fallbackMethod())) {
                fallbackConfig = builder.build();
            } else {
                fallbackConfig = builder
                        // Note: Important
                        // If targetClass is not configured, use current method's declaring class as default
                        .targetClass(fallback.fallbackClass() == Void.class
                                ? method.getDeclaringClass() : fallback.fallbackClass())
                        // Note: Important
                        // If methodName is not configured, use current method's name as default
                        .methodName(StringUtils.isEmpty(fallback.fallbackMethod())
                                ? method.getName() : fallback.fallbackMethod()).build();
            }
        }

        final Retryable retryable = method.getAnnotation(Retryable.class);
        if (retryable != null) {
            RetryConfig.Builder builder = RetryConfig.builder()
                    .includeExceptions(retryable.includeExceptions())
                    .excludeExceptions(retryable.excludeExceptions())
                    .maxAttempts(AnnotationUtils.resolve("RetryConfig.maxAttempts",
                            retryable.maxAttempts(),
                            retryable.value(),
                            Retryable.DEFAULT_MAX_ATTEMPTS));
            Backoff backoff = retryable.backoff();
            if (backoff.delay() != 0) {
                builder.backoffConfig(BackoffConfig.builder()
                        .delay(backoff.delay())
                        .maxDelay(backoff.maxDelay())
                        .multiplier(backoff.multiplier())
                        .build());
            }
            retryConfig = builder.build();
        }

        return ServiceKeeperConfig.builder()
                .concurrentLimiterConfig(concurrentConfig)
                .circuitBreakerConfig(circuitBreakerConfig)
                .rateLimiterConfig(rateLimitConfig)
                .fallbackConfig(fallbackConfig)
                .retryConfig(retryConfig)
                .build();
    }

    static GroupResourceId getGroup(Method method) {
        final Group group = method.getAnnotation(Group.class);
        return group == null ? null : GroupResourceId.from(group.value());
    }
}

