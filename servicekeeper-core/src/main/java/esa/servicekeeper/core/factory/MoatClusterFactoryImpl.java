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
package esa.servicekeeper.core.factory;

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.fallback.FallbackHandlerConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.FallbackMoatCluster;
import esa.servicekeeper.core.moats.FallbackMoatClusterImpl;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.MoatClusterImpl;
import esa.servicekeeper.core.moats.MoatType;
import esa.servicekeeper.core.moats.RetryableMoatCluster;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.retry.RetryOperations;
import esa.servicekeeper.core.retry.RetryableExecutor;
import esa.servicekeeper.core.utils.ConfigUtils;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapCircuitBreaker;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapConcurrent;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapRate;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasBootstrapRetry;
import static esa.servicekeeper.core.internal.ImmutableConfigs.ConfigType.CIRCUITBREAKER_CONFIG;
import static esa.servicekeeper.core.internal.ImmutableConfigs.ConfigType.CONCURRENTLIMIT_CONFIG;
import static esa.servicekeeper.core.internal.ImmutableConfigs.ConfigType.RATELIMIT_CONFIG;
import static esa.servicekeeper.core.internal.ImmutableConfigs.ConfigType.RETRY_CONFIG;
import static esa.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static esa.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static esa.servicekeeper.core.moats.MoatType.RETRY;

public class MoatClusterFactoryImpl implements MoatClusterFactory {

    private static final Logger logger = LogUtils.logger();

    private final LimitableMoatFactoryContext context;
    private final Map<MoatType, AbstractMoatFactory<?, ?>> factories;
    private final InternalMoatCluster cluster;
    private final ImmutableConfigs configs;

    public MoatClusterFactoryImpl(LimitableMoatFactoryContext ctx, InternalMoatCluster cluster,
                                  ImmutableConfigs configs) {
        Checks.checkNotNull(ctx, "ctx");
        Checks.checkNotNull(cluster, "cluster");
        Checks.checkNotNull(configs, "configs");
        this.factories = MoatFactory.factories(ctx);
        this.cluster = cluster;
        this.context = ctx;
        this.configs = configs;
    }

    @Override
    public MoatCluster getOrCreate(ResourceId resourceId,
                                   Supplier<OriginalInvocation> originalInvocation,
                                   Supplier<ServiceKeeperConfig> immutableConfig,
                                   Supplier<ExternalConfig> externalConfig,
                                   boolean isAsync) {
        final MoatCluster cluster0 = cluster.get(resourceId);
        if (cluster0 != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Got moat cluster {} successfully, resourceId: {}", cluster0, resourceId);
            }
            return cluster0;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("The moat cluster got from current map is null, try to create a new one," +
                    " resourceId: {}", resourceId);
        }

