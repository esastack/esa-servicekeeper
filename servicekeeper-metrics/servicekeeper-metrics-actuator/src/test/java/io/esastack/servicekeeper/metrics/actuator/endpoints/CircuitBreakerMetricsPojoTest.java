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

import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class CircuitBreakerMetricsPojoTest {

    @Test
    void testBasic() {
        final CircuitBreakerMetrics metrics = new CircuitBreakerMetrics() {
            @Override
            public float failureRateThreshold() {
                return 99.0f;
            }

            @Override
            public int numberOfBufferedCalls() {
                return 1;
            }

            @Override
            public int numberOfFailedCalls() {
                return 2;
            }

            @Override
            public long numberOfNotPermittedCalls() {
                return 3L;
            }

            @Override
            public int maxNumberOfBufferedCalls() {
                return 4;
            }

            @Override
            public int numberOfSuccessfulCalls() {
                return 5;
            }

            @Override
            public CircuitBreaker.State state() {
                return CircuitBreaker.State.CLOSED;
            }
        };

        final CircuitBreakerMetricsPojo pojo = CircuitBreakerMetricsPojo.from(metrics);
        then(pojo.getFailureThreshold()).isEqualTo(99.0f);
        then(pojo.getNumberOfBufferedCalls()).isEqualTo(1);
        then(pojo.getNumberOfFailedCalls()).isEqualTo(2);
        then(pojo.getNumberOfNotPermittedCalls()).isEqualTo(3L);
        then(pojo.getMaxNumberOfBufferedCalls()).isEqualTo(4);
        then(pojo.getNumberOfSuccessfulCalls()).isEqualTo(5);
        then(pojo.getState()).isEqualTo(CircuitBreaker.State.CLOSED.toString());
    }

}
