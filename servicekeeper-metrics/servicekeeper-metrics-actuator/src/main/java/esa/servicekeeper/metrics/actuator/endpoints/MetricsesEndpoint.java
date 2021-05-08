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
import esa.servicekeeper.metrics.actuator.collector.MetricsCollector;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static esa.servicekeeper.core.metrics.Metrics.Type.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.metrics.Metrics.Type.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.metrics.Metrics.Type.RATE_LIMIT;
import static esa.servicekeeper.core.metrics.Metrics.Type.RETRY;

@Endpoint(id = "skmetricses")
public class MetricsesEndpoint {

    private final MetricsCollector collector;

    public MetricsesEndpoint(MetricsCollector collector) {
        this.collector = collector;
    }

    @ReadOperation
    public Map<String, CompositeMetricsPojo> skMetricses() {
        final Map<ResourceId, Map<Metrics.Type, Metrics>> metricsMap = collector.all();
        if (metricsMap == null) {
            return Collections.emptyMap();
        }

        final Map<String, CompositeMetricsPojo> result = new LinkedHashMap<>(metricsMap.size());

        for (Map.Entry<ResourceId, Map<Metrics.Type, Metrics>> entry : metricsMap.entrySet()) {
            result.putIfAbsent(entry.getKey().getName(),
                    CompositeMetricsPojo.from((ConcurrentLimitMetrics) entry.getValue().get(CONCURRENT_LIMIT),
                            (RateLimitMetrics) entry.getValue().get(RATE_LIMIT),
                            (CircuitBreakerMetrics) entry.getValue().get(CIRCUIT_BREAKER),
                            (RetryMetrics) entry.getValue().get(RETRY))
            );
        }

        return result;
    }
}
