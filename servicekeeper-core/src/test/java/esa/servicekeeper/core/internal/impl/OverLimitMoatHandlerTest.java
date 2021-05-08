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
package esa.servicekeeper.core.internal.impl;

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.MoatClusterImpl;
import esa.servicekeeper.core.moats.MoatType;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static org.assertj.core.api.BDDAssertions.then;

class OverLimitMoatHandlerTest {

    private final ResourceId id = ResourceId.from("OverLimitMoatHandlerTest");
    private final InternalMoatCluster cluster = new CacheMoatClusterImpl();
    private final OverLimitMoatHandler handler = new OverLimitMoatHandler(cluster, new ImmutableConfigsImpl());
    private final String argName = "arg0";

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 100; i++) {
            cluster.computeIfAbsent(new ArgResourceId(id, argName, "value" + i), (id) -> {
                final List<Moat<?>> moats = new ArrayList<>(3);
                final MoatConfig config = new MoatConfig(id, null);
                moats.add(new ConcurrentLimitMoat(config, ConcurrentLimitConfig.ofDefault(),
                        null, null));

                moats.add(new RateLimitMoat(config, RateLimitConfig.ofDefault(),
                        null, null));

                moats.add(new CircuitBreakerMoat(config, CircuitBreakerConfig.ofDefault(), null,
                        new PredicateByException()));

                return new MoatClusterImpl(moats, null);
            });
        }
    }

    @Test
    void testOnUpdate() {
        then(cluster.getAll().size()).isEqualTo(100);
        then(countByType(CIRCUIT_BREAKER)).isEqualTo(100);
        then(countByType(RATE_LIMIT)).isEqualTo(100);
        then(countByType(CONCURRENT_LIMIT)).isEqualTo(100);

        final ArgConfigKey key0 = new ArgConfigKey(id, argName, CIRCUIT_BREAKER);
        handler.onUpdate(key0, null, null);
        then(countByType(CIRCUIT_BREAKER)).isEqualTo(100);
        handler.onUpdate(key0, null, 0);
        then(countByType(RATE_LIMIT)).isEqualTo(100);
        then(countByType(CONCURRENT_LIMIT)).isEqualTo(100);

        final ArgConfigKey key1 = new ArgConfigKey(id, argName, RATE_LIMIT);
        handler.onUpdate(key1, null, null);
        then(countByType(CIRCUIT_BREAKER)).isEqualTo(0);
        then(countByType(RATE_LIMIT)).isEqualTo(100);

        handler.onUpdate(key1, null, 0);
        then(countByType(RATE_LIMIT)).isEqualTo(0);
        then(countByType(CONCURRENT_LIMIT)).isEqualTo(100);

        final ArgConfigKey key2 = new ArgConfigKey(id, argName, CONCURRENT_LIMIT);
        handler.onUpdate(key2, null, null);
        then(countByType(CIRCUIT_BREAKER)).isEqualTo(0);
        then(countByType(RATE_LIMIT)).isEqualTo(0);
        then(countByType(CONCURRENT_LIMIT)).isEqualTo(100);

        handler.onUpdate(key2, null, 0);
        then(countByType(CONCURRENT_LIMIT)).isEqualTo(0);
    }

    private int countByType(final MoatType type) {
        final AtomicInteger count = new AtomicInteger(0);
        cluster.getAll().forEach((k, v) -> {
            if (v.contains(type)) {
                count.incrementAndGet();
            }
        });

        return count.get();
    }
}
