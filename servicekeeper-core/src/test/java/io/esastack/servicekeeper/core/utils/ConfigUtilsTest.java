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

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ConfigUtilsTest {

    @Test
    void testCombineWhenExternalConfigIsNull() {
        // Case1: ImmutableConfig is null
        BDDAssertions.then(ConfigUtils.combine((ServiceKeeperConfig) null, null)).isNull();

        // Case2: Only RateLimitConfig is not null
        ServiceKeeperConfig immutableConfig = ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();

        // Case3: Only ConcurrentLimitConfig is not null
        immutableConfig = ServiceKeeperConfig.builder()
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();

        // Case4: Only CircuitBreakerConfig is not null
        immutableConfig = ServiceKeeperConfig.builder().circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();

        // Case5: Only FallbackConfig is not null
        immutableConfig = ServiceKeeperConfig.builder().fallbackConfig(FallbackConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNotNull();
    }

    @Test
    void testCombineWhenExternalConfigIsNotNull() {
        // Case1: ImmutableConfig is null

        // Case1.1: Only ExternalConfig's fallbackConfig is not null
        ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setFallbackMethodName("testFallback");
        externalConfig.setFallbackClass(RuntimeException.class);
        externalConfig.setFallbackValue("Hello");
        externalConfig.setFallbackExceptionClass(IllegalArgumentException.class);
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig)).isNotNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()
                .getSpecifiedValue()).isEqualTo("Hello");
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()
                .getMethodName()).isEqualTo("testFallback");
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()
                .getTargetClass()).isEqualTo(RuntimeException.class);
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()
                .getSpecifiedException()).isEqualTo(IllegalArgumentException.class);

        // Case1.2: Only ExternalConfig's rateLimitConfig is not null
        externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(RandomUtils.randomInt(500));
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getCircuitBreakerConfig()).isNull();

        // Case1.3: Only ExternalConfig's concurrentLimitConfig is not null
        externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(500));
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getCircuitBreakerConfig()).isNull();

        // Case1.4: Only ExternalConfig's circuitBreakerConfig is not null
        externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine((ServiceKeeperConfig) null, externalConfig).getCircuitBreakerConfig()).isNotNull();

        // Case2: ImmutableConfig is not null

        // Case2.1: Only ImmutableConfig's rateLimitConfig is not null
        ServiceKeeperConfig immutableConfig = ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(RandomUtils.randomInt(500));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFallbackMethodName("fallback");
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        // Case2.2: Only ImmutableConfig's concurrentLimitConfig is not null
        immutableConfig = ServiceKeeperConfig.builder()
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(RandomUtils.randomInt(500));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFallbackMethodName("fallback");
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        // Case2.3: Only ImmutableConfig's circuitBreakerConfig is not null
        immutableConfig = ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(RandomUtils.randomInt(500));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFallbackMethodName("fallback");
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        // Case2.4: Only ImmutableConfig's fallbackConfig is not null
        immutableConfig = ServiceKeeperConfig.builder()
                .fallbackConfig(FallbackConfig.ofDefault()).build();
        then(ConfigUtils.combine(immutableConfig, null).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, null).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, null).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(RandomUtils.randomInt(500));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNotNull();

        externalConfig = new ExternalConfig();
        externalConfig.setFallbackMethodName("fallback");
        then(ConfigUtils.combine(immutableConfig, externalConfig).getRateLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getFallbackConfig()).isNotNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getConcurrentLimitConfig()).isNull();
        then(ConfigUtils.combine(immutableConfig, externalConfig).getCircuitBreakerConfig()).isNull();
    }

    @Test
    void testCombineRateLimitConfig() {
        then(ConfigUtils.combine((RateLimitConfig) null, null)).isNull();
        then(ConfigUtils.combine(RateLimitConfig.ofDefault(), null)).isNotNull();
    }

    @Test
    void testCombineConcurrentLimitConfig() {
        then(ConfigUtils.combine((ConcurrentLimitConfig) null, null)).isNull();
        then(ConfigUtils.combine(ConcurrentLimitConfig.ofDefault(), null)).isNotNull();
    }

    @Test
    void testCombineFallbackConfig() {
        then(ConfigUtils.combine((FallbackConfig) null, null)).isNull();
        then(ConfigUtils.combine(FallbackConfig.ofDefault(), null)).isNotNull();
    }

    @Test
    void testCombineCircuitBreakerConfig() {
        then(ConfigUtils.combine((CircuitBreakerConfig) null, null)).isNull();
        then(ConfigUtils.combine(CircuitBreakerConfig.ofDefault(), null)).isNotNull();
    }

    @Test
    void testCombineRetryConfig() {
        BDDAssertions.then(ConfigUtils.combine((RetryConfig) null, null)).isNull();
        then(ConfigUtils.combine(RetryConfig.ofDefault(), null)).isNotNull();
    }
}
