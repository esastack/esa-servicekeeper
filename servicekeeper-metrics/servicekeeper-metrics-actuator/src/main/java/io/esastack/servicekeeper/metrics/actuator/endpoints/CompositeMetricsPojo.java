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
import io.esastack.servicekeeper.core.metrics.ConcurrentLimitMetrics;
import io.esastack.servicekeeper.core.metrics.RateLimitMetrics;
import io.esastack.servicekeeper.core.metrics.RetryMetrics;

class CompositeMetricsPojo {

    private final ConcurrentLimitMetricsPojo concurrentLimitMetrics;
    private final RateLimitMetricsPojo rateLimitMetrics;
    private final CircuitBreakerMetricsPojo circuitBreakerMetrics;
    private final RetryMetricsPojo retryMetrics;

    private CompositeMetricsPojo(ConcurrentLimitMetricsPojo concurrentLimitMetrics,
                                 RateLimitMetricsPojo rateLimitMetrics,
                                 CircuitBreakerMetricsPojo circuitBreakerMetrics,
                                 RetryMetricsPojo retryMetrics) {
        this.concurrentLimitMetrics = concurrentLimitMetrics;
        this.rateLimitMetrics = rateLimitMetrics;
        this.circuitBreakerMetrics = circuitBreakerMetrics;
        this.retryMetrics = retryMetrics;
    }

    static CompositeMetricsPojo from(ConcurrentLimitMetrics concurrentLimitMetrics,
                                     RateLimitMetrics rateLimitMetrics,
                                     CircuitBreakerMetrics circuitBreakerMetrics,
                                     RetryMetrics retryMetrics) {
        return new CompositeMetricsPojo(concurrentLimitMetrics == null
                ? null : ConcurrentLimitMetricsPojo.from(concurrentLimitMetrics),
                rateLimitMetrics == null ? null : RateLimitMetricsPojo.from(rateLimitMetrics),
                circuitBreakerMetrics == null ? null : CircuitBreakerMetricsPojo.from(circuitBreakerMetrics),
                retryMetrics == null ? null : RetryMetricsPojo.from(retryMetrics));
    }

    public ConcurrentLimitMetricsPojo getConcurrentLimitMetrics() {
        return concurrentLimitMetrics;
    }

    public RateLimitMetricsPojo getRateLimitMetrics() {
        return rateLimitMetrics;
    }

    public CircuitBreakerMetricsPojo getCircuitBreakerMetrics() {
        return circuitBreakerMetrics;
    }

    public RetryMetricsPojo getRetryMetrics() {
        return retryMetrics;
    }
}