        final ServiceKeeperConfig immutableConfig0 = immutableConfig == null ? null : immutableConfig.get();
        final ExternalConfig externalConfig0 = externalConfig == null ? null : externalConfig.get();
        if (immutableConfig == null && externalConfig == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Unable to create a moat cluster, immutable config and external config are all null," +
                        " resourceId: {}", resourceId);
            }
            return null;
        }

        final OriginalInvocation invocation0 = originalInvocation == null
                ? null : originalInvocation.get();
        return cluster.computeIfAbsent(resourceId, (id) -> doCreate(resourceId,
                invocation0, immutableConfig0, externalConfig0, isAsync));
    }

    @Override
    public void update(ResourceId resourceId, MoatCluster cluster0, ExternalConfig config) {
        doUpdate(resourceId, cluster0, config);
    }

    private void doUpdate(ResourceId resourceId, MoatCluster cluster0, ExternalConfig config) {
        if (config == null || cluster0 == null) {
            return;
        }

        if (hasBootstrapCircuitBreaker(config) && !cluster0.contains(CIRCUIT_BREAKER)) {
            // New a CircuitBreaker moat.
            final CircuitBreakerConfig immutableConfig =
                    (CircuitBreakerConfig) configs.getConfig(resourceId, CIRCUITBREAKER_CONFIG);

            final CircuitBreakerMoat moat =
                    ((LimitableMoatFactory.LimitableCircuitBreakerMoatFactory) factories.get(CIRCUIT_BREAKER))
                            .doCreate(resourceId,
                                    null,
                                    ConfigUtils.combine(immutableConfig, config),
                                    immutableConfig);

            if (moat != null) {
                // Add circuitBreaker moat.
                cluster0.add(moat);
            }
        }

        if (hasBootstrapRate(config) && !cluster0.contains(RATE_LIMIT)) {
            // New a RateLimit moat.
            final RateLimitConfig limitConfig =
                    (RateLimitConfig) configs.getConfig(resourceId, RATELIMIT_CONFIG);
            final RateLimitMoat moat =
                    ((LimitableMoatFactory.LimitableRateMoatFactory) factories.get(RATE_LIMIT))
                            .doCreate0(resourceId,
                                    null,
                                    ConfigUtils.combine(limitConfig, config), limitConfig);

            if (moat != null) {
                // Add rateLimiter moat.
                cluster0.add(moat);
            }
        }
        if (hasBootstrapConcurrent(config) && !cluster0.contains(CONCURRENT_LIMIT)) {
            // New a concurrentLimit moat.
            final ConcurrentLimitConfig limitConfig =
                    (ConcurrentLimitConfig) configs.getConfig(resourceId, CONCURRENTLIMIT_CONFIG);
            final ConcurrentLimitMoat moat =
                    ((LimitableMoatFactory.LimitableConcurrentMoatFactory) factories.get(CONCURRENT_LIMIT))
                            .doCreate0(resourceId, null,
                                    ConfigUtils.combine(limitConfig, config), limitConfig);

            if (moat != null) {
                // Add concurrent limiter moat.
                cluster0.add(moat);
            }
        }

        if (RetryableMoatCluster.isInstance(cluster0)) {
            if (hasBootstrapRetry(config) && ((RetryableMoatCluster) cluster0).retryExecutor() == null) {
                final RetryConfig retryConfig = (RetryConfig) configs.getConfig(resourceId, RETRY_CONFIG);
                final RetryOperations retryOperations =
                        ((LimitableMoatFactory.RetryOperationFactory) factories.get(RETRY))
                                .doCreate(resourceId, null,
                                        ConfigUtils.combine(retryConfig, config), retryConfig);
                if (retryOperations != null) {
                    ((RetryableMoatCluster) cluster0).updateRetryExecutor(new RetryableExecutor(retryOperations));
                } else {
                    ((RetryableMoatCluster) cluster0).updateRetryExecutor(null);
                }
            }
        }
    }

    /**
     * Create a moat cluster by immutable config and external config.
     *
     * @param resourceId      resourceId
     * @param invocation      original invocation
     * @param immutableConfig immutable config
     * @param externalConfig  combineConfigSupplier
     * @return chain
     */
    private MoatCluster doCreate(final ResourceId resourceId, OriginalInvocation invocation,
                                 ServiceKeeperConfig immutableConfig, ExternalConfig externalConfig,
                                 boolean isAsync) {
        final ServiceKeeperConfig combinedConfig = ConfigUtils.combine(immutableConfig, externalConfig);

        if (combinedConfig == null) {
            return null;
        }

        List<Moat<?>> moats = createMoats(resourceId, invocation,
                combinedConfig, immutableConfig, externalConfig);

        // If it's args level resourceId, just create and return a default moat cluster.
        if (resourceId instanceof ArgResourceId) {
            return createArgMoatCluster(moats);
        } else {
            if (isAsync) {
                //Async don,t support Retry
                return createFallbackMoatCluster(invocation, moats, combinedConfig);
            } else {
                return createRetryableMoatCluster(resourceId, invocation, moats,
                        combinedConfig, immutableConfig);
            }
        }

    }

    private List<Moat<?>> createMoats(final ResourceId resourceId, OriginalInvocation invocation,
                                      ServiceKeeperConfig combinedConfig,
                                      ServiceKeeperConfig immutableConfig,
                                      ExternalConfig externalConfig) {

        if (combinedConfig.getRateLimitConfig() != null || combinedConfig.getConcurrentLimitConfig() != null
                || combinedConfig.getCircuitBreakerConfig() != null || combinedConfig.getRetryConfig() != null) {
            logger.info("Begin to create a new moat cluster, resourceId: {}, config:{};" +
                            " immutable config: {}; external config: {}",
                    resourceId.getName(), combinedConfig,
                    immutableConfig == null ? "null" : immutableConfig.toString(),
                    externalConfig == null ? "null" : externalConfig.toString());
        } else {
            return null;
        }

        List<Moat<?>> moats = new ArrayList<>(3);
        if (combinedConfig.getRateLimitConfig() != null) {
            final RateLimitMoat rateLimitMoat =
                    ((LimitableMoatFactory.LimitableRateMoatFactory) factories.get(RATE_LIMIT))
                            .doCreate(resourceId, invocation,
                                    combinedConfig.getRateLimitConfig(),
                                    immutableConfig == null ? null : immutableConfig.getRateLimitConfig());
            if (rateLimitMoat != null) {
                moats.add(rateLimitMoat);
            }
        }

        if (combinedConfig.getConcurrentLimitConfig() != null) {
            final ConcurrentLimitMoat concurrentLimitMoat =
                    ((LimitableMoatFactory.LimitableConcurrentMoatFactory) factories.get(CONCURRENT_LIMIT))
                            .doCreate(resourceId,
                                    invocation, combinedConfig.getConcurrentLimitConfig(),
                                    immutableConfig == null ? null : immutableConfig.getConcurrentLimitConfig());
            if (concurrentLimitMoat != null) {
                moats.add(concurrentLimitMoat);
            }
        }

        if (combinedConfig.getCircuitBreakerConfig() != null) {
            final CircuitBreakerMoat circuitBreakerMoat =
                    ((LimitableMoatFactory.LimitableCircuitBreakerMoatFactory) factories.get(CIRCUIT_BREAKER))
                            .doCreate(resourceId,
                                    invocation, combinedConfig.getCircuitBreakerConfig(),
                                    immutableConfig == null ? null : immutableConfig.getCircuitBreakerConfig());
            if (circuitBreakerMoat != null) {
                moats.add(circuitBreakerMoat);
            }
        }

        return moats;
    }

    private MoatCluster createArgMoatCluster(List<Moat<?>> moats) {
        if (moats == null || moats.isEmpty()) {
            return null;
        }
        return new MoatClusterImpl(moats, context.listeners());
    }

    private FallbackMoatCluster createFallbackMoatCluster(OriginalInvocation invocation,
                                                          List<Moat<?>> moats, ServiceKeeperConfig combinedConfig) {
        return new FallbackMoatClusterImpl(
                moats,
                context.listeners(),
                createFallbackHandler(invocation, combinedConfig.getFallbackConfig()));
    }

    private RetryableMoatCluster createRetryableMoatCluster(ResourceId resourceId, OriginalInvocation invocation,
                                                            List<Moat<?>> moats, ServiceKeeperConfig combinedConfig,
                                                            ServiceKeeperConfig immutableConfig) {
        return new RetryableMoatCluster(
                moats,
                context.listeners(),
                createFallbackHandler(invocation, combinedConfig.getFallbackConfig()),
                createRetryableExecutor(resourceId, invocation, combinedConfig.getRetryConfig(), immutableConfig));
    }


    private FallbackHandler<?> createFallbackHandler(OriginalInvocation invocation, FallbackConfig fallbackConfig) {
        if (fallbackConfig == null) {
            return null;
        }

        // Note: Try to fill fallbackConfig with original method
        if (invocation != null) {
            tryToFillConfig(fallbackConfig, invocation.getMethod());
        }

        return context.handler().get(new FallbackHandlerConfig(fallbackConfig, invocation));
    }

    private RetryableExecutor createRetryableExecutor(ResourceId resourceId,
                                                      OriginalInvocation invocation,
                                                      RetryConfig retryConfig,
                                                      ServiceKeeperConfig immutableConfig) {
        if (retryConfig == null) {
            return null;
        }

        final RetryOperations retryOperations = ((AbstractMoatFactory.RetryOperationFactory) factories.get(RETRY))
                .doCreate(resourceId, invocation, retryConfig,
                        immutableConfig == null ? null : immutableConfig.getRetryConfig());

        if (retryOperations != null) {
            return new RetryableExecutor(retryOperations);
        }
        return null;
    }

    /**
     * Try to update fallback config with method's name and declaringClass
     *
     * @param fallbackConfig fallbackConfig
     * @param method         method
     */
    void tryToFillConfig(final FallbackConfig fallbackConfig, final Method method) {
        if (method == null || fallbackConfig == null) {
            return;
        }
        if (StringUtils.isEmpty(fallbackConfig.getMethodName()) && fallbackConfig.getTargetClass() == null) {
            return;
        }
        if (StringUtils.isEmpty(fallbackConfig.getMethodName())) {
            fallbackConfig.setMethodName(method.getName());
        }
        if (fallbackConfig.getTargetClass() == null) {
            fallbackConfig.setTargetClass(method.getDeclaringClass());
        }
    }
}
