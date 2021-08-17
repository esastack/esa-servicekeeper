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
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.MoatClusterImpl;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealTimeConfigCollectorTest {

    @Test
    void testGetCircuitBreakerConfig() {
        final ResourceId id = ResourceId.from("testGetCircuitBreakerConfig");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.circuitBreakerConfig(id)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                        null,
                        new PredicateByException())),
                Collections.emptyList());

        clusters.putIfAbsent(id, cluster0);
        final CircuitBreakerConfig config0 = collector.circuitBreakerConfig(id);
        then(config0).isNotNull();
        then(config0).isEqualTo(CircuitBreakerConfig.ofDefault());
    }

    @Test
    void testGetConcurrentLimitConfig() {
        final ResourceId id = ResourceId.from("testGetConcurrentLimitConfig");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.concurrentLimitConfig(id)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        clusters.putIfAbsent(id, cluster0);
        final ConcurrentLimitConfig config0 = collector.concurrentLimitConfig(id);
        then(config0).isNotNull();
        then(config0).isEqualTo(ConcurrentLimitConfig.ofDefault());
    }

    @Test
    void testGetRateLimitConfig() {
        final ResourceId id = ResourceId.from("testGetRateLimitConfig");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.rateLimitConfig(id)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        clusters.putIfAbsent(id, cluster0);
        final RateLimitConfig config0 = collector.rateLimitConfig(id);
        then(config0).isNotNull();
        then(config0).isEqualTo(RateLimitConfig.ofDefault());
    }

    @Test
    void testGetServiceKeeperConfig() {
        final ResourceId id = ResourceId.from("testGetServiceKeeperConfig");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.config(id)).isNull();
        final MoatConfig config = new MoatConfig(id);

        final List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));

        final MoatCluster cluster0 = new MoatClusterImpl(moats, Collections.emptyList());

        clusters.putIfAbsent(id, cluster0);
        final ServiceKeeperConfig config0 = collector.config(id);
        then(config0).isNotNull();
        then(config0.getRateLimitConfig()).isEqualTo(RateLimitConfig.ofDefault());
        then(config0.getCircuitBreakerConfig()).isEqualTo(CircuitBreakerConfig.ofDefault());
        then(config0.getConcurrentLimitConfig()).isEqualTo(ConcurrentLimitConfig.ofDefault());
    }

    @Test
    void testGetCircuitBreakerConfigs() {
        final ResourceId id0 = ResourceId.from("testGetCircuitBreakerConfigs0");
        final ResourceId id1 = ResourceId.from("testGetCircuitBreakerConfigs1");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.circuitBreakerConfigs()).isEmpty();
        final MoatConfig config = new MoatConfig(id0);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                        null,
                        new PredicateByException())),
                Collections.emptyList());

        final MoatCluster cluster1 = new MoatClusterImpl(Collections.singletonList(
                new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                        null,
                        new PredicateByException())),
                Collections.emptyList());

        clusters.putIfAbsent(id0, cluster0);
        clusters.putIfAbsent(id1, cluster1);
        final Map<ResourceId, CircuitBreakerConfig> configs = collector.circuitBreakerConfigs();
        then(configs).isNotNull();
        then(configs.size()).isEqualTo(2);
        then(configs.get(id0)).isEqualTo(CircuitBreakerConfig.ofDefault());
        then(configs.get(id1)).isEqualTo(CircuitBreakerConfig.ofDefault());
    }

    @Test
    void testGetConcurrentLimitConfigs() {
        final ResourceId id0 = ResourceId.from("testGetConcurrentLimitConfigs0");
        final ResourceId id1 = ResourceId.from("testGetConcurrentLimitConfigs1");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.concurrentLimitConfigs()).isEmpty();
        final MoatConfig config = new MoatConfig(id0);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        final MoatCluster cluster1 = new MoatClusterImpl(Collections.singletonList(
                new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        clusters.putIfAbsent(id0, cluster0);
        clusters.putIfAbsent(id1, cluster1);
        final Map<ResourceId, ConcurrentLimitConfig> configs = collector.concurrentLimitConfigs();
        then(configs).isNotNull();
        then(configs.size()).isEqualTo(2);
        then(configs.get(id0)).isEqualTo(ConcurrentLimitConfig.ofDefault());
        then(configs.get(id1)).isEqualTo(ConcurrentLimitConfig.ofDefault());
    }

    @Test
    void testGetRateLimitConfigs() {
        final ResourceId id0 = ResourceId.from("testGetRateLimitConfigs0");
        final ResourceId id1 = ResourceId.from("testGetRateLimitConfigs1");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.rateLimitConfigs()).isEmpty();
        final MoatConfig config = new MoatConfig(id0);

        final MoatCluster cluster0 = new MoatClusterImpl(Collections.singletonList(
                new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        final MoatCluster cluster1 = new MoatClusterImpl(Collections.singletonList(
                new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                        null,
                        Collections.emptyList())),
                Collections.emptyList());

        clusters.putIfAbsent(id0, cluster0);
        clusters.putIfAbsent(id1, cluster1);
        final Map<ResourceId, RateLimitConfig> configs = collector.rateLimitConfigs();
        then(configs).isNotNull();
        then(configs.size()).isEqualTo(2);
        then(configs.get(id0)).isEqualTo(RateLimitConfig.ofDefault());
        then(configs.get(id1)).isEqualTo(RateLimitConfig.ofDefault());
    }

    @Test
    void testGetServiceKeeperConfigs() {
        final ResourceId id0 = ResourceId.from("testGetServiceKeeperConfigs0");
        final ResourceId id1 = ResourceId.from("testGetServiceKeeperConfigs1");
        final InternalMoatCluster cluster = mock(InternalMoatCluster.class);
        final RealTimeConfigCollector collector = new RealTimeConfigCollector(cluster);

        Map<ResourceId, MoatCluster> clusters = new HashMap<>();
        when(cluster.getAll()).thenReturn(clusters);
        then(collector.configs()).isEmpty();
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

        final MoatCluster cluster0 = new MoatClusterImpl(moats0, Collections.emptyList());

        final List<Moat<?>> moats1 = new ArrayList<>(3);
        moats1.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(),
                null,
                new PredicateByException()));
        moats1.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));
        moats1.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null,
                Collections.emptyList()));

        final MoatCluster cluster1 = new MoatClusterImpl(moats1, Collections.emptyList());

        clusters.putIfAbsent(id0, cluster0);
        clusters.putIfAbsent(id1, cluster1);
        final Map<ResourceId, ServiceKeeperConfig> configs = collector.configs();
        then(configs).isNotNull();
        then(configs.size()).isEqualTo(2);
        then(configs.get(id0).getConcurrentLimitConfig()).isEqualTo(ConcurrentLimitConfig.ofDefault());
        then(configs.get(id0).getRateLimitConfig()).isEqualTo(RateLimitConfig.ofDefault());
        then(configs.get(id0).getCircuitBreakerConfig()).isEqualTo(CircuitBreakerConfig.ofDefault());

        then(configs.get(id1).getConcurrentLimitConfig()).isEqualTo(ConcurrentLimitConfig.ofDefault());
        then(configs.get(id1).getRateLimitConfig()).isEqualTo(RateLimitConfig.ofDefault());
        then(configs.get(id1).getCircuitBreakerConfig()).isEqualTo(CircuitBreakerConfig.ofDefault());
    }
}
