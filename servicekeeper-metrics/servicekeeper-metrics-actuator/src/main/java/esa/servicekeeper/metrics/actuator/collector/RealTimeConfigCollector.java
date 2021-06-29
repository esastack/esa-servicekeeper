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
package esa.servicekeeper.metrics.actuator.collector;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.RetryableMoatCluster;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.retry.RetryableExecutor;

import java.util.LinkedHashMap;
import java.util.Map;

public class RealTimeConfigCollector {

    private final InternalMoatCluster cluster;

    public RealTimeConfigCollector(InternalMoatCluster cluster) {
        this.cluster = cluster;
    }

    CircuitBreakerConfig circuitBreakerConfig(ResourceId resourceId) {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final MoatCluster cluster = chainMap.get(resourceId);
        if (cluster == null) {
            return null;
        }

        for (Moat<?> moat : cluster.getAll()) {
            if (moat instanceof CircuitBreakerMoat) {
                return ((CircuitBreakerMoat) moat).getCircuitBreaker().config();
            }
        }
        return null;
    }

    ConcurrentLimitConfig concurrentLimitConfig(ResourceId resourceId) {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final MoatCluster cluster = chainMap.get(resourceId);
        if (cluster == null) {
            return null;
        }

        for (Moat<?> moat : cluster.getAll()) {
            if (moat instanceof ConcurrentLimitMoat) {
                return ConcurrentLimitConfig.builder()
                        .threshold(((ConcurrentLimitMoat) moat).getConcurrentLimiter().metrics().threshold())
                        .build();
            }
        }
        return null;
    }

    RateLimitConfig rateLimitConfig(ResourceId resourceId) {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final MoatCluster moatCluster = chainMap.get(resourceId);
        if (moatCluster == null) {
            return null;
        }

        for (Moat<?> moat : moatCluster.getAll()) {
            if (moat instanceof RateLimitMoat) {
                return ((RateLimitMoat) moat).rateLimiter().config();
            }
        }
        return null;
    }

    public ServiceKeeperConfig config(ResourceId resourceId) {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final MoatCluster cluster = chainMap.get(resourceId);
        if (cluster == null) {
            return null;
        }

        ServiceKeeperConfig.Builder builder = ServiceKeeperConfig.builder();
        for (Moat<?> moat : cluster.getAll()) {
            if (moat instanceof RateLimitMoat) {
                builder.rateLimiterConfig(((RateLimitMoat) moat).rateLimiter().config());
            } else if (moat instanceof ConcurrentLimitMoat) {
                builder.concurrentLimiterConfig(ConcurrentLimitConfig.builder()
                        .threshold(((ConcurrentLimitMoat) moat).getConcurrentLimiter().metrics().threshold())
                        .build());
            } else if (moat instanceof CircuitBreakerMoat) {
                builder.circuitBreakerConfig(((CircuitBreakerMoat) moat).getCircuitBreaker().config());
            }
        }

        if (cluster instanceof RetryableMoatCluster) {
            final RetryableExecutor executor = ((RetryableMoatCluster) cluster).retryExecutor();
            if (executor != null) {
                builder.retryConfig(executor.getOperations().getConfig());
            }
        }
        return builder.build();
    }

    Map<ResourceId, CircuitBreakerConfig> circuitBreakerConfigs() {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final Map<ResourceId, CircuitBreakerConfig> configs = new LinkedHashMap<>(chainMap.size());
        for (Map.Entry<ResourceId, MoatCluster> entry : chainMap.entrySet()) {
            CircuitBreakerConfig config = circuitBreakerConfig(entry.getKey());
            if (config != null) {
                configs.putIfAbsent(entry.getKey(), config);
            }
        }
        return configs;
    }

    Map<ResourceId, ConcurrentLimitConfig> concurrentLimitConfigs() {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final Map<ResourceId, ConcurrentLimitConfig> configs = new LinkedHashMap<>(chainMap.size());
        for (Map.Entry<ResourceId, MoatCluster> entry : chainMap.entrySet()) {
            ConcurrentLimitConfig config = concurrentLimitConfig(entry.getKey());
            if (config != null) {
                configs.putIfAbsent(entry.getKey(), config);
            }
        }
        return configs;
    }

    Map<ResourceId, RateLimitConfig> rateLimitConfigs() {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final Map<ResourceId, RateLimitConfig> configs = new LinkedHashMap<>(chainMap.size());
        for (Map.Entry<ResourceId, MoatCluster> entry : chainMap.entrySet()) {
            RateLimitConfig config = rateLimitConfig(entry.getKey());
            if (config != null) {
                configs.putIfAbsent(entry.getKey(), config);
            }
        }
        return configs;
    }

    public Map<ResourceId, ServiceKeeperConfig> configs() {
        final Map<ResourceId, MoatCluster> chainMap = cluster.getAll();
        final Map<ResourceId, ServiceKeeperConfig> configs = new LinkedHashMap<>(chainMap.size());
        for (Map.Entry<ResourceId, MoatCluster> entry : chainMap.entrySet()) {
            ServiceKeeperConfig config = config(entry.getKey());
            if (config != null) {
                configs.putIfAbsent(entry.getKey(), config);
            }
        }
        return configs;
    }

}
