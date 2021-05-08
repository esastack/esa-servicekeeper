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

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.fallback.FallbackToValue;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class MoatStatisticsImplTest {

    @Test
    void testConstruct0() {
        final MoatStatisticsImpl statistics = new MoatStatisticsImpl();

        final ResourceId methodId = ResourceId.from("testConstruct0");
        final List<Moat<?>> moats = new ArrayList<>(3);
        final MoatConfig config = new MoatConfig(methodId, new FallbackToValue("String"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null, new PredicateByException());
        moats.add(circuitBreakerMoat);

        RateLimitMoat limitMoat1 = new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat1);

        ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat2);

        new MoatClusterImpl(moats, Collections.singletonList(statistics));
        then(statistics.totalCount()).isEqualTo(3);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(1);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(1);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(1);

        then(statistics.countOf(new ArgConfigKey(methodId, "arg0", CIRCUIT_BREAKER))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, "arg0", RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, "arg0", CONCURRENT_LIMIT))).isEqualTo(0);
    }

    @Test
    void testConstruct1() {
        final MoatStatisticsImpl statistics = new MoatStatisticsImpl();

        final ResourceId methodId = ResourceId.from("testConstruct1");
        final String argName = "arg0";
        final ArgResourceId argId = new ArgResourceId(methodId, argName, "foo");

        final List<Moat<?>> moats = new ArrayList<>(3);
        final MoatConfig config = new MoatConfig(argId, new FallbackToValue("String"));
        CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                CircuitBreakerConfig.ofDefault(), null, new PredicateByException());
        moats.add(circuitBreakerMoat);

        RateLimitMoat limitMoat1 = new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat1);

        ConcurrentLimitMoat limitMoat2 = new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                null, Collections.emptyList());
        moats.add(limitMoat2);

        new MoatClusterImpl(moats, Collections.singletonList(statistics));
        then(statistics.totalCount()).isEqualTo(3);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(1);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(1);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(1);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(1);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(1);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(1);
    }

    @Test
    void testStatistics() {
        final MoatStatisticsImpl statistics = new MoatStatisticsImpl();

        final ResourceId methodId = ResourceId.from("testStatistics");
        final String argName = "arg0";

        final Moat<?>[] moats = new Moat[100];
        final MoatCluster[] clusters = new MoatCluster[100];

        // Add
        for (int i = 0; i < 100; i++) {
            final MoatCluster cluster = new MoatClusterImpl(null, Collections.singletonList(statistics));

            clusters[i] = cluster;
            final MoatConfig config = new MoatConfig(new ArgResourceId(methodId, argName, "foo" + i),
                    new FallbackToValue("String"));
            CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                    CircuitBreakerConfig.ofDefault(), null,
                    new PredicateByException());
            cluster.add(circuitBreakerMoat);
            moats[i] = circuitBreakerMoat;
        }

        then(statistics.totalCount()).isEqualTo(100);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(100);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(100);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);


        // Remove
        for (int i = 0; i < 99; i++) {
            clusters[i].remove(moats[i]);
        }

        then(statistics.totalCount()).isEqualTo(1);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(1);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(1);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);


        // Remove through type
        clusters[99].remove(CIRCUIT_BREAKER);
        then(statistics.totalCount()).isEqualTo(0);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(0);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);
    }

    @Test
    void testParallel() throws InterruptedException {
        final MoatStatisticsImpl statistics = new MoatStatisticsImpl();

        final ResourceId methodId = ResourceId.from("testParallel");
        final String argName = "arg0";

        final Moat<?>[] moats = new Moat[10];
        final MoatCluster[] clusters = new MoatCluster[10];

        final CountDownLatch latch = new CountDownLatch(10);
        // Add
        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                final MoatCluster cluster = new MoatClusterImpl(null, Collections.singletonList(statistics));

                clusters[index] = cluster;
                final MoatConfig config = new MoatConfig(new ArgResourceId(methodId, argName, "foo" + index),
                        new FallbackToValue("String"));
                CircuitBreakerMoat circuitBreakerMoat = new CircuitBreakerMoat(config,
                        CircuitBreakerConfig.ofDefault(), null,
                        new PredicateByException());
                cluster.add(circuitBreakerMoat);
                moats[index] = circuitBreakerMoat;

                latch.countDown();
            }).start();
        }

        latch.await();

        then(statistics.totalCount()).isEqualTo(10);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(10);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(10);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);


        // Remove
        final CountDownLatch latch1 = new CountDownLatch(9);
        for (int i = 0; i < 9; i++) {
            final int index = i;
            new Thread(() -> {
                clusters[index].remove(moats[index]);
                latch1.countDown();
            }).start();
        }
        latch1.await();

        then(statistics.totalCount()).isEqualTo(1);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(1);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(1);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);


        // Remove through type
        clusters[9].remove(CIRCUIT_BREAKER);
        then(statistics.totalCount()).isEqualTo(0);

        then(statistics.countOf(CIRCUIT_BREAKER)).isEqualTo(0);
        then(statistics.countOf(RATE_LIMIT)).isEqualTo(0);
        then(statistics.countOf(CONCURRENT_LIMIT)).isEqualTo(0);

        then(statistics.countOf(new ArgConfigKey(methodId, argName, CIRCUIT_BREAKER))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, RATE_LIMIT))).isEqualTo(0);
        then(statistics.countOf(new ArgConfigKey(methodId, argName, CONCURRENT_LIMIT))).isEqualTo(0);
    }

}
