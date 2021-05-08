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

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.metrics.actuator.collector.RealTimeConfigCollector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationEndpointTest {

    @Test
    void testSkConfig() {
        final RealTimeConfigCollector collector = mock(RealTimeConfigCollector.class);
        final ImmutableConfigs configs = mock(ImmutableConfigs.class);

        final ConfigurationEndpoint endpoint = new ConfigurationEndpoint(collector, configs);
        when(collector.config(ResourceId.from("A"))).thenReturn(null)
                .thenReturn(ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                .rateLimiterConfig(RateLimitConfig.ofDefault())
                .retryConfig(RetryConfig.ofDefault()).build());

        then(endpoint.skConfig("A")).isNull();

        final FallbackConfig fallbackConfig = FallbackConfig.builder()
                .methodName("A")
                .specifiedException(RuntimeException.class)
                .targetClass(Object.class)
                .specifiedValue("DEF")
                .build();
        when(configs.getConfig(ResourceId.from("A"), ImmutableConfigs.ConfigType.FALLBACK_CONFIG))
                .thenReturn(fallbackConfig);


        final ServiceKeeperConfigPojo pojo = endpoint.skConfig("A");

        then(pojo.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(100);
        then(pojo.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(pojo.getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(-1L);
        then(pojo.getCircuitBreakerConfig().getWaitDurationInOpenState()).isEqualTo("1m");
        then(pojo.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        then(pojo.getCircuitBreakerConfig().getPredicateStrategy()).isEqualTo(PredicateByException.class);
        then(pojo.getCircuitBreakerConfig().getState()).isEqualTo(CircuitBreaker.State.AUTO.toString());

        then(pojo.getRateLimitConfig().getLimitForPeriod()).isEqualTo(Integer.MAX_VALUE);
        then(pojo.getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo("1s");

        then(pojo.getConcurrentLimitConfig().getThreshold()).isEqualTo(Integer.MAX_VALUE);

        then(pojo.getFallbackConfig().getMethodName()).isEqualTo("A");
        then(pojo.getFallbackConfig().getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(pojo.getFallbackConfig().getTargetClass()).isEqualTo(Object.class);
        then(pojo.getFallbackConfig().getSpecifiedValue()).isEqualTo("DEF");

        then(pojo.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        then(pojo.getRetryConfig().getBackoffConfig()).isNull();
    }

}
