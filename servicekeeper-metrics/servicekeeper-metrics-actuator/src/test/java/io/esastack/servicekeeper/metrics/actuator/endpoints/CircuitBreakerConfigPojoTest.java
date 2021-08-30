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
package io.esastack.servicekeeper.metrics.actuator.endpoints;

import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.BDDAssertions.then;

class CircuitBreakerConfigPojoTest {

    @SuppressWarnings("unchecked")
    @Test
    void testBasic() {
        final CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .predicateStrategy(PredicateByExceptionAndSpendTime.class)
                .ringBufferSizeInHalfOpenState(99)
                .ringBufferSizeInClosedState(9)
                .failureRateThreshold(99.0f)
                .waitDurationInOpenState(Duration.ofSeconds(99L))
                .maxSpendTimeMs(99L)
                .ignoreExceptions(new Class[]{RuntimeException.class})
                .state(CircuitBreaker.State.AUTO)
                .build();

        final CircuitBreakerConfigPojo pojo = CircuitBreakerConfigPojo.from(config);
        then(pojo.getFailureRateThreshold()).isEqualTo(99.0f);
        then(pojo.getPredicateStrategy()).isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(pojo.getRingBufferSizeInHalfOpenState()).isEqualTo(99);
        then(pojo.getRingBufferSizeInClosedState()).isEqualTo(9);
        then(pojo.getWaitDurationInOpenState()).isEqualTo("99s");
        then(Arrays.equals(pojo.getIgnoreExceptions(), new Class[]{RuntimeException.class})).isTrue();
        then(pojo.getState()).isEqualTo(CircuitBreaker.State.AUTO.toString());
        then(pojo.getMaxSpendTimeMs()).isEqualTo(99L);
    }
}
