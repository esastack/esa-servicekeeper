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

public class ServiceKeeperConfig implements Serializable {

    private static final long serialVersionUID = -2673286612954950280L;

    private CircuitBreakerConfig circuitBreakerConfig;
    private ConcurrentLimitConfig concurrentLimitConfig;
    private RateLimitConfig rateLimitConfig;
    private FallbackConfig fallbackConfig;
    private RetryConfig retryConfig;

    public static Builder builder() {
        return new Builder();
    }

    public static Builder copyFrom(ServiceKeeperConfig config) {
        Checks.checkNotNull(config, "config");
        return new Builder().fallbackConfig(config.getFallbackConfig())
                .circuitBreakerConfig(config.getCircuitBreakerConfig())
                .rateLimiterConfig(config.getRateLimitConfig())
                .retryConfig(config.getRetryConfig())
                .concurrentLimiterConfig(config.getConcurrentLimitConfig());
    }

    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    public void setCircuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
    }

    public ConcurrentLimitConfig getConcurrentLimitConfig() {
        return concurrentLimitConfig;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setConcurrentLimitConfig(ConcurrentLimitConfig concurrentLimitConfig) {
        this.concurrentLimitConfig = concurrentLimitConfig;
    }

    public RateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }

    public void setRateLimitConfig(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    public FallbackConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public void setFallbackConfig(FallbackConfig fallbackConfig) {
        this.fallbackConfig = fallbackConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceKeeperConfig{");
        boolean isFirstOne = true;
        if (circuitBreakerConfig != null) {
            sb.append("circuitBreakerConfig=").append(circuitBreakerConfig);
            isFirstOne = false;
        }
        if (concurrentLimitConfig != null) {
            if (isFirstOne) {
                sb.append("concurrentLimitConfig=").append(concurrentLimitConfig);
                isFirstOne = false;
            } else {
                sb.append(", concurrentLimitConfig=").append(concurrentLimitConfig);
            }
        }
        if (rateLimitConfig != null) {
            if (isFirstOne) {
                sb.append("rateLimitConfig=").append(rateLimitConfig);
                isFirstOne = false;
            } else {
                sb.append(", rateLimitConfig=").append(rateLimitConfig);
            }
        }
        if (fallbackConfig != null) {
            if (isFirstOne) {
                sb.append("fallbackConfig=").append(fallbackConfig);
                isFirstOne = false;
            } else {
                sb.append(", fallbackConfig=").append(fallbackConfig);
            }
        }
        if (retryConfig != null) {
            if (isFirstOne) {
                sb.append("retryConfig=").append(retryConfig);
            } else {
                sb.append(", retryConfig=").append(retryConfig);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        private CircuitBreakerConfig circuitBreakerConfig1;
        private ConcurrentLimitConfig concurrentLimitConfig;
        private RateLimitConfig rateLimitConfig;
        private FallbackConfig fallbackConfig;
        private RetryConfig retryConfig;

        private Builder() {
        }

        public Builder circuitBreakerConfig(CircuitBreakerConfig fuseConfig) {
            this.circuitBreakerConfig1 = fuseConfig;
            return this;
        }

        public Builder concurrentLimiterConfig(ConcurrentLimitConfig concurrentLimitConfig) {
            this.concurrentLimitConfig = concurrentLimitConfig;
            return this;
        }

        public Builder rateLimiterConfig(RateLimitConfig rateLimitConfig) {
            this.rateLimitConfig = rateLimitConfig;
            return this;
        }

        public Builder fallbackConfig(FallbackConfig fallbackConfig) {
            this.fallbackConfig = fallbackConfig;
            return this;
        }

        public Builder retryConfig(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
            return this;
        }

        public ServiceKeeperConfig build() {
            ServiceKeeperConfig serviceKeeperConfig = new ServiceKeeperConfig();
            serviceKeeperConfig.setCircuitBreakerConfig(circuitBreakerConfig1);
            serviceKeeperConfig.setConcurrentLimitConfig(concurrentLimitConfig);
            serviceKeeperConfig.setRateLimitConfig(rateLimitConfig);
            serviceKeeperConfig.setFallbackConfig(fallbackConfig);
            serviceKeeperConfig.setRetryConfig(retryConfig);
            return serviceKeeperConfig;
        }
    }
}
