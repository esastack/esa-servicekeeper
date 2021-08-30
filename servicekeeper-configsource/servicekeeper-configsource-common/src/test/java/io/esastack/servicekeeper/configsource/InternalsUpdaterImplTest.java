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

import io.esastack.servicekeeper.configsource.utils.RandomUtils;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.ExternalGroupConfig;
import io.esastack.servicekeeper.core.configsource.GroupConfigSource;
import io.esastack.servicekeeper.core.factory.FallbackHandlerFactoryImpl;
import io.esastack.servicekeeper.core.factory.LimitableMoatFactoryContext;
import io.esastack.servicekeeper.core.factory.MoatClusterFactory;
import io.esastack.servicekeeper.core.factory.MoatClusterFactoryImpl;
import io.esastack.servicekeeper.core.factory.PredicateStrategyFactoryImpl;
import io.esastack.servicekeeper.core.factory.SateTransitionProcessorFactoryImpl;
import io.esastack.servicekeeper.core.internal.GlobalConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import io.esastack.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import io.esastack.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import io.esastack.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import io.esastack.servicekeeper.core.retry.RetryOperations;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;

class InternalsUpdaterImplTest {

    private final InternalMoatCluster cluster = new CacheMoatClusterImpl();
    private final ImmutableConfigs config = new ImmutableConfigsImpl();
    private final GlobalConfig globalConfig = new GlobalConfig();

    private final MoatClusterFactory factory = new MoatClusterFactoryImpl(
            LimitableMoatFactoryContext.builder()
                    .strategy(new PredicateStrategyFactoryImpl())
                    .fallbackHandler(new FallbackHandlerFactoryImpl())
                    .limite((key) -> true)
                    .processors(Collections.emptyList())
                    .cProcessors(new SateTransitionProcessorFactoryImpl())
                    .build(),
            cluster, config);

    private final InternalsUpdater updater = new InternalsUpdaterImpl(cluster,
            new InternalGroupConfigSource(), factory, globalConfig, null);

