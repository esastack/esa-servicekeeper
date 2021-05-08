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

import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ServiceKeeperConfigPojoTest {

    @Test
    void testBasic() {
        final ServiceKeeperConfig config = ServiceKeeperConfig.builder()
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                .rateLimiterConfig(RateLimitConfig.ofDefault())
                .retryConfig(RetryConfig.ofDefault())
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                .build();

        final ServiceKeeperConfigPojo pojo = ServiceKeeperConfigPojo.from(config, null);
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

        then(pojo.getFallbackConfig()).isNull();

        then(pojo.getRetryConfig().getMaxAttempts()).isEqualTo(3);
        then(pojo.getRetryConfig().getBackoffConfig()).isNull();
    }
}
