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
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.MoatConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.metrics.Metrics;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.retry.RetryOperationsImpl;
import io.esastack.servicekeeper.core.retry.RetryableExecutor;
import io.esastack.servicekeeper.core.retry.internal.impl.ExceptionPredicate;
import io.esastack.servicekeeper.core.retry.internal.impl.ExponentialBackOffPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsCollectorTest {

    @Test
    void testGetMetrics() {
        final ResourceId id = ResourceId.from("testGetMetrics");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final MetricsCollector collector = new MetricsCollector(cluster);

        when(cluster.get(id)).thenReturn(null);
        then(collector.metrics(id, Metrics.Type.RATE_LIMIT)).isNull();
        then(collector.metrics(id, Metrics.Type.CIRCUIT_BREAKER)).isNull();
        then(collector.metrics(id, Metrics.Type.CONCURRENT_LIMIT)).isNull();
        then(collector.metrics(id, Metrics.Type.RETRY)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final List<Moat<?>> moats0 = new ArrayList<>(3);
        moats0.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats0.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats0.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));

        final MoatCluster cluster0 = new RetryableMoatCluster(moats0, Collections.emptyList(), null,
                new RetryableExecutor(new RetryOperationsImpl(id,
                        new LinkedList<>(),
                        new ExponentialBackOffPolicy(200, 10_000, 1.0d),
                        new ExceptionPredicate(4),
                        RetryConfig.ofDefault(),
                        null)));
        when(cluster.get(id)).thenReturn(cluster0);

        then(collector.metrics(id, Metrics.Type.RETRY)).isNotNull();
        then(collector.metrics(id, Metrics.Type.CIRCUIT_BREAKER)).isNotNull();
        then(collector.metrics(id, Metrics.Type.CONCURRENT_LIMIT)).isNotNull();
        then(collector.metrics(id, Metrics.Type.RATE_LIMIT)).isNotNull();
    }

    @Test
    void testGetAllMetrics() {
        final ResourceId id = ResourceId.from("testGetMetrics");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final MetricsCollector collector = new MetricsCollector(cluster);

        when(cluster.get(id)).thenReturn(null);
        then(collector.metrics(id, Metrics.Type.RATE_LIMIT)).isNull();
        then(collector.metrics(id, Metrics.Type.CIRCUIT_BREAKER)).isNull();
        then(collector.metrics(id, Metrics.Type.CONCURRENT_LIMIT)).isNull();
        then(collector.metrics(id, Metrics.Type.RETRY)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final List<Moat<?>> moats0 = new ArrayList<>(3);
        moats0.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats0.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats0.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));

        final MoatCluster cluster0 = new RetryableMoatCluster(moats0, Collections.emptyList(), null,
                new RetryableExecutor(new RetryOperationsImpl(id,
                        new LinkedList<>(),
                        new ExponentialBackOffPolicy(200, 10_000, 1.0d),
                        new ExceptionPredicate(4),
                        RetryConfig.ofDefault(),
                        null)));
        when(cluster.get(id)).thenReturn(cluster0);

        then(collector.metricsesOfId(id).get(Metrics.Type.RETRY)).isNotNull();
        then(collector.metricsesOfId(id).get(Metrics.Type.CIRCUIT_BREAKER)).isNotNull();
        then(collector.metricsesOfId(id).get(Metrics.Type.CONCURRENT_LIMIT)).isNotNull();
        then(collector.metricsesOfId(id).get(Metrics.Type.RATE_LIMIT)).isNotNull();
    }

    @Test
    void testGetAllMetricsMap() {
        final ResourceId id0 = ResourceId.from("testGetAllMetricsMap0");
        final ResourceId id1 = ResourceId.from("testGetAllMetricsMap1");

        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final MetricsCollector collector = new MetricsCollector(cluster);

        when(cluster.getAll()).thenReturn(Collections.emptyMap());
        then(collector.all()).isEmpty();

        final Map<ResourceId, MoatCluster> clusters = new HashMap<>(2);

        // MoatCluster0
        final MoatConfig config = new MoatConfig(id0);
        final List<Moat<?>> moats0 = new ArrayList<>(3);
        moats0.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats0.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats0.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        final MoatCluster cluster0 = new RetryableMoatCluster(moats0, Collections.emptyList(), null,
                new RetryableExecutor(new RetryOperationsImpl(id0,
                        new LinkedList<>(),
                        new ExponentialBackOffPolicy(200, 10_000, 1.0d),
                        new ExceptionPredicate(4),
                        RetryConfig.ofDefault(),
                        null)));
        clusters.putIfAbsent(id0, cluster0);

        // MoatCluster1
        final MoatConfig config1 = new MoatConfig(id1);
        final List<Moat<?>> moats1 = new ArrayList<>(3);
        moats1.add(new CircuitBreakerMoat(config1, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats1.add(new ConcurrentLimitMoat(config1, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats1.add(new RateLimitMoat(config1, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        final MoatCluster cluster1 = new RetryableMoatCluster(moats1, Collections.emptyList(), null,
                new RetryableExecutor(new RetryOperationsImpl(id1,
                        new LinkedList<>(),
                        new ExponentialBackOffPolicy(200, 10_000, 1.0d),
                        new ExceptionPredicate(4),
                        RetryConfig.ofDefault(),
                        null)));
        clusters.putIfAbsent(id1, cluster1);

        when(cluster.getAll()).thenReturn(clusters);
        when(cluster.get(id0)).thenReturn(cluster0);
        when(cluster.get(id1)).thenReturn(cluster1);

        then(collector.all().size()).isEqualTo(2);

        then(collector.all().get(id0).get(Metrics.Type.RETRY)).isNotNull();
        then(collector.all().get(id0).get(Metrics.Type.CIRCUIT_BREAKER)).isNotNull();
        then(collector.all().get(id0).get(Metrics.Type.CONCURRENT_LIMIT)).isNotNull();
        then(collector.all().get(id0).get(Metrics.Type.RATE_LIMIT)).isNotNull();

        then(collector.all().get(id1).get(Metrics.Type.RETRY)).isNotNull();
        then(collector.all().get(id1).get(Metrics.Type.CIRCUIT_BREAKER)).isNotNull();
        then(collector.all().get(id1).get(Metrics.Type.CONCURRENT_LIMIT)).isNotNull();
        then(collector.all().get(id1).get(Metrics.Type.RATE_LIMIT)).isNotNull();
    }
}