    @Test
    void testAddRateLimitMoat() {
        final ResourceId resourceId = ResourceId.from("testAddRateLimitMoat");
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig config1 = new ExternalConfig();
        config1.setLimitForPeriod(RandomUtils.randomInt(100));

        // Case 1: Original moatChain is null.
        updater.update(resourceId, config1);
        then(cluster.get(resourceId)).isNull();

        // Case 2: Original moatChain is not null.
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, config1);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);
    }

    @Test
    void testUpdateRateLimitMoat() {
        final ResourceId resourceId = ResourceId.from("testUpdateRateLimitMoat");
        then(cluster.get(resourceId)).isNull();

        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);

        RateLimitMoat rateLimitMoat = (RateLimitMoat) cluster.get(resourceId).getAll().get(0);
        then(rateLimitMoat.config().getLimitForPeriod()).isEqualTo(Integer.MAX_VALUE);

        final ExternalConfig config = new ExternalConfig();
        int limitForPeriod = RandomUtils.randomInt(200);
        config.setLimitForPeriod(limitForPeriod);
        updater.update(resourceId, config);
        then(rateLimitMoat.config().getLimitForPeriod()).isEqualTo(limitForPeriod);

        // Update with null and fallback to original config
        updater.update(resourceId, null);
        then(rateLimitMoat.config().getLimitForPeriod()).isEqualTo(Integer.MAX_VALUE);

        // Update with null and delete all
        cluster.remove(resourceId);
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setLimitForPeriod(limitForPeriod);
        factory.getOrCreate(resourceId, () -> null, () -> null, () -> externalConfig, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().get(0).config()).isEqualTo(RateLimitConfig.ofDefault());
    }

    @Test
    void testDeleteRateLimit() {
        final ResourceId resourceId = ResourceId.from("testDeleteRateLimit");
        then(cluster.get(resourceId)).isNull();

        final ExternalConfig config = new ExternalConfig();
        config.setLimitForPeriod(RandomUtils.randomInt(200));
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), () -> config, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);

        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
    }

    @Test
    void testAddConcurrentLimit() {
        final ResourceId resourceId = ResourceId.from("testAddConcurrentLimit");
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig config1 = new ExternalConfig();
        config1.setMaxConcurrentLimit(RandomUtils.randomInt(100));

        // Case 1: Original moatChain is null.
        updater.update(resourceId, config1);
        then(cluster.get(resourceId)).isNull();

        // Case 2: Original moatChain is not null.
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, config1);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);
    }

    @Test
    void testUpdateConcurrentLimit() {
        final ResourceId resourceId = ResourceId.from("testUpdateConcurrentLimit");
        then(cluster.get(resourceId)).isNull();

        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);

        ConcurrentLimitMoat concurrentLimitMoat = (ConcurrentLimitMoat) cluster.get(resourceId).getAll().get(0);
        then(concurrentLimitMoat.config().getThreshold()).isEqualTo(Integer.MAX_VALUE);

        final ExternalConfig config = new ExternalConfig();
        int maxConcurrentLimit = RandomUtils.randomInt(200);
        config.setMaxConcurrentLimit(maxConcurrentLimit);
        updater.update(resourceId, config);
        then(concurrentLimitMoat.config().getThreshold()).isEqualTo(maxConcurrentLimit);

        // Update with null and fallback to original config
        updater.update(resourceId, null);
        then(concurrentLimitMoat.config().getThreshold()).isEqualTo(Integer.MAX_VALUE);

        // Update with null and delete all
        cluster.remove(resourceId);
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(maxConcurrentLimit);
        factory.getOrCreate(resourceId, () -> null, () -> null, () -> externalConfig, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().get(0).config()).isEqualTo(ConcurrentLimitConfig.ofDefault());
    }

    @Test
    void testDeleteConcurrentLimit() {
        final ResourceId resourceId = ResourceId.from("testDeleteConcurrentLimit");
        then(cluster.get(resourceId)).isNull();

        final ExternalConfig config = new ExternalConfig();
        config.setMaxConcurrentLimit(RandomUtils.randomInt(200));
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), () -> config, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);

        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
    }

    @Test
    void testAddCircuitBreaker() {
        final ResourceId resourceId = ResourceId.from("testAddCircuitBreaker");
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig config1 = new ExternalConfig();
        config1.setFailureRateThreshold(RandomUtils.randomFloat(100));

        // Case 1: Original moatChain is null.
        updater.update(resourceId, config1);
        then(cluster.get(resourceId)).isNull();

        // Case 2: Original moatChain is not null.
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, config1);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);
    }

    @Test
    void testUpdateCircuitBreaker() {
        final ResourceId resourceId = ResourceId.from("testUpdateCircuitBreaker");
        then(cluster.get(resourceId)).isNull();

        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);

        CircuitBreakerMoat breakerMoat = (CircuitBreakerMoat) cluster.get(resourceId).getAll().get(0);
        then(breakerMoat.config().getFailureRateThreshold()).isEqualTo(50.0f);

        final ExternalConfig config = new ExternalConfig();
        float failureRateThreshold = RandomUtils.randomFloat(200);
        config.setFailureRateThreshold(failureRateThreshold);
        updater.update(resourceId, config);
        then(breakerMoat.config().getFailureRateThreshold()).isEqualTo(failureRateThreshold);

        // Update with null and fallback to original config
        updater.update(resourceId, null);
        then(breakerMoat.config().getFailureRateThreshold()).isEqualTo(50.0f);

        // Update with null and delete all
        cluster.remove(resourceId);
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setFailureRateThreshold(failureRateThreshold);
        factory.getOrCreate(resourceId, () -> null, () -> null, () -> externalConfig, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().get(0).config()).isEqualTo(CircuitBreakerConfig.ofDefault());
    }

    @Test
    void testDeleteCircuitBreaker() {
        final ResourceId resourceId = ResourceId.from("testDeleteCircuitBreaker");
        then(cluster.get(resourceId)).isNull();

        final ExternalConfig config = new ExternalConfig();
        config.setFailureRateThreshold(RandomUtils.randomFloat(200));
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build(), () -> config, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(2);

        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
    }

    @Test
    void testAddRetry() {
        final ResourceId resourceId = ResourceId.from("testAddRetry");
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig config1 = new ExternalConfig();
        int maxAttempts = RandomUtils.randomInt(100);
        config1.setMaxAttempts(maxAttempts);

        // Case 1: Original moatChain is null.
        updater.update(resourceId, config1);
        then(cluster.get(resourceId)).isNull();

        // Case 2: Original moatChain is not null.
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        updater.update(resourceId, config1);
        then(((RetryableMoatCluster) cluster.get(resourceId)).retryExecutor().getOperations().getConfig()
                .getMaxAttempts()).isEqualTo(maxAttempts);
    }

    @Test
    void testUpdateRetry() {
        final ResourceId resourceId = ResourceId.from("testUpdateRetry");
        then(cluster.get(resourceId)).isNull();

        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .retryConfig(RetryConfig.ofDefault()).build(), null, false);
        then(cluster.get(resourceId)).isNotNull();
        then(cluster.get(resourceId).getAll().size()).isEqualTo(0);

        RetryOperations retryOperations = ((RetryableMoatCluster) cluster.get(resourceId))
                .retryExecutor().getOperations();
        then(retryOperations.getConfig().getMaxAttempts()).isEqualTo(3);

        final ExternalConfig config = new ExternalConfig();
        int maxAttempts = RandomUtils.randomInt(200);
        config.setMaxAttempts(maxAttempts);
        updater.update(resourceId, config);
        then(retryOperations.getConfig().getMaxAttempts()).isEqualTo(maxAttempts);

        // Update with null and fallback to original config
        updater.update(resourceId, null);
        then(retryOperations.getConfig().getMaxAttempts()).isEqualTo(3);

        // Update with null and delete all
        cluster.remove(resourceId);
        then(cluster.get(resourceId)).isNull();
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setMaxAttempts(RandomUtils.randomInt(100));
        factory.getOrCreate(resourceId, () -> null, () -> null, () -> externalConfig, false);
        then(cluster.get(resourceId)).isNotNull();
        updater.update(resourceId, null);
        then(cluster.get(resourceId)).isNull();
    }

    @Test
    void testDeleteRetry() {
        final ResourceId resourceId = ResourceId.from("testDeleteRetry");
        then(cluster.get(resourceId)).isNull();

        final ExternalConfig config = new ExternalConfig();
        config.setMaxAttempts(RandomUtils.randomInt(200));
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .rateLimiterConfig(RateLimitConfig.ofDefault()).build(), () -> config, false);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        then(((RetryableMoatCluster) cluster.get(resourceId)).retryExecutor()).isNotNull();

        updater.update(resourceId, null);
        then(cluster.get(resourceId).getAll().size()).isEqualTo(1);
        then(((RetryableMoatCluster) cluster.get(resourceId)).retryExecutor()).isNull();
    }

    @Test
    void testUpdateMaxSpendTimeMs() {
        // PredicateByMaxSpendTimeMs
        final ResourceId resourceId = ResourceId.from("testUpdateMaxSpendTimeMs0");
        then(cluster.get(resourceId)).isNull();

        final long originalMaxSpendTimeMs = RandomUtils.randomLong();
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setPredicateStrategy(PredicateBySpendTime.class);
        externalConfig.setMaxSpendTimeMs(originalMaxSpendTimeMs);
        factory.getOrCreate(resourceId, () -> null, () -> ServiceKeeperConfig.builder()
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault()).build(), () -> externalConfig, false);
        CircuitBreakerMoat circuitBreakerMoat = ((CircuitBreakerMoat) cluster.get(resourceId).getAll().get(0));
        then(((PredicateBySpendTime) circuitBreakerMoat.getPredicate()).getMaxSpendTimeMs())
                .isEqualTo(originalMaxSpendTimeMs);
        final ExternalConfig config = new ExternalConfig();
        final long maxSpendTimeMs = RandomUtils.randomLong();
        config.setMaxSpendTimeMs(maxSpendTimeMs);
        updater.update(resourceId, config);
        then(((PredicateBySpendTime) circuitBreakerMoat.getPredicate()).getMaxSpendTimeMs())
                .isEqualTo(maxSpendTimeMs);

        updater.update(resourceId, null);
        then(((PredicateBySpendTime) circuitBreakerMoat.getPredicate()).getMaxSpendTimeMs())
                .isEqualTo(originalMaxSpendTimeMs);
    }

    @Test
    void testUpdateGroupConfig() {
        final GroupResourceId groupId = GroupResourceId.from("testUpdateGroupConfig1");
        final ExternalConfig externalConfig = new ExternalConfig();
        externalConfig.setMaxConcurrentLimit(RandomUtils.randomInt(300));
        factory.getOrCreate(ResourceId.from("abc"), () -> null, () -> null, () -> externalConfig, false);
        factory.getOrCreate(ResourceId.from("def"), () -> null, () -> null, () -> externalConfig, false);
        factory.getOrCreate(ResourceId.from("xyz"), () -> null, () -> null, () -> externalConfig, false);
        then(cluster.get(ResourceId.from("abc")).getAll().size()).isEqualTo(1);
        then(cluster.get(ResourceId.from("def")).getAll().size()).isEqualTo(1);
        then(cluster.get(ResourceId.from("xyz")).getAll().size()).isEqualTo(1);

        final ExternalConfig config = new ExternalConfig();
        config.setMaxAttempts(RandomUtils.randomInt(5));
        config.setLimitForPeriod(RandomUtils.randomInt(300));
        config.setFailureRateThreshold(RandomUtils.randomFloat(100));
        config.setMaxConcurrentLimit(RandomUtils.randomInt(500));

        updater.update(groupId, config);
        then(cluster.get(ResourceId.from("abc")).getAll().size()).isEqualTo(3);
        then(RetryableMoatCluster.isInstance(cluster.get(ResourceId.from("abc")))).isTrue();
        then(cluster.get(ResourceId.from("def")).getAll().size()).isEqualTo(3);
        then(RetryableMoatCluster.isInstance(cluster.get(ResourceId.from("def")))).isTrue();
        then(cluster.get(ResourceId.from("xyz")).getAll().size()).isEqualTo(3);
        then(RetryableMoatCluster.isInstance(cluster.get(ResourceId.from("xyz")))).isTrue();

        updater.update(groupId, null);
        then(cluster.get(ResourceId.from("abc"))).isNull();
        then(cluster.get(ResourceId.from("def"))).isNull();
        then(cluster.get(ResourceId.from("xyz"))).isNull();
    }

    private static class InternalGroupConfigSource implements GroupConfigSource {

        private final Map<GroupResourceId, ExternalConfig> configMap = new HashMap<>(2);
        private final Set<ResourceId> groupItems = new HashSet<>(3);

        private InternalGroupConfigSource() {
            ExternalGroupConfig config1 = new ExternalGroupConfig();
            config1.setMaxConcurrentLimit(RandomUtils.randomInt(200));
            groupItems.add(ResourceId.from("abc"));
            groupItems.add(ResourceId.from("def"));
            groupItems.add(ResourceId.from("xyz"));
            config1.setItems(groupItems);

            configMap.putIfAbsent(GroupResourceId.from("testUpdateGroupConfig1"), config1);
            ExternalGroupConfig config2 = new ExternalGroupConfig();
            configMap.putIfAbsent(GroupResourceId.from("testUpdateGroupConfig2"), config2);
        }

        @Override
        public Map<GroupResourceId, ExternalConfig> allGroups() {
            return configMap;
        }

        @Override
        public ExternalConfig config(GroupResourceId groupId) {
            return configMap.get(groupId);
        }

        @Override
        public GroupResourceId mappingGroupId(ResourceId methodId) {
            if (groupItems.contains(methodId)) {
                return GroupResourceId.from("testUpdateGroupConfig1");
            }
            return GroupResourceId.from("testUpdateGroupConfig2");
        }

        @Override
        public Set<ResourceId> mappingResourceIds(GroupResourceId groupId) {
            return ((ExternalGroupConfig) configMap.get(groupId)).getItems();
        }
    }
}
