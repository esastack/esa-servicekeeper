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
package esa.servicekeeper.configsource;

import esa.servicekeeper.configsource.cache.ConfigCache;
import esa.servicekeeper.configsource.cache.ConfigCacheImp;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.*;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;
import esa.servicekeeper.core.configsource.GroupConfigSourceImpl;
import esa.servicekeeper.core.configsource.MoatLimitConfigSourceImpl;
import esa.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import esa.servicekeeper.core.factory.*;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.internal.MoatCreationLimit;
import esa.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import esa.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import esa.servicekeeper.core.internal.impl.MoatCreationLimitImpl;
import esa.servicekeeper.core.internal.impl.OverLimitMoatHandler;
import esa.servicekeeper.core.moats.*;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.retry.RetryOperations;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static esa.servicekeeper.core.moats.MoatType.*;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.*;
import static esa.servicekeeper.core.utils.BeanUtils.newAs;
import static esa.servicekeeper.core.utils.ClassCastUtils.cast;
import static java.util.Collections.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigsHandlerImplTest {

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

    private final ConfigCache cache = new ConfigCacheImp();
    private final ConfigsHandlerImpl handler = new ConfigsHandlerImpl(cache, updater);

    @Test
    void testUpdateConfig() {
        final ResourceId resourceId = ResourceId.from("testUpdateConfig");
        final RetryableMoatCluster cluster0 = factory.getOrCreateOfMethod(resourceId, () -> null,
                () -> ServiceKeeperConfig.builder()
                        .retryConfig(RetryConfig.ofDefault())
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), () -> null);
        then(cluster0.getAll().size()).isEqualTo(3);
        then(cluster0).isInstanceOf(RetryableMoatCluster.class);

        final ExternalConfig config0 = new ExternalConfig();
        // Concurrent
        config0.setMaxConcurrentLimit(200);

        // Rate
        config0.setLimitForPeriod(100);
        config0.setLimitRefreshPeriod(Duration.ofSeconds(3L));

        // Circuit Breaker
        config0.setFailureRateThreshold(60.0f);
        config0.setIgnoreExceptions(cast(new Class[]{RuntimeException.class}));
        config0.setRingBufferSizeInHalfOpenState(11);
        config0.setRingBufferSizeInClosedState(101);
        config0.setWaitDurationInOpenState(Duration.ofSeconds(61L));

        // Retry
        config0.setMaxDelay(300L);
        config0.setMaxAttempts(5);
        config0.setIncludeExceptions(cast(new Class[]{IllegalArgumentException.class}));
        config0.setExcludeExceptions(cast(new Class[]{RuntimeException.class}));
        config0.setDelay(100L);
        config0.setMultiplier(2.0d);

        handler.update(singletonMap(resourceId, config0));

        CircuitBreakerMoat moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat0.config().getIgnoreExceptions()).isEqualTo(cast(new Class[]{RuntimeException.class}));
        then(moat0.config().getRingBufferSizeInClosedState()).isEqualTo(101);
        then(moat0.config().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(moat0.config().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(61L));
        then(moat0.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat0.getCircuitBreaker().config().getIgnoreExceptions())
                .isEqualTo(cast(new Class[]{RuntimeException.class}));
        then(moat0.getCircuitBreaker().config().getRingBufferSizeInClosedState()).isEqualTo(101);
        then(moat0.getCircuitBreaker().config().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(moat0.getCircuitBreaker().config().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(61L));

        ConcurrentLimitMoat moat1 = (ConcurrentLimitMoat) getByType(cluster0, CONCURRENT_LIMIT);
        assert moat1 != null;
        then(moat1.config().getThreshold()).isEqualTo(200);
        then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(200);

        RateLimitMoat moat2 = (RateLimitMoat) getByType(cluster0, RATE_LIMIT);
        assert moat2 != null;
        then(moat2.config().getLimitForPeriod()).isEqualTo(100);
        then(moat2.config().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(3L));

        RetryOperations operations = cluster0.retryExecutor().getOperations();
        then(operations.getConfig().getMaxAttempts()).isEqualTo(5);
        then(operations.getConfig().getIncludeExceptions())
                .isEqualTo(cast(new Class[]{IllegalArgumentException.class}));
        then(operations.getConfig().getExcludeExceptions())
                .isEqualTo(cast(new Class[]{RuntimeException.class}));
        then(operations.getConfig().getBackoffConfig().getDelay()).isEqualTo(100);
        then(operations.getConfig().getBackoffConfig().getMaxDelay()).isEqualTo(300);
        then(operations.getConfig().getBackoffConfig().getMultiplier()).isEqualTo(2.0d);

        // Forced open
        final ExternalConfig config1 = newAs(config0);
        config1.setForcedOpen(true);
        handler.update(singletonMap(resourceId, config1));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

        // forced open is null
        final ExternalConfig config2 = newAs(config1);
        config2.setForcedOpen(null);
        handler.update(singletonMap(resourceId, config2));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        final ExternalConfig config3 = newAs(config2);
        config3.setForcedOpen(true);
        handler.update(singletonMap(resourceId, config3));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(FORCED_OPEN);

        // forced open is false
        final ExternalConfig config4 = newAs(config3);
        config4.setForcedOpen(false);
        handler.update(singletonMap(resourceId, config4));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        // Forced disable
        final ExternalConfig config5 = newAs(config4);
        config5.setForcedDisabled(true);
        handler.update(singletonMap(resourceId, config5));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);

        /// forced disabled is null
        final ExternalConfig config6 = newAs(config5);
        config6.setForcedDisabled(null);
        handler.update(singletonMap(resourceId, config6));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        final ExternalConfig config7 = newAs(config6);
        config7.setForcedDisabled(true);
        handler.update(singletonMap(resourceId, config7));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(FORCED_DISABLED);

        // forced disabled is false
        final ExternalConfig config8 = newAs(config7);
        config8.setForcedDisabled(false);
        handler.update(singletonMap(resourceId, config8));
        moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.getCircuitBreaker().getState()).isEqualTo(CLOSED);

        handler.update(emptyMap());
        then(cluster0.getAll().size()).isEqualTo(3);
        then(cache.configs()).isEmpty();
    }

    @Test
    void testUpdateMatchAllConfig() {
        final ResourceId resourceId = ResourceId.from("testUpdateMatchAllConfig");
        final ArgResourceId argId = new ArgResourceId(resourceId, "arg0", "value1");

        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(Integer.MAX_VALUE);
        config.setFailureRateThreshold(50.0f);
        config.setMaxConcurrentLimit(Integer.MAX_VALUE);

        final MoatCluster cluster0 = factory.getOrCreateOfArg(argId, () -> null, () -> null, () -> config);
        final MoatCluster cluster1 = factory.getOrCreateOfArg(new ArgResourceId(resourceId,
                "arg0", "value2"), () -> null, () -> null, () -> config);

        then(cluster0.getAll().size()).isEqualTo(3);
        then(cluster1.getAll().size()).isEqualTo(3);

        final ExternalConfig config0 = new ExternalConfig();
        // Concurrent
        config0.setMaxConcurrentLimit(200);

        // Rate
        config0.setLimitForPeriod(100);
        config0.setLimitRefreshPeriod(Duration.ofSeconds(3L));

        // Circuit Breaker
        config0.setFailureRateThreshold(60.0f);
        config0.setIgnoreExceptions(cast(new Class[]{RuntimeException.class}));
        config0.setRingBufferSizeInHalfOpenState(11);
        config0.setRingBufferSizeInClosedState(101);
        config0.setWaitDurationInOpenState(Duration.ofSeconds(61L));

        // Retry
        config0.setMaxDelay(300L);
        config0.setMaxAttempts(5);
        config0.setIncludeExceptions(cast(new Class[]{IllegalArgumentException.class}));
        config0.setExcludeExceptions(cast(new Class[]{RuntimeException.class}));
        config0.setDelay(100L);
        config0.setMultiplier(2.0d);

        final ResourceId argId1 = new ArgResourceId(resourceId, "arg0", "*");

        handler.update(singletonMap(argId1, config0));

        CircuitBreakerMoat moat0 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat0 != null;
        then(moat0.config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat0.config().getIgnoreExceptions()).isEqualTo(cast(new Class[]{RuntimeException.class}));
        then(moat0.config().getRingBufferSizeInClosedState()).isEqualTo(101);
        then(moat0.config().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(moat0.config().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(61L));
        then(moat0.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat0.getCircuitBreaker().config().getIgnoreExceptions())
                .isEqualTo(cast(new Class[]{RuntimeException.class}));
        then(moat0.getCircuitBreaker().config().getRingBufferSizeInClosedState()).isEqualTo(101);
        then(moat0.getCircuitBreaker().config().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(moat0.getCircuitBreaker().config().getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(61L));

        ConcurrentLimitMoat moat1 = (ConcurrentLimitMoat) getByType(cluster0, CONCURRENT_LIMIT);
        assert moat1 != null;
        then(moat1.config().getThreshold()).isEqualTo(200);
        then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(200);

        RateLimitMoat moat2 = (RateLimitMoat) getByType(cluster0, RATE_LIMIT);
        assert moat2 != null;
        then(moat2.config().getLimitForPeriod()).isEqualTo(100);
        then(moat2.config().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(3L));

        final Map<ResourceId, ExternalConfig> configs = new HashMap<>(2);
        configs.putIfAbsent(new ArgResourceId(resourceId,
                "arg0", "value2"), config);
        handler.update(configs);
        then(factory.getOrCreateOfArg(argId, () -> null, () -> null, () -> null)).isNull();

        then(cluster1.getAll().size()).isEqualTo(3);
    }

    @Test
    void testAddConfig() {
        final ResourceId resourceId = ResourceId.from("testAddConfig");
        then(cluster.get(resourceId)).isNull();
        final RetryableMoatCluster cluster0 = factory.getOrCreateOfMethod(resourceId, () -> null,
                () -> ServiceKeeperConfig.builder()
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build(), () -> null);
        then(cluster0.getAll().size()).isEqualTo(1);
        then(cluster0).isInstanceOf(RetryableMoatCluster.class);

        final ExternalConfig config = new ExternalConfig();
        config.setForcedOpen(true);
        config.setMaxAttempts(5);
        config.setLimitForPeriod(100);

        handler.update(singletonMap(resourceId, config));
        then(cluster0.getAll().size()).isEqualTo(3);
        then(cluster0.retryExecutor().getOperations()
                .getConfig().getMaxAttempts()).isEqualTo(5);
        then(cache.configs().size()).isEqualTo(1);
    }

    @Test
    void testRemoveConfig() {
        final ResourceId resourceId = ResourceId.from("testRemoveConfig");
        then(cluster.get(resourceId)).isNull();

        final ExternalConfig config = new ExternalConfig();
        config.setForcedOpen(true);
        config.setMaxAttempts(5);
        config.setLimitForPeriod(100);
        cache.updateConfigs(singletonMap(resourceId, config));

        final RetryableMoatCluster cluster0 = factory.getOrCreateOfMethod(resourceId, () -> null,
                () -> ServiceKeeperConfig.builder()
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build(), () -> config);
        then(cluster0.getAll().size()).isEqualTo(3);
        then(cluster0.retryExecutor().getOperations()
                .getConfig().getMaxAttempts()).isEqualTo(5);

        handler.update(emptyMap());
        then(cluster0.getAll().size()).isEqualTo(1);
        then(cluster0).isInstanceOf(RetryableMoatCluster.class);
    }

    @Test
    void testUpdateGroupConfig() {
        final GroupResourceId groupId = GroupResourceId.from("testUpdateGroupConfig");

        final ResourceId id0 = ResourceId.from("testUpdateGroupConfig0");
        final ResourceId id1 = ResourceId.from("testUpdateGroupConfig1");
        final ResourceId id2 = ResourceId.from("testUpdateGroupConfig2");

        Supplier<CompositeServiceKeeperConfig> compositeConfig = () ->
                CompositeServiceKeeperConfig.builder().group(groupId).build();
        config.getOrCompute(id0, compositeConfig);
        config.getOrCompute(id1, compositeConfig);
        config.getOrCompute(id2, compositeConfig);

        Supplier<ServiceKeeperConfig> immutable =
                () -> ServiceKeeperConfig.builder()
                        .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .retryConfig(RetryConfig.ofDefault()).build();
        factory.getOrCreateOfMethod(id0, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id1, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id2, () -> null, immutable, () -> null);

        then(cluster.get(id0).getAll().size()).isEqualTo(3);
        then(cluster.get(id1).getAll().size()).isEqualTo(3);
        then(cluster.get(id2).getAll().size()).isEqualTo(3);

        final ExternalGroupConfig groupConfig = new ExternalGroupConfig();
        groupConfig.setMaxAttempts(5);
        groupConfig.setMaxConcurrentLimit(100);
        groupConfig.setLimitForPeriod(200);
        groupConfig.setFailureRateThreshold(60.0f);

        handler.update(singletonMap(groupId, groupConfig));

        // id0
        final MoatCluster cluster0 = cluster.get(id0);
        ConcurrentLimitMoat moat1 = (ConcurrentLimitMoat) getByType(cluster0, CONCURRENT_LIMIT);
        assert moat1 != null;
        then(moat1.config().getThreshold()).isEqualTo(100);
        then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(100);

        RateLimitMoat moat2 = (RateLimitMoat) getByType(cluster0, RATE_LIMIT);
        assert moat2 != null;
        then(moat2.config().getLimitForPeriod()).isEqualTo(200);
        then(moat2.rateLimiter().config().getLimitForPeriod()).isEqualTo(200);

        CircuitBreakerMoat moat3 = (CircuitBreakerMoat) getByType(cluster0, CIRCUIT_BREAKER);
        assert moat3 != null;
        then(moat3.config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat3.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(60.0f);

        RetryOperations operations = ((RetryableMoatCluster) cluster0).retryExecutor().getOperations();
        then(operations.getConfig().getMaxAttempts()).isEqualTo(5);


        // id1
        final MoatCluster cluster1 = cluster.get(id1);
        moat1 = (ConcurrentLimitMoat) getByType(cluster1, CONCURRENT_LIMIT);
        assert moat1 != null;
        then(moat1.config().getThreshold()).isEqualTo(100);
        then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(100);

        moat2 = (RateLimitMoat) getByType(cluster1, RATE_LIMIT);
        assert moat2 != null;
        then(moat2.config().getLimitForPeriod()).isEqualTo(200);
        then(moat2.rateLimiter().config().getLimitForPeriod()).isEqualTo(200);

        moat3 = (CircuitBreakerMoat) getByType(cluster1, CIRCUIT_BREAKER);
        assert moat3 != null;
        then(moat3.config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat3.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(60.0f);

        operations = ((RetryableMoatCluster) cluster1).retryExecutor().getOperations();
        then(operations.getConfig().getMaxAttempts()).isEqualTo(5);


        // id1
        final MoatCluster cluster2 = cluster.get(id0);
        moat1 = (ConcurrentLimitMoat) getByType(cluster2, CONCURRENT_LIMIT);
        assert moat1 != null;
        then(moat1.config().getThreshold()).isEqualTo(100);
        then(moat1.getConcurrentLimiter().config().getThreshold()).isEqualTo(100);

        moat2 = (RateLimitMoat) getByType(cluster2, RATE_LIMIT);
        assert moat2 != null;
        then(moat2.config().getLimitForPeriod()).isEqualTo(200);
        then(moat2.rateLimiter().config().getLimitForPeriod()).isEqualTo(200);

        moat3 = (CircuitBreakerMoat) getByType(cluster2, CIRCUIT_BREAKER);
        assert moat3 != null;
        then(moat3.config().getFailureRateThreshold()).isEqualTo(60.0f);
        then(moat3.getCircuitBreaker().config().getFailureRateThreshold()).isEqualTo(60.0f);

        operations = ((RetryableMoatCluster) cluster2).retryExecutor().getOperations();
        then(operations.getConfig().getMaxAttempts()).isEqualTo(5);
    }

    @Test
    void testAddGroupConfig() {
        final GroupResourceId groupId = GroupResourceId.from("testAddGroupConfig");

        final ResourceId id0 = ResourceId.from("testAddGroupConfig0");
        final ResourceId id1 = ResourceId.from("testAddGroupConfig1");
        final ResourceId id2 = ResourceId.from("testAddGroupConfig2");

        Supplier<CompositeServiceKeeperConfig> compositeConfig = () ->
                CompositeServiceKeeperConfig.builder().group(groupId).build();
        config.getOrCompute(id0, compositeConfig);
        config.getOrCompute(id1, compositeConfig);
        config.getOrCompute(id2, compositeConfig);

        then(cluster.get(id0)).isNull();
        then(cluster.get(id1)).isNull();
        then(cluster.get(id2)).isNull();

        final ExternalGroupConfig groupConfig = new ExternalGroupConfig();
        groupConfig.setMaxAttempts(5);
        groupConfig.setMaxConcurrentLimit(100);
        groupConfig.setLimitForPeriod(200);
        groupConfig.setFailureRateThreshold(60.0f);

        handler.update(singletonMap(groupId, groupConfig));
        then(cluster.get(id0)).isNull();
        then(cluster.get(id1)).isNull();
        then(cluster.get(id2)).isNull();

        Supplier<ServiceKeeperConfig> immutable =
                () -> ServiceKeeperConfig.builder().concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();
        factory.getOrCreateOfMethod(id0, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id1, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id2, () -> null, immutable, () -> null);

        then(cluster.get(id0).getAll().size()).isEqualTo(1);
        then(cluster.get(id1).getAll().size()).isEqualTo(1);
        then(cluster.get(id2).getAll().size()).isEqualTo(1);

        final ExternalGroupConfig groupConfig1 = newAs(groupConfig);
        groupConfig1.setFailureRateThreshold(70.0f);
        handler.update(singletonMap(groupId, groupConfig1));
        then(cluster.get(id0).getAll().size()).isEqualTo(3);
        then(cluster.get(id1).getAll().size()).isEqualTo(3);
        then(cluster.get(id2).getAll().size()).isEqualTo(3);
        then(cluster.getAll().size()).isEqualTo(3);
    }

    @Test
    void testRemoveGroupConfig() {
        final GroupResourceId groupId = GroupResourceId.from("testRemoveGroupConfig");

        final ResourceId id0 = ResourceId.from("testRemoveGroupConfig0");
        final ResourceId id1 = ResourceId.from("testRemoveGroupConfig1");
        final ResourceId id2 = ResourceId.from("testRemoveGroupConfig2");

        Supplier<CompositeServiceKeeperConfig> compositeConfig = () ->
                CompositeServiceKeeperConfig.builder().group(groupId).build();
        config.getOrCompute(id0, compositeConfig);
        config.getOrCompute(id1, compositeConfig);
        config.getOrCompute(id2, compositeConfig);

        Supplier<ServiceKeeperConfig> immutable =
                () -> ServiceKeeperConfig.builder().concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();
        factory.getOrCreateOfMethod(id0, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id1, () -> null, immutable, () -> null);
        factory.getOrCreateOfMethod(id2, () -> null, immutable, () -> null);

        then(cluster.get(id0).getAll().size()).isEqualTo(1);
        then(cluster.get(id1).getAll().size()).isEqualTo(1);
        then(cluster.get(id2).getAll().size()).isEqualTo(1);

        final ExternalGroupConfig groupConfig = new ExternalGroupConfig();
        groupConfig.setMaxAttempts(5);
        groupConfig.setMaxConcurrentLimit(100);
        groupConfig.setLimitForPeriod(200);
        groupConfig.setFailureRateThreshold(60.0f);

        handler.update(singletonMap(groupId, groupConfig));
        then(cluster.get(id0).getAll().size()).isEqualTo(3);
        then(cluster.get(id1).getAll().size()).isEqualTo(3);
        then(cluster.get(id2).getAll().size()).isEqualTo(3);

        handler.update(emptyMap());
        then(cluster.get(id0).getAll().size()).isEqualTo(1);
        then(cluster.get(id1).getAll().size()).isEqualTo(1);
        then(cluster.get(id2).getAll().size()).isEqualTo(1);
    }

    @Test
    void testComposite() {
        final Map<ResourceId, ExternalConfig> configs0 = new HashMap<>(1);
        final ExternalConfig config = new ExternalConfig();
        config.setMaxAttempts(5);

        configs0.put(ResourceId.from("testComposite0"), config);
        handler.update(configs0);

        final Map<ResourceId, ExternalConfig> configs1 = new HashMap<>(2);
        final ExternalConfig config1 = new ExternalConfig();
        config1.setMaxAttempts(3);
        final ExternalConfig config2 = new ExternalConfig();
        config2.setMaxAttempts(4);
        configs1.put(ResourceId.from("testComposite1"), config1);
        configs1.put(ResourceId.from("testComposite2"), config2);

        handler.update(configs1);
        then(cache.configs().size()).isEqualTo(2);
        then(cache.configOf(ResourceId.from("testComposite0"))).isNull();
        then(cache.configOf(ResourceId.from("testComposite1"))).isEqualTo(config1);
        then(cache.configOf(ResourceId.from("testComposite2"))).isEqualTo(config2);
        handler.update(emptyMap());
    }

    @Test
    void testParallel() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            if (i % 2 == 0) {
                final int index = i;
                new Thread(() -> {
                    try {
                        handler.update(singletonMap(ResourceId.from("testParallel" + index), new ExternalConfig()));
                    } catch (Throwable th) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        handler.update(emptyMap());
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
