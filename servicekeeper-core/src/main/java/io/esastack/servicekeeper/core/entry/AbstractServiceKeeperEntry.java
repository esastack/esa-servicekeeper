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
package io.esastack.servicekeeper.core.entry;

import esa.commons.Checks;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.OriginalInvocation;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.executionchain.AbstractExecutionChain;
import io.esastack.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.executionchain.ExecutionChain;
import io.esastack.servicekeeper.core.executionchain.RetryableExecutionChain;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChainImpl;
import io.esastack.servicekeeper.core.factory.MoatClusterFactory;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.internal.GlobalConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.moats.FallbackMoatCluster;
import io.esastack.servicekeeper.core.moats.Moat;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.retry.RetryableExecutor;
import io.esastack.servicekeeper.core.utils.GenericTypeUtils;
import io.esastack.servicekeeper.core.utils.LogUtils;
import io.esastack.servicekeeper.core.utils.MethodUtils;
import io.esastack.servicekeeper.core.utils.ParameterUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;

abstract class AbstractServiceKeeperEntry implements ServiceKeeperEntry {

    private static final Logger logger = LogUtils.logger();

    private final GlobalConfig globalConfig;
    private final MoatClusterFactory factory;

    AbstractServiceKeeperEntry(MoatClusterFactory factory, GlobalConfig globalConfig) {
        Checks.checkNotNull(globalConfig, "globalConfig");
        Checks.checkNotNull(factory, "factory");
        this.globalConfig = globalConfig;
        this.factory = factory;
    }

    @Override
    public Object invoke(Method method, Object delegate, Object... args) throws Throwable {
        return invoke(MethodUtils.getMethodAlias(method), method, delegate, args);
    }

    @Override
    public <T> T call(String resourceId, Callable<T> callable, Object... args) throws Throwable {
        return call(resourceId, null, callable, args);
    }

    @Override
    public <T> T call(String resourceId, CompositeServiceKeeperConfig immutableConfig,
                      Callable<T> callable, Object... args) throws Throwable {
        final Supplier<OriginalInvocation> originalInvocation =
                getOriginalInvocation(callable);

        return call(resourceId, () -> immutableConfig, originalInvocation, callable, args);
    }

    @Override
    public <T> T call(String resourceId, CompositeServiceKeeperConfig immutableConfig,
                      OriginalInvocation originalInvocation, Callable<T> callable,
                      Object... args) throws Throwable {
        return call(resourceId, () -> immutableConfig, () -> originalInvocation, callable, args);
    }

    @Override
    public void run(String resourceId, Runnable runnable, Object... args) throws Throwable {
        run(resourceId, null, runnable, args);
    }

    /**
     * Build composite chain of current invocation(include method's and args').
     * <p>
     * When accessing original resource, a {@link ExecutionChain} corresponding with the invocation will be built, and
     * you can get the permission of current accesses with {@link ExecutionChain#tryToExecute(Context)}. The chain is
     * built with method's {@link MoatCluster} and args' {@link MoatCluster}.
     *
     * @param name            the name of current method
     * @param invocation      the Supplier to supply OriginalInvocation
     * @param immutableConfig to supply config or name
     * @param isAsync         async or not
     * @param args            args
     * @return execution chain
     */
    final AbstractExecutionChain buildExecutionChain(String name,
                                                     Supplier<OriginalInvocation> invocation,
                                                     Supplier<CompositeServiceKeeperConfig> immutableConfig,
                                                     boolean isAsync,
                                                     Object... args) {
        // When the global service keeper is disabled, just return null.
        if (globalConfig.globalDisable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("ServiceKeeper has been disabled, so current call: {} will through without blocking",
                        name);
            }
            return null;
        }

        final ResourceId resourceId = ResourceId.from(name);
        final CompositeServiceKeeperConfig immutableConfig0 = getOrComputeConfig(resourceId, immutableConfig);

        // Get method chain
        final FallbackMoatCluster moatClusterOfMethod = (FallbackMoatCluster) factory.getOrCreate(resourceId,
                invocation,
                () -> (immutableConfig0 == null ? null : immutableConfig0.getMethodConfig()),
                () -> getExternalConfig(resourceId),
                isAsync);

