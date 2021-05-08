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
package esa.servicekeeper.metrics.actuator.endpoints;

import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;

class ServiceKeeperConfigPojo {

    private final CircuitBreakerConfigPojo circuitBreakerConfig;
    private final ConcurrentLimitConfig concurrentLimitConfig;
    private final RateLimitConfigPojo rateLimitConfig;
    private final FallbackConfig fallbackConfig;
    private final RetryConfigPojo retryConfig;

    private ServiceKeeperConfigPojo(CircuitBreakerConfigPojo circuitBreakerConfig,
                                    ConcurrentLimitConfig concurrentLimitConfig,
                                    RateLimitConfigPojo rateLimitConfig,
                                    FallbackConfig fallbackConfig,
                                    RetryConfigPojo retryConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.concurrentLimitConfig = concurrentLimitConfig;
        this.rateLimitConfig = rateLimitConfig;
        this.fallbackConfig = fallbackConfig;
        this.retryConfig = retryConfig;
    }

    static ServiceKeeperConfigPojo from(ServiceKeeperConfig config, FallbackConfig fallbackConfig) {
        return config == null ? null : new ServiceKeeperConfigPojo(config.getCircuitBreakerConfig() == null
                ? null : CircuitBreakerConfigPojo.from(config.getCircuitBreakerConfig()),
                config.getConcurrentLimitConfig(),
                config.getRateLimitConfig() == null ? null : RateLimitConfigPojo.from(config.getRateLimitConfig()),
                fallbackConfig,
                config.getRetryConfig() == null ? null : RetryConfigPojo.from(config.getRetryConfig()));
    }

    public CircuitBreakerConfigPojo getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    public ConcurrentLimitConfig getConcurrentLimitConfig() {
        return concurrentLimitConfig;
    }

    public RateLimitConfigPojo getRateLimitConfig() {
        return rateLimitConfig;
    }

    public FallbackConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public RetryConfigPojo getRetryConfig() {
        return retryConfig;
    }
}
