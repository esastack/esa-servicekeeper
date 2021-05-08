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

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.MoatClusterImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static esa.servicekeeper.core.internal.impl.CacheMoatClusterImpl.DEFAULT_CACHE_SIZE_KEY;
import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class CacheMoatClusterImplTest {

    private final InternalMoatCluster cluster = new CacheMoatClusterImpl();

    @Test
    void testGet() {
        then(cluster.get(ResourceId.from("testGet"))).isNull();
    }

    @Test
    void testGetAll() {
        then(cluster.getAll()).isEmpty();
        for (int i = 0; i < 10; i++) {
            cluster.computeIfAbsent(ResourceId.from("testGetAll" + i),
                    (id) -> new MoatClusterImpl(null, null));
        }

        final Map<ResourceId, MoatCluster> cluster0 = cluster.getAll();
        then(cluster0.size()).isEqualTo(10);
    }

    @Test
    void testRemove() {
        cluster.remove(null);
        final ResourceId id = ResourceId.from("testRemove");
        cluster.remove(id);

        cluster.computeIfAbsent(id, (id0) -> new MoatClusterImpl(null, null));
        then(cluster.getAll().size()).isEqualTo(1);
        cluster.remove(id);
        then(cluster.getAll()).isEmpty();
    }

    @Test
    void testParallel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    cluster.computeIfAbsent(ResourceId.from("testParallel"),
                            (id) -> new MoatClusterImpl(null, null));
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        then(cluster.getAll().size()).isEqualTo(1);

        cluster.computeIfAbsent(ResourceId.from("testParallel0"), (id0) -> null);
        then(cluster.getAll().size()).isEqualTo(1);
        cluster.computeIfAbsent(ResourceId.from("testParallel0"), null);

        cluster.remove(ResourceId.from("testParallel"));
        then(cluster.getAll()).isEmpty();

        final CountDownLatch latch1 = new CountDownLatch(10);
        for (int i = 0; i < 5; i++) {
            final int index = i;
            if (i % 3 == 0) {
                new Thread(() -> {
                    try {
                        cluster.computeIfAbsent(ResourceId.from("testParallel" + index),
                                (id) -> new MoatClusterImpl(null, null));
                    } finally {
                        latch1.countDown();
                    }
                }).start();
            } else if (i % 3 == 1) {
                new Thread(() -> {
                    try {
                        cluster.getAll();
                    } finally {
                        latch1.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        cluster.remove(ResourceId.from("testParallel" + index));
                    } finally {
                        latch1.countDown();
                    }
                }).start();
            }
        }

        latch.await();
    }

    @Test
    void testMaxSizeLimit() {
        final int maxSize = 100;
        System.setProperty(DEFAULT_CACHE_SIZE_KEY, String.valueOf(maxSize));
        final InternalMoatCluster cluster = new CacheMoatClusterImpl();

        for (int i = 0; i < maxSize; i++) {
            cluster.computeIfAbsent(ResourceId.from("testMaxSizeLimit" + i),
                    (id) -> new MoatClusterImpl(null, null));
        }

        then(cluster.getAll().size()).isEqualTo(maxSize);
        for (int i = maxSize; i < 2 * maxSize; i++) {
            cluster.computeIfAbsent(ResourceId.from("testMaxSizeLimit" + i),
                    (id) -> new MoatClusterImpl(null, null));
        }

        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + 500L);
        then(cluster.getAll().size() < maxSize * 2).isTrue();

        for (int i = 0; i < 2 * maxSize; i++) {
            cluster.remove(ResourceId.from("testMaxSizeLimit" + i));
        }

        then(cluster.getAll()).isEmpty();

        System.clearProperty(DEFAULT_CACHE_SIZE_KEY);
    }
}