        if (!globalConfig.argLevelEnable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("ServiceKeeper args' governance has been disabled, so the args'" +
                        " checking will be ignored");
            }
            return buildExecutionChain(moatClusterOfMethod, null, isAsync, name);
        }

        if (args == null || args.length == 0) {
            return buildExecutionChain(moatClusterOfMethod, null, isAsync, name);
        }

        return buildExecutionChain(moatClusterOfMethod,
                getMoatClustersOfArgs(resourceId, invocation, immutableConfig0, isAsync, args),
                isAsync, name);
    }

    private List<MoatCluster> getMoatClustersOfArgs(ResourceId resourceId,
                                                    Supplier<OriginalInvocation> invocation,
                                                    CompositeServiceKeeperConfig immutableConfig,
                                                    boolean isAsync,
                                                    Object... args
    ) {
        final CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = immutableConfig == null
                ? null : immutableConfig.getArgConfig();
        final Map<Integer, CompositeServiceKeeperConfig.CompositeArgConfig> argConfigMap = argsConfig == null
                ? null : argsConfig.getArgConfigMap();

        String argName;
        List<MoatCluster> moatClustersOfArgs = new ArrayList<>(3);
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                continue;
            }

            CompositeServiceKeeperConfig.CompositeArgConfig argConfig =
                    argConfigMap == null ? null : argConfigMap.get(i);
            argName = argConfig == null ? ParameterUtils.defaultName(i) : argConfig.getArgName();

            final ArgResourceId argId = new ArgResourceId(resourceId, argName, args[i]);

            // If the current arg value is not configured in immutable config or external config, that means
            // the value is not considered as a governed value, just continue the next arg.
            final int index = i;
            final MoatCluster argMoatCluster = factory.getOrCreate(argId,
                    invocation,
                    () -> getImmutableConfig(resourceId, args[index], argConfig),
                    () -> getExternalConfig(argId), isAsync);
            if (argMoatCluster != null) {
                moatClustersOfArgs.add(argMoatCluster);
            }
        }
        return moatClustersOfArgs;
    }

    /**
     * Get supplier of originalInvocationWrapper from runnable
     *
     * @param callable callable
     * @return Supplier
     */
    protected Supplier<OriginalInvocation> getOriginalInvocation(final Callable<?> callable) {
        return () -> new OriginalInvocation(
                GenericTypeUtils.getSuperClassGenericType(callable.getClass()), new Class[0]);
    }

    /**
     * Get external config of specified {@link ResourceId} and group.
     *
     * @param resourceId resourceId
     * @return external config
     */
    protected abstract ExternalConfig getExternalConfig(ResourceId resourceId);

    /**
     * Get immutable composite configuration, and add the governed values and maxSizeLimits to
     * {@link ImmutableConfigs} when compute the config successfully.
     *
     * @param resourceId      resourceId
     * @param immutableConfig the supplier to get immutable config
     * @return composite config
     */
    protected abstract CompositeServiceKeeperConfig getOrComputeConfig(ResourceId resourceId,
                                                                       Supplier<CompositeServiceKeeperConfig>
                                                                               immutableConfig);

    /**
     * Get arg value's {@link ServiceKeeperConfig}.
     *
     * @param argValue  argValue
     * @param argConfig argConfig
     * @return config
     */
    ServiceKeeperConfig getImmutableConfig(ResourceId methodId, Object argValue,
                                           CompositeServiceKeeperConfig.CompositeArgConfig argConfig) {
        if (argConfig == null) {
            return null;
        }
        Map<Object, ServiceKeeperConfig> valueMap = argConfig.getValueToConfig();
        if (valueMap == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}.{} immutable config from template: {}", methodId.getName(),
                        argConfig.getArgName(), argConfig.getTemplate());
            }
            return argConfig.getTemplate();
        }

        ServiceKeeperConfig configOfCurrentValue = valueMap.get(argValue);
        ServiceKeeperConfig configOfMatchAll;
        if (configOfCurrentValue == null) {
            configOfMatchAll = valueMap.get(VALUE_MATCH_ALL);
            if (configOfMatchAll == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Obtained {}.{} immutable config from template: {}", methodId.getName(),
                            argConfig.getArgName(), argConfig.getTemplate());
                }
                return argConfig.getTemplate();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Obtained {}.{} immutable config from value of matches all: {}", methodId.getName(),
                            argConfig.getArgName(), configOfMatchAll);
                }
                return configOfMatchAll;
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}.{} immutable config: {}", methodId.getName(),
                        argConfig.getArgName(), argValue);
            }
            return configOfCurrentValue;
        }
    }

    private AbstractExecutionChain buildExecutionChain(FallbackMoatCluster moatClusterOfMethod,
                                                       List<MoatCluster> moatClustersOfArgs,
                                                       boolean isAsync, String name) {

        if (moatClusterOfMethod == null && moatClustersOfArgs == null) {
            return null;
        }

        final List<Moat<?>> moats = new ArrayList<>(3);
        FallbackHandler<?> fallbackHandler = null;
        RetryableExecutor executor = null;
        if (moatClusterOfMethod != null) {
            moats.addAll(moatClusterOfMethod.getAll());
            fallbackHandler = moatClusterOfMethod.fallbackHandler();
            if (RetryableMoatCluster.isInstance(moatClusterOfMethod)) {
                executor = ((RetryableMoatCluster) moatClusterOfMethod).retryExecutor();
            }
        }

        if (moatClustersOfArgs != null) {
            for (MoatCluster argMoatCluster : moatClustersOfArgs) {
                moats.addAll(argMoatCluster.getAll());
            }
        }

        if (fallbackHandler == null &&
                executor == null &&
                moats.isEmpty()) {
            return null;
        }

        // Async invocation
        if (isAsync) {
            return moats.isEmpty() ? null : new AsyncExecutionChainImpl(moats, fallbackHandler);
        }

        // Sync invocation
        if (!globalConfig.retryEnable()) {
            if (logger.isDebugEnabled()) {
                logger.debug("ServiceKeeper retry has been disabled, so current call {} will through without" +
                        " retrying", name);
            }
            return new SyncExecutionChainImpl(moats, fallbackHandler);
        }

        return executor == null ? new SyncExecutionChainImpl(moats, fallbackHandler) :
                new RetryableExecutionChain(moats, fallbackHandler, executor);
    }
}
