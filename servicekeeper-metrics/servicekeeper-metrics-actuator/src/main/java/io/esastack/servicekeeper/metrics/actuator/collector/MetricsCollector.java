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
package io.esastack.servicekeeper.metrics.actuator.collector;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.metrics.Metrics;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.retry.RetryableExecutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MetricsCollector {

    private final InternalMoatCluster cluster;

    public MetricsCollector(InternalMoatCluster cluster) {
        this.cluster = cluster;
    }

    public Metrics metrics(ResourceId resourceId, Metrics.Type type) {
        final MoatCluster cluster = this.cluster.get(resourceId);
        if (cluster == null) {
            return null;
        }

        return selectByType(cluster, type);
    }

    public Map<ResourceId, Map<Metrics.Type, Metrics>> all() {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final Map<ResourceId, Map<Metrics.Type, Metrics>> metricsMap = new LinkedHashMap<>(chainMap.size());
        for (Map.Entry<ResourceId, MoatCluster> entry : chainMap.entrySet()) {
            Map<Metrics.Type, Metrics> metrics = metricsesOfId(entry.getKey());
            if (metrics != null) {
                metricsMap.putIfAbsent(entry.getKey(), metrics);
            }
        }
        return metricsMap;
    }

    Map<Metrics.Type, Metrics> metricsesOfId(ResourceId resourceId) {
        Map<Metrics.Type, Metrics> metricsMap = new LinkedHashMap<>(Metrics.Type.values().length);
        for (Metrics.Type type : Metrics.Type.values()) {
            Metrics metrics = metrics(resourceId, type);
            if (metrics != null) {
                metricsMap.putIfAbsent(type, metrics);
            }
        }
        return metricsMap;
    }

    private static Metrics selectByType(MoatCluster cluster, Metrics.Type type) {
        final List<Moat<?>> moats = cluster.getAll();
        switch (type) {
            case RATE_LIMIT:
                for (Moat<?> moat : moats) {
                    if (moat instanceof RateLimitMoat) {
                        return ((RateLimitMoat) moat).rateLimiter().metrics();
                    }
                }
                return null;
            case CIRCUIT_BREAKER:
                for (Moat<?> moat : moats) {
                    if (moat instanceof CircuitBreakerMoat) {
                        return ((CircuitBreakerMoat) moat).getCircuitBreaker().metrics();
                    }
                }
                return null;
            case CONCURRENT_LIMIT:
                for (Moat<?> moat : moats) {
                    if (moat instanceof ConcurrentLimitMoat) {
                        return ((ConcurrentLimitMoat) moat).getConcurrentLimiter().metrics();
                    }
                }
                return null;
            case RETRY:
                if (RetryableMoatCluster.isInstance(cluster)) {
                    final RetryableExecutor executor = ((RetryableMoatCluster) cluster).retryExecutor();
                    if (executor == null) {
                        return null;
                    } else {
                        return executor.getOperations().getMetrics();
                    }
                }
                return null;
            default:
                return null;
        }
    }
}

