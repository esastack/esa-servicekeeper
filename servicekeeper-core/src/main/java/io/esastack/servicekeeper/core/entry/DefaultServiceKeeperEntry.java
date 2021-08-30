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
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.PlainConfigSource;
import io.esastack.servicekeeper.core.executionchain.SyncContext;
import io.esastack.servicekeeper.core.executionchain.SyncExecutionChain;
import io.esastack.servicekeeper.core.factory.MoatClusterFactory;
import io.esastack.servicekeeper.core.internal.GlobalConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.utils.LogUtils;
import io.esastack.servicekeeper.core.utils.MethodUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;

public class DefaultServiceKeeperEntry extends AbstractServiceKeeperEntry {

    private static final Logger logger = LogUtils.logger();

    private final PlainConfigSource configSource;
    private final ImmutableConfigs immutableConfigs;

    DefaultServiceKeeperEntry(PlainConfigSource config,
                              ImmutableConfigs immutableConfigs,
                              MoatClusterFactory factory,
                              GlobalConfig globalConfig) {
        super(factory, globalConfig);
        Checks.checkNotNull(immutableConfigs, "immutableConfigs");
        this.configSource = config;
        this.immutableConfigs = immutableConfigs;
    }

    @Override
    public Object invoke(String aliasName, Method method, Object delegate, Object... args) throws Throwable {
        final Supplier<OriginalInvocation> originalInvocation =
                getOriginalInvocation(method);
        final SyncExecutionChain executionChain = buildExecutionChain(aliasName,
                getOriginalInvocation(method),
                () -> MethodUtils.getCompositeConfig(method), false, args);
        method.setAccessible(true);
        if (executionChain == null) {
            return method.invoke(delegate, args);
        }
        return executionChain.execute(buildContext(aliasName, args), originalInvocation,
                () -> method.invoke(delegate, args));
    }

    @Override
    public void run(String resourceId, CompositeServiceKeeperConfig immutableConfig,
                    Runnable runnable, Object... args) throws Throwable {
        final Supplier<OriginalInvocation> originalInvocation = getOriginalInvocation();
        final SyncExecutionChain executionChain = buildExecutionChain(resourceId, originalInvocation,
                () -> immutableConfig, false, args);
        if (executionChain == null) {
            runnable.run();
        } else {
            executionChain.execute(buildContext(resourceId, args), originalInvocation, runnable);
        }
    }

    @Override
    public <T> T call(String resourceId, Supplier<CompositeServiceKeeperConfig> immutableConfigSupplier,
                      Supplier<OriginalInvocation> originalInvocation,
                      Callable<T> callable, Object[] args) throws Throwable {
        final SyncExecutionChain executionChain = buildExecutionChain(resourceId, originalInvocation,
                immutableConfigSupplier, false, args);
        if (executionChain == null) {
            return callable.call();
        }
        return executionChain.execute(buildContext(resourceId, args), originalInvocation, callable::call);
    }

    @Override
    protected ExternalConfig getExternalConfig(ResourceId resourceId) {
        if (configSource == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}'s external config: null, due that config source is null", resourceId);
            }
            return null;
        }

        final ExternalConfig config = configSource.config(resourceId);
        if (config != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained {}'s external config: {}", resourceId, config);
            }
            return config;
        }

        // Fallback to get arg values' config which matches alls, eg:
        // args.test.rateLimit1.appId.limitForPeriod={appC: 10, appA: 10, appB: 20, *: 3}
        // args.test.rateLimit1.appId.maxRateLimitValueSize=5
        // When appId's value equals appD then the external config should be limitForPeriod = 3, rather null.
        // Note that this logic is important to fix bug, because that when a resourceId's external config and
        // immutable are all null then no moat will be created.
        if (resourceId instanceof ArgResourceId) {
            final ArgResourceId argId = (ArgResourceId) resourceId;
            final ArgResourceId matchAllArgId = new ArgResourceId(argId.getMethodAndArgId().getName(),
                    VALUE_MATCH_ALL);

            final ExternalConfig config0 = configSource.config(matchAllArgId);
            if (logger.isDebugEnabled()) {
                logger.debug("Obtained arg {}'s external config: {}", matchAllArgId, config0);
            }
            return config0;
        }

        return null;
    }

    @Override
    protected CompositeServiceKeeperConfig getOrComputeConfig(ResourceId resourceId,
                                                              Supplier<CompositeServiceKeeperConfig> immutableConfig) {
        return immutableConfigs.getOrCompute(resourceId, immutableConfig);
    }

    /**
     * Get supplier of originalInvocationWrapper from method
     *
     * @param method original method
     * @return Supplier
     */
    protected Supplier<OriginalInvocation> getOriginalInvocation(final Method method) {
        return () -> new OriginalInvocation(method.getReturnType(), method.getParameterTypes());
    }

    protected SyncContext buildContext(String resourceId, Object[] args) {
        return new SyncContext(resourceId, args);
    }

    /**
     * Get supplier of originalInvocationWrapper from runnable
     *
     * @return Supplier
     */
    protected Supplier<OriginalInvocation> getOriginalInvocation() {
        return () -> new OriginalInvocation(void.class, new Class[0]);
    }
}
