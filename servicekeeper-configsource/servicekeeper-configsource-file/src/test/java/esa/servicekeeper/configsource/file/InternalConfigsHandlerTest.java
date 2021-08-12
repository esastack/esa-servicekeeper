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
package esa.servicekeeper.configsource.file;

import esa.servicekeeper.configsource.InternalsUpdater;
import esa.servicekeeper.configsource.InternalsUpdaterImpl;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.MoatLimitConfigSourceImpl;
import esa.servicekeeper.core.factory.FallbackHandlerFactoryImpl;
import esa.servicekeeper.core.factory.LimitableMoatFactoryContext;
import esa.servicekeeper.core.factory.MoatClusterFactory;
import esa.servicekeeper.core.factory.MoatClusterFactoryImpl;
import esa.servicekeeper.core.factory.PredicateStrategyFactoryImpl;
import esa.servicekeeper.core.factory.SateTransitionProcessorFactoryImpl;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.internal.MoatCreationLimit;
import esa.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import esa.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import esa.servicekeeper.core.internal.impl.MoatCreationLimitImpl;
import esa.servicekeeper.core.internal.impl.OverLimitMoatHandler;
import esa.servicekeeper.core.moats.MoatStatisticsImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

class InternalConfigsHandlerTest {

    private static final InternalMoatCluster CLUSTER = new CacheMoatClusterImpl();
    private static final ImmutableConfigs CONFIG = new ImmutableConfigsImpl();
    private static final GlobalConfig GLOBAL_CONFIG = new GlobalConfig();

    private static final MoatStatisticsImpl STATISTICS = new MoatStatisticsImpl();
    private static final PropertyFileConfigCache CACHE = SingletonFactory.cache();

    private static final MoatCreationLimit LIMIT = new MoatCreationLimitImpl(STATISTICS,
            new MoatLimitConfigSourceImpl(new PropertyFileConfigSource(CACHE), CONFIG));

    private static final MoatClusterFactory FACTORY = new MoatClusterFactoryImpl(
            LimitableMoatFactoryContext.builder()
                    .strategy(new PredicateStrategyFactoryImpl())
                    .fallbackHandler(new FallbackHandlerFactoryImpl())
                    .limite(LIMIT)
                    .processors(Collections.emptyList())
                    .listeners(Collections.singletonList(STATISTICS))
                    .cProcessors(new SateTransitionProcessorFactoryImpl())
                    .build(), CLUSTER, CONFIG);

    private static final InternalsUpdater UPDATER = new InternalsUpdaterImpl(CLUSTER,
            new InternalGroupConfigSource(), FACTORY, GLOBAL_CONFIG,
            Collections.singletonList(new OverLimitMoatHandler(CLUSTER, CONFIG)));

    private static final InternalConfigsHandler HANDLER = SingletonFactory.handler(UPDATER);

    @Test
    void testHandleGlobalConfigs() {
        then(GLOBAL_CONFIG.globalDisable()).isFalse();
        then(GLOBAL_CONFIG.argLevelEnable()).isTrue();
        then(GLOBAL_CONFIG.retryEnable()).isTrue();

        HANDLER.updateGlobalConfigs(true, false, false);

        then(GLOBAL_CONFIG.globalDisable()).isTrue();
        then(GLOBAL_CONFIG.retryEnable()).isFalse();
        then(GLOBAL_CONFIG.argLevelEnable()).isFalse();

        HANDLER.updateGlobalConfigs(null, null, null);

        then(GLOBAL_CONFIG.globalDisable()).isFalse();
        then(GLOBAL_CONFIG.argLevelEnable()).isTrue();
        then(GLOBAL_CONFIG.retryEnable()).isTrue();
    }

    @Test
    void testUpdateMaxSizeLimits() {
        // It's not permitted to create when max size LIMIT is null
        for (int i = 0; i < 200; i++) {
            FACTORY.getOrCreateOfArg(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> null);
        }
        then(CLUSTER.getAll().size()).isEqualTo(0);

        final ExternalConfig config = new ExternalConfig();
        config.setFailureRateThreshold(60.0f);
        config.setLimitForPeriod(100);
        config.setMaxConcurrentLimit(20);
        for (int i = 0; i < 200; i++) {
            FACTORY.getOrCreateOfArg(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> config);
        }
        then(CLUSTER.getAll().size()).isEqualTo(101);
        for (int i = 0; i < 101; i++) {
            then(CLUSTER.get(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i))
                    .getAll().size()).isEqualTo(3);
        }

        // It's permitted to create when max size LIMIT is not null
        final Map<ArgConfigKey, Integer> maxSizeLimits = new HashMap<>(2);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CIRCUIT_BREAKER), 200);
        HANDLER.updateMaxSizeLimits(maxSizeLimits);

        for (int i = 0; i < 200; i++) {
            FACTORY.getOrCreateOfArg(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> config);
        }
        then(CLUSTER.getAll().size()).isEqualTo(200);

        for (int i = 101; i < 200; i++) {
            then(CLUSTER.get(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i))
                    .getAll().size()).isEqualTo(1);
        }

        // Decrement the max size LIMIT
        maxSizeLimits.clear();
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CIRCUIT_BREAKER), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", RATE_LIMIT), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CONCURRENT_LIMIT), 0);

        HANDLER.updateMaxSizeLimits(maxSizeLimits);
        then(CLUSTER.getAll().size()).isEqualTo(0);
    }

    @Test
    void testParallel() throws InterruptedException {
        final Map<ArgConfigKey, Integer> maxSizeLimits = new HashMap<>(3);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", CIRCUIT_BREAKER), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", RATE_LIMIT), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", CONCURRENT_LIMIT), 0);

        final CountDownLatch latch = new CountDownLatch(6);
        for (int i = 0; i < 6; i++) {
            if (i % 2 == 0) {
                new Thread(() -> {
                    try {
                        HANDLER.updateMaxSizeLimits(maxSizeLimits);
                    } catch (Throwable th) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        HANDLER.updateMaxSizeLimits(emptyMap());
                    } catch (Throwable th) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
        }

        latch.await();
    }

    private static class InternalGroupConfigSource implements GroupConfigSource {
        @Override
        public Map<GroupResourceId, ExternalConfig> allGroups() {
            return null;
        }

        @Override
        public ExternalConfig config(GroupResourceId groupId) {
            return null;
        }

        @Override
        public GroupResourceId mappingGroupId(ResourceId methodId) {
            return null;
        }

        @Override
        public Set<ResourceId> mappingResourceIds(GroupResourceId groupId) {
            return null;
        }
    }
}
