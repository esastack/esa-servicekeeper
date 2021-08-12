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
package esa.servicekeeper.core.moats;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

class MoatClusterImplTest {

    @Test
    void testConstructSort() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final MoatConfig config = new MoatConfig(ResourceId.from("testConstructSort"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null, new PredicateByException());
        moats.add(circuitBreakerMoat);

        RateLimitMoat limitMoat1 = new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat1);

        ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat2);

        MoatCluster cluster = new MoatClusterImpl(moats, null);
        List<Moat<?>> sortedMoat = cluster.getAll();
        then(sortedMoat.size()).isEqualTo(3);
        then(sortedMoat.get(0)).isInstanceOf(RateLimitMoat.class);
        then(sortedMoat.get(1)).isInstanceOf(ConcurrentLimitMoat.class);
        then(sortedMoat.get(2)).isInstanceOf(CircuitBreakerMoat.class);
    }

    @Test
    void testAdd() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        CircuitBreakerMoat breakerMoat = mock(CircuitBreakerMoat.class);
        moats.add(breakerMoat);

        MoatCluster cluster = new MoatClusterImpl(moats, null);
        then(cluster.getAll().size()).isEqualTo(1);
        cluster.add(mock(ConcurrentLimitMoat.class));
        cluster.add(mock(RateLimitMoat.class));
        then(cluster.getAll().size()).isEqualTo(3);
        then(cluster.getAll().get(0)).isInstanceOf(CircuitBreakerMoat.class);
        then(cluster.getAll().get(1)).isInstanceOf(ConcurrentLimitMoat.class);
        then(cluster.getAll().get(2)).isInstanceOf(RateLimitMoat.class);
    }

    @Test
    void testAddSort() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final MoatConfig config = new MoatConfig(ResourceId.from("testAddSort"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null, new PredicateByException());
        moats.add(circuitBreakerMoat);

        RateLimitMoat limitMoat1 = new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat1);

        MoatCluster cluster = new MoatClusterImpl(moats, null);

        ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList());
        cluster.add(limitMoat2);

        List<Moat<?>> sortedMoat = cluster.getAll();
        then(sortedMoat.size()).isEqualTo(3);
        then(sortedMoat.get(0)).isInstanceOf(RateLimitMoat.class);
        then(sortedMoat.get(1)).isInstanceOf(ConcurrentLimitMoat.class);
        then(sortedMoat.get(2)).isInstanceOf(CircuitBreakerMoat.class);
    }

    @Test
    void testRemove() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final CircuitBreakerMoat breakerMoat = mock(CircuitBreakerMoat.class);
        final ConcurrentLimitMoat concurrentMoat = mock(ConcurrentLimitMoat.class);
        final RateLimitMoat rateLimitMoat = mock(RateLimitMoat.class);

        moats.add(breakerMoat);
        moats.add(concurrentMoat);
        moats.add(rateLimitMoat);
        MoatCluster cluster = new MoatClusterImpl(moats, null);
        then(cluster.getAll().size()).isEqualTo(3);
        cluster.remove(rateLimitMoat);
        cluster.remove(concurrentMoat);
        cluster.remove(breakerMoat);
        then(cluster.getAll().size()).isEqualTo(0);

        cluster.remove(breakerMoat);
        cluster.remove(concurrentMoat);
        cluster.remove(rateLimitMoat);
        then(cluster.getAll().size()).isEqualTo(0);
    }

    @Test
    void testGetAll() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final CircuitBreakerMoat breakerMoat = mock(CircuitBreakerMoat.class);

        moats.add(breakerMoat);
        MoatCluster cluster = new MoatClusterImpl(moats, null);
        then(cluster.getAll().size()).isEqualTo(1);

        cluster.add(mock(ConcurrentLimitMoat.class));
        then(cluster.getAll().size()).isEqualTo(2);
        cluster.add(mock(RateLimitMoat.class));
        then(cluster.getAll().size()).isEqualTo(3);
    }

    @Test
    void testContains() {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final MoatConfig config = new MoatConfig(ResourceId.from("testContains"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());
        moats.add(circuitBreakerMoat);

        RateLimitMoat limitMoat1 = new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat1);

        MoatCluster cluster = new MoatClusterImpl(moats, null);
        List<Moat<?>> sortedMoat = cluster.getAll();
        then(sortedMoat.size()).isEqualTo(2);
        then(cluster.contains(CIRCUIT_BREAKER)).isTrue();
        then(cluster.contains(RATE_LIMIT)).isTrue();
        then(cluster.contains(CONCURRENT_LIMIT)).isFalse();
    }

    @Test
    void testDeleteByType() {
        final ResourceId resourceId = ResourceId.from("testDeleteByType");

        final MoatConfig config = new MoatConfig(resourceId);
        final ServiceKeeperConfig config0 = ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                .rateLimiterConfig(RateLimitConfig.ofDefault())
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();

        final List<Moat<?>> moats = new ArrayList<>(3);
        moats.add(new RateLimitMoat(config, config0.getRateLimitConfig(),
                null, null));

        moats.add(new ConcurrentLimitMoat(config, config0.getConcurrentLimitConfig(),
                null, null));
        moats.add(new CircuitBreakerMoat(config, config0.getCircuitBreakerConfig(), null,
                new PredicateByException()));

        MoatCluster cluster = new MoatClusterImpl(moats, null);
        then(cluster.getAll().size()).isEqualTo(3);
        cluster.remove(MoatType.CIRCUIT_BREAKER);
        then(cluster.getAll().size()).isEqualTo(2);
        cluster.remove(MoatType.CIRCUIT_BREAKER);
        then(cluster.getAll().size()).isEqualTo(2);

        cluster.remove(MoatType.RATE_LIMIT);
        then(cluster.getAll().size()).isEqualTo(1);
        cluster.remove(MoatType.RATE_LIMIT);
        then(cluster.getAll().size()).isEqualTo(1);

        cluster.remove(MoatType.CONCURRENT_LIMIT);
        then(cluster.getAll().size()).isEqualTo(0);
        cluster.remove(MoatType.CONCURRENT_LIMIT);
        then(cluster.getAll().size()).isEqualTo(0);
    }

    @Test
    void testParallelOperate() throws InterruptedException {
        List<Moat<?>> moats = new CopyOnWriteArrayList<>();
        final MoatCluster cluster = new MoatClusterImpl(moats, null);

        final MoatConfig config = new MoatConfig(ResourceId.from("testParallelOperate"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null,
                new PredicateByException());

        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    cluster.add(circuitBreakerMoat);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        then(cluster.getAll().size()).isEqualTo(5);

        final CountDownLatch latch1 = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    cluster.remove(circuitBreakerMoat);
                } finally {
                    latch1.countDown();
                }
            }).start();
        }
        latch1.await();
        then(cluster.getAll().size()).isEqualTo(0);

        final CountDownLatch latch2 = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) {
                try {
                    cluster.add(circuitBreakerMoat);
                } finally {
                    latch2.countDown();
                }
            } else {
                try {
                    cluster.remove(circuitBreakerMoat);
                } finally {
                    latch2.countDown();
                }
            }
        }
        latch2.await();
    }
}
