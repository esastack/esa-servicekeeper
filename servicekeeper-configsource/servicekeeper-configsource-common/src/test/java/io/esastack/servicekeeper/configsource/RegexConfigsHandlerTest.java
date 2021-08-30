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
package io.esastack.servicekeeper.configsource;

import io.esastack.servicekeeper.configsource.cache.RegexConfigCache;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.GroupConfigSourceImpl;
import io.esastack.servicekeeper.core.configsource.MoatLimitConfigSourceImpl;
import io.esastack.servicekeeper.core.factory.FallbackHandlerFactoryImpl;
import io.esastack.servicekeeper.core.factory.LimitableMoatFactoryContext;
import io.esastack.servicekeeper.core.factory.MoatClusterFactory;
import io.esastack.servicekeeper.core.factory.MoatClusterFactoryImpl;
import io.esastack.servicekeeper.core.factory.PredicateStrategyFactoryImpl;
import io.esastack.servicekeeper.core.factory.SateTransitionProcessorFactoryImpl;
import io.esastack.servicekeeper.core.internal.GlobalConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.internal.MoatCreationLimit;
import io.esastack.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import io.esastack.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import io.esastack.servicekeeper.core.internal.impl.MoatCreationLimitImpl;
import io.esastack.servicekeeper.core.internal.impl.OverLimitMoatHandler;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.MoatStatisticsImpl;
import io.esastack.servicekeeper.core.moats.MoatType;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.retry.RetryOperations;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.BDDAssertions.then;

class RegexConfigsHandlerTest {

    private final InternalMoatCluster cluster = new CacheMoatClusterImpl();

    private final ImmutableConfigs config = new ImmutableConfigsImpl();
    private final GlobalConfig globalConfig = new GlobalConfig();

    private final MoatStatisticsImpl statistics = new MoatStatisticsImpl();

    private final MoatCreationLimit limit = new MoatCreationLimitImpl(statistics,
            new MoatLimitConfigSourceImpl(null, config));

    private final MoatClusterFactory factory = new MoatClusterFactoryImpl(
            LimitableMoatFactoryContext.builder()
                    .strategy(new PredicateStrategyFactoryImpl())
                    .fallbackHandler(new FallbackHandlerFactoryImpl())
                    .processors(emptyList())
                    .limite(limit)
                    .listeners(singletonList(statistics))
                    .cProcessors(new SateTransitionProcessorFactoryImpl())
                    .build(), cluster, config);

    private final InternalsUpdater updater = new InternalsUpdaterImpl(cluster,
            new GroupConfigSourceImpl(null, config), factory, globalConfig,
            singletonList(new OverLimitMoatHandler(cluster, config)));

    private final RegexConfigCache cache = new RegexConfigCache();
    private final RegexConfigsHandler handler = new RegexConfigsHandler(cache, updater);

    @Test
    void testAddRegex() {
        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testAddRegex" + i))).isNull();
        }

        final ResourceId regexId = ResourceId.from("testAddRegex.*", true);
        final ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(100);
        config.setLimitForPeriod(200);
        config.setFailureRateThreshold(50.0f);
        handler.update(singletonMap(regexId, config));

        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testAddRegex" + i))).isEqualTo(config);
        }
    }

    @Test
    void testRemoveRegex() {
        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testRemoveRegex" + i))).isNull();
        }

        final ResourceId regexId = ResourceId.from("testRemoveRegex.*", true);
        final ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(100);
        config.setLimitForPeriod(200);
        config.setFailureRateThreshold(50.0f);
        handler.update(singletonMap(regexId, config));

        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testRemoveRegex" + i))).isEqualTo(config);
            factory.getOrCreate(ResourceId.from("testRemoveRegex" + i), () -> null, () -> null, () -> config, false);
        }
        then(cluster.getAll().size()).isEqualTo(10);

        handler.update(emptyMap());
        then(cluster.getAll()).isEmpty();
    }

    @Test
    void testUpdateRegex() {
        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testUpdateRegex" + i))).isNull();
        }

        final ResourceId regexId = ResourceId.from("testUpdateRegex.*", true);
        final ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(100);
        config.setLimitForPeriod(200);
        config.setFailureRateThreshold(50.0f);
        handler.update(singletonMap(regexId, config));

        for (int i = 0; i < 10; i++) {
            then(cache.configOf(ResourceId.from("testUpdateRegex" + i))).isEqualTo(config);
            factory.getOrCreate(ResourceId.from("testUpdateRegex" + i), () -> null, () -> null, () -> config, false);
        }
        then(cluster.getAll().size()).isEqualTo(10);

        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxConcurrentLimit(10);
        config0.setLimitForPeriod(20);
        config0.setFailureRateThreshold(40.0f);
        config0.setMaxAttempts(5);

        handler.update(singletonMap(regexId, config0));
        then(cluster.getAll().size()).isEqualTo(10);

        MoatCluster cluster0;
        for (int i = 0; i < 10; i++) {
            cluster0 = cluster.get(ResourceId.from("testUpdateRegex" + i));

            CircuitBreakerMoat moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
            assert moat0 != null;
            then(moat0.config().getFailureRateThreshold()).isEqualTo(40.0f);
            then(moat0.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(40.0f);

            ConcurrentLimitMoat moat1 = (ConcurrentLimitMoat) getByType(cluster0, CONCURRENT_LIMIT);
            assert moat1 != null;
            then(moat1.config().getThreshold()).isEqualTo(10);
            then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(10);

            RateLimitMoat moat2 = (RateLimitMoat) getByType(cluster0, RATE_LIMIT);
            assert moat2 != null;
            then(moat2.config().getLimitForPeriod()).isEqualTo(20);
            then(moat2.rateLimiter().config().getLimitForPeriod()).isEqualTo(20);

            RetryOperations operations = ((RetryableMoatCluster) cluster0).retryExecutor().getOperations();
            then(operations.getConfig().getMaxAttempts()).isEqualTo(5);
        }
    }

    private Moat<?> getByType(final MoatCluster cluster, MoatType type) {
        List<Moat<?>> moats = cluster.getAll();
        if (moats == null || moats.isEmpty()) {
            return null;
        }

        if (type == null) {
            return null;
        }

        for (Moat<?> moat : moats) {
            if (type.equals(moat.type())) {
                return moat;
            }
        }

        return null;
    }
}
