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

import esa.servicekeeper.core.config.BackoffConfig;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.configsource.DynamicConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;

import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapCircuitBreaker;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapConcurrent;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapDynamic;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapRate;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapRetry;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasFallback;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_DISABLED;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_OPEN;

/**
 * Combines the {@link ExternalConfig} with the immutable {@link ServiceKeeperConfig}. The rule is:
 * If the external config is null then just get config from immutable config, else try to override the immutable
 * config by external config.
 */
public final class ConfigUtils {

    private ConfigUtils() {
    }

    public static ServiceKeeperConfig combine(ServiceKeeperConfig immutable, ExternalConfig config) {
        if (config == null) {
            return immutable == null ? null : ServiceKeeperConfig.copyFrom(immutable).build();
        }
        if (immutable == null) {
            return buildWhenImmutableConfigIsNull(config);
        }
        return overrideImmutableConfig(immutable, config);
    }

    private static ServiceKeeperConfig buildWhenImmutableConfigIsNull(final ExternalConfig config) {
        // NOTE: Only combine external config when some config to bootstrap rateLimiter, circuitBreaker,
        // concurrentLimiter, retryOptions or fallback config has existed. The fallback config is very
        // useful for generating fallback handler.
        if (!(hasBootstrapDynamic(config) || hasFallback(config))) {
            return null;
        }

        final ServiceKeeperConfig.Builder builder = ServiceKeeperConfig.builder();
        if (hasBootstrapConcurrent(config)) {
            builder.concurrentLimiterConfig(combine(ConcurrentLimitConfig.ofDefault(), config));
        }
        if (hasBootstrapCircuitBreaker(config)) {
            builder.circuitBreakerConfig(combine(CircuitBreakerConfig.ofDefault(), config));
        }
        if (hasBootstrapRate(config)) {
            builder.rateLimiterConfig(combine(RateLimitConfig.ofDefault(), config));
        }
        builder.fallbackConfig(combine((FallbackConfig) null, config));
        if (hasBootstrapRetry(config)) {
            builder.retryConfig(combine(RetryConfig.ofDefault(), config));
        }
        return builder.build();
    }

    private static ServiceKeeperConfig overrideImmutableConfig(final ServiceKeeperConfig immutable,
                                                               final ExternalConfig config) {
        final ServiceKeeperConfig.Builder builder = ServiceKeeperConfig.builder();
        if (immutable.getConcurrentLimitConfig() == null) {
            if (hasBootstrapConcurrent(config)) {
                builder.concurrentLimiterConfig(combine(ConcurrentLimitConfig.ofDefault(), config));
            }
        } else {
            builder.concurrentLimiterConfig(combine(immutable.getConcurrentLimitConfig(), config));
        }

        if (immutable.getRateLimitConfig() == null) {
            if (hasBootstrapRate(config)) {
                builder.rateLimiterConfig(combine(RateLimitConfig.ofDefault(), config));
            }
        } else {
            builder.rateLimiterConfig(combine(immutable.getRateLimitConfig(), config));
        }

        if (immutable.getCircuitBreakerConfig() == null) {
            if (hasBootstrapCircuitBreaker(config)) {
                builder.circuitBreakerConfig(combine(CircuitBreakerConfig.ofDefault(), config));
            }
        } else {
            builder.circuitBreakerConfig(combine(immutable.getCircuitBreakerConfig(), config));
        }

        builder.fallbackConfig(combine(immutable.getFallbackConfig(), config));

        if (immutable.getRetryConfig() == null) {
            if (hasBootstrapRetry(config)) {
                builder.retryConfig(combine(RetryConfig.ofDefault(), config));
            }
        } else {
            builder.retryConfig(combine(immutable.getRetryConfig(), config));
        }

        return builder.build();
    }

    public static ConcurrentLimitConfig combine(final ConcurrentLimitConfig config,
                                                final ExternalConfig external) {
        if (external == null) {
            return config == null ? null : ConcurrentLimitConfig.from(config).build();
        }
        ConcurrentLimitConfig.Builder builder = (config == null
                ? ConcurrentLimitConfig.builder() : ConcurrentLimitConfig.from(config));

        if (external.getMaxConcurrentLimit() != null) {
            return builder.threshold(external.getMaxConcurrentLimit()).build();
        }
        return builder.build();
    }

    public static RateLimitConfig combine(final RateLimitConfig config, final ExternalConfig external) {
        if (external == null) {
            return config == null ? null : RateLimitConfig.from(config).build();
        }
        final RateLimitConfig.Builder builder = (config == null
                ? RateLimitConfig.builder() : RateLimitConfig.from(config));

        if (external.getLimitRefreshPeriod() != null) {
            builder.limitRefreshPeriod(external.getLimitRefreshPeriod());
        }
        if (external.getLimitForPeriod() != null) {
            builder.limitForPeriod(external.getLimitForPeriod());
        }
        return builder.build();
    }

