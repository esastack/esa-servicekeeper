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
import esa.servicekeeper.core.metrics.CircuitBreakerMetrics;
import esa.servicekeeper.core.metrics.ConcurrentLimitMetrics;
import esa.servicekeeper.core.metrics.Metrics;
import esa.servicekeeper.core.metrics.RateLimitMetrics;
import esa.servicekeeper.core.metrics.RetryMetrics;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import esa.servicekeeper.metrics.actuator.collector.MetricsCollector;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsesEndpointTest {

    @Test
    void testSkMetricses() {
        final MetricsCollector collector = mock(MetricsCollector.class);
        final ResourceId id = ResourceId.from("A");

        final MetricsesEndpoint endpoint = new MetricsesEndpoint(collector);

        Map<Metrics.Type, Metrics> metrices = new HashMap<>(4);
        metrices.put(Metrics.Type.RETRY, new RetryMetrics() {
            @Override
            public int maxAttempts() {
                return 10;
            }

            @Override
            public long retriedTimes() {
                return 20L;
            }

            @Override
            public long totalRetriedCount() {
                return 30L;
            }
        });
        metrices.put(Metrics.Type.RATE_LIMIT, new RateLimitMetrics() {
            @Override
            public int numberOfWaitingThreads() {
                return 40;
            }

            @Override
            public int availablePermissions() {
                return 50;
            }
        });
        metrices.put(Metrics.Type.CONCURRENT_LIMIT, new ConcurrentLimitMetrics() {
            @Override
            public int threshold() {
                return 60;
            }

            @Override
            public int currentCallCount() {
                return 70;
            }
        });
        metrices.put(Metrics.Type.CIRCUIT_BREAKER, new CircuitBreakerMetrics() {
            @Override
            public float failureRateThreshold() {
                return 90.0f;
            }

            @Override
            public int numberOfBufferedCalls() {
                return 80;
            }

            @Override
            public int numberOfFailedCalls() {
                return 90;
            }

            @Override
            public long numberOfNotPermittedCalls() {
                return 100L;
            }

            @Override
            public int maxNumberOfBufferedCalls() {
                return 110;
            }

            @Override
            public int numberOfSuccessfulCalls() {
                return 120;
            }

            @Override
            public CircuitBreaker.State state() {
                return CircuitBreaker.State.CLOSED;
            }
        });

        when(collector.all()).thenReturn(null).thenReturn(Collections.singletonMap(id, metrices));
        then(endpoint.skMetricses()).isEmpty();

        final Map<String, CompositeMetricsPojo> pojo0s = endpoint.skMetricses();
        then(pojo0s.size()).isEqualTo(1);

        final CompositeMetricsPojo pojo = pojo0s.get(id.getName());
        then(pojo.getRetryMetrics().getTotalRetryCount()).isEqualTo(30L);
        then(pojo.getRetryMetrics().getHasRetryTimes()).isEqualTo(20L);

        then(pojo.getRateLimitMetrics().getWaitingThreads()).isEqualTo(40);
        then(pojo.getRateLimitMetrics().getAvailablePermissions()).isEqualTo(50);

        then(pojo.getConcurrentLimitMetrics().getMaxConcurrentLimit()).isEqualTo(60);
        then(pojo.getConcurrentLimitMetrics().getCurrentCallCounter()).isEqualTo(70);

        then(pojo.getCircuitBreakerMetrics().getFailureThreshold()).isEqualTo(90.0f);
        then(pojo.getCircuitBreakerMetrics().getNumberOfBufferedCalls()).isEqualTo(80);
        then(pojo.getCircuitBreakerMetrics().getNumberOfFailedCalls()).isEqualTo(90);
        then(pojo.getCircuitBreakerMetrics().getNumberOfNotPermittedCalls()).isEqualTo(100L);
        then(pojo.getCircuitBreakerMetrics().getMaxNumberOfBufferedCalls()).isEqualTo(110);
        then(pojo.getCircuitBreakerMetrics().getNumberOfSuccessfulCalls()).isEqualTo(120);
        then(pojo.getCircuitBreakerMetrics().getState()).isEqualTo(CircuitBreaker.State.CLOSED.toString());
    }

}
