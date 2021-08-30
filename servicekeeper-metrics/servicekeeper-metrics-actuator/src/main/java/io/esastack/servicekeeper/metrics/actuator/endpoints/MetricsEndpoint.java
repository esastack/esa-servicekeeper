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

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.metrics.ConcurrentLimitMetrics;
import io.esastack.servicekeeper.core.metrics.RateLimitMetrics;
import io.esastack.servicekeeper.core.metrics.RetryMetrics;
import io.esastack.servicekeeper.metrics.actuator.collector.MetricsCollector;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import static io.esastack.servicekeeper.core.metrics.Metrics.Type.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.metrics.Metrics.Type.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.metrics.Metrics.Type.RATE_LIMIT;
import static io.esastack.servicekeeper.core.metrics.Metrics.Type.RETRY;

@Endpoint(id = "skmetrics")
public class MetricsEndpoint {

    private final MetricsCollector collector;

    public MetricsEndpoint(MetricsCollector collector) {
        this.collector = collector;
    }

    @ReadOperation
    public CompositeMetricsPojo skMetrics(String resourceId) {
        final ResourceId id = ResourceId.from(resourceId);
        return CompositeMetricsPojo.from((ConcurrentLimitMetrics) collector.metrics(id, CONCURRENT_LIMIT),
                (RateLimitMetrics) collector.metrics(id, RATE_LIMIT),
                (CircuitBreakerMetrics) collector.metrics(id, CIRCUIT_BREAKER),
                (RetryMetrics) collector.metrics(id, RETRY));
    }
}