    public static CircuitBreakerConfig combine(final CircuitBreakerConfig config,
                                               final ExternalConfig external) {
        if (external == null) {
            return config == null ? null : CircuitBreakerConfig.from(config).build();
        }
        CircuitBreakerConfig.Builder builder = (config == null
                ? CircuitBreakerConfig.builder() : CircuitBreakerConfig.from(config));

        if (external.getRingBufferSizeInClosedState() != null) {
            builder.ringBufferSizeInClosedState(external.getRingBufferSizeInClosedState());
        }
        if (external.getRingBufferSizeInHalfOpenState() != null) {
            builder.ringBufferSizeInHalfOpenState(external
                    .getRingBufferSizeInHalfOpenState());
        }
        if (external.getMaxSpendTimeMs() != null) {
            builder.maxSpendTimeMs(external.getMaxSpendTimeMs());
        }
        if (external.getFailureRateThreshold() != null) {
            builder.failureRateThreshold(external.getFailureRateThreshold());
        }
        if (external.getIgnoreExceptions() != null) {
            builder.ignoreExceptions(external.getIgnoreExceptions());
        }
        if (external.getPredicateStrategy() != null) {
            builder.predicateStrategy(external.getPredicateStrategy());
        }
        if (external.getWaitDurationInOpenState() != null) {
            builder.waitDurationInOpenState(external.getWaitDurationInOpenState());
        }
        if (external.getForcedDisabled() != null && external.getForcedDisabled()) {
            builder.state(FORCED_DISABLED);
        }
        if (external.getForcedOpen() != null && external.getForcedOpen()) {
            builder.state(FORCED_OPEN);
        }

        return builder.build();
    }

    public static FallbackConfig combine(final FallbackConfig config, final ExternalConfig external) {
        if (external == null) {
            return config == null ? null : FallbackConfig.copyFrom(config).build();
        }
        boolean allIsNull = true;
        final FallbackConfig.Builder builder = config == null
                ? FallbackConfig.builder() : FallbackConfig.copyFrom(config);
        if (external.getFallbackValue() != null) {
            allIsNull = false;
            builder.specifiedValue(external.getFallbackValue());
        }
        if (external.getFallbackClass() != null) {
            allIsNull = false;
            builder.targetClass(external.getFallbackClass());
        }
        if (external.getFallbackExceptionClass() != null) {
            allIsNull = false;
            builder.specifiedException(external.getFallbackExceptionClass());
        }
        if (external.getFallbackMethodName() != null) {
            allIsNull = false;
            builder.methodName(external.getFallbackMethodName());
        }
        if (external.getAlsoApplyFallbackToBizException() != null) {
            allIsNull = false;
            builder.alsoApplyToBizException(external.getAlsoApplyFallbackToBizException());
        }

        if (config == null && allIsNull) {
            return null;
        }
        return builder.build();
    }

    public static RetryConfig combine(final RetryConfig config, DynamicConfig dynamic) {
        if (dynamic == null) {
            return config == null ? null : RetryConfig.copyFrom(config).build();
        }

        final RetryConfig.Builder builder = (config == null ? RetryConfig.builder() : RetryConfig.copyFrom(config));
        if (dynamic.getMaxAttempts() != null) {
            builder.maxAttempts(dynamic.getMaxAttempts());
        }
        if (dynamic.getIncludeExceptions() != null && dynamic.getIncludeExceptions().length > 0) {
            builder.includeExceptions(dynamic.getIncludeExceptions());
        }
        if (dynamic.getExcludeExceptions() != null && dynamic.getExcludeExceptions().length > 0) {
            builder.excludeExceptions(dynamic.getExcludeExceptions());
        }

        boolean config0 = config != null && config.getBackoffConfig() != null;
        BackoffConfig.Builder builder0 = config0
                ? BackoffConfig.copyFrom(config.getBackoffConfig()) : BackoffConfig.builder();

        boolean hasExternalBackOffConfig = false;
        if (dynamic.getDelay() != null) {
            builder0.delay(dynamic.getDelay());
            hasExternalBackOffConfig = true;
        }
        if (dynamic.getMaxDelay() != null) {
            builder0.maxDelay(dynamic.getMaxDelay());
        }
        if (dynamic.getMultiplier() != null) {
            builder0.multiplier(dynamic.getMultiplier());
        }

        if (config0 || hasExternalBackOffConfig) {
            builder.backoffConfig(builder0.build());
        }

        return builder.build();
    }
}
