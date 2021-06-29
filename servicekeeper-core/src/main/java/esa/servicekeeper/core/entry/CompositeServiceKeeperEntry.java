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
package esa.servicekeeper.core.entry;

import esa.commons.reflect.BeanUtils;
import esa.commons.reflect.ReflectionUtils;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.executionchain.AbstractExecutionChain;
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.factory.MoatClusterFactory;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.utils.MethodUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static esa.servicekeeper.core.asynchandle.RequestHandle.DEFAULT_PASS_WITHOUT_OBSTACLE;

/**
 * The composite entry which implements from {@link ServiceKeeperEntry} and {@link ServiceKeeperAsyncEntry}, Which
 * supports async invocation and group.
 */
public class CompositeServiceKeeperEntry extends DefaultServiceKeeperEntry implements ServiceKeeperAsyncEntry {

    private final GroupConfigSource groupConfig;
    private final List<AsyncResultHandler<?>> handlers;
    private final boolean absentHandlers;

    public CompositeServiceKeeperEntry(PlainConfigSource config,
                                       ImmutableConfigs immutableConfigs,
                                       MoatClusterFactory factory,
                                       GlobalConfig globalConfig,
                                       GroupConfigSource groupConfig,
                                       List<AsyncResultHandler<?>> handlers) {
        super(config, immutableConfigs, factory, globalConfig);
        this.handlers = handlers == null
                ? null : Collections.unmodifiableList(handlers);
        absentHandlers = handlers == null || handlers.isEmpty();
        this.groupConfig = groupConfig;
    }

    @Override
    public Object invoke(String aliasName, Method method, Object delegate, Object... args) throws Throwable {
        if (absentHandlers) {
            return super.invoke(aliasName, method, delegate, args);
        } else {
            final Supplier<OriginalInvocation> originalInvocation =
                    getOriginalInvocation(method);

            final Class<?> returnType = method.getReturnType();

            method.setAccessible(true);
            final Executable<?> executable = () -> method.invoke(delegate, args);
            final Supplier<CompositeServiceKeeperConfig> configSupplier = () -> MethodUtils.getCompositeConfig(method);

            for (AsyncResultHandler<?> asyncResultHandler : handlers) {
                if (asyncResultHandler.supports(returnType)) {
                    final AbstractExecutionChain executionChain =
                            buildExecutionChain(aliasName, getOriginalInvocation(method),
                                    configSupplier, true, args);
                    if (executionChain == null) {
                        return method.invoke(delegate, args);
                    }

                    return executionChain.asyncExecute(buildAsyncContext(aliasName, args),
                            originalInvocation, executable,
                            asyncResultHandler);
                }
            }

            AbstractExecutionChain executionChain =
                    buildExecutionChain(aliasName, getOriginalInvocation(method), configSupplier,
                            false, args);
            if (executionChain == null) {
                return method.invoke(delegate, args);
            }

            return executionChain.execute(buildContext(aliasName, args), originalInvocation, executable);
        }
    }

    @Override
    public <T> T call(String resourceId, Supplier<CompositeServiceKeeperConfig> immutableConfig,
                      Supplier<OriginalInvocation> originalInvocation,
                      Callable<T> callable, Object... args) throws Throwable {
        if (absentHandlers) {
            return super.call(resourceId, immutableConfig, originalInvocation, callable, args);
        } else {
            final Class<T> returnType = originalInvocation == null ? null :
                    (Class<T>) originalInvocation.get().getReturnType();
            if (returnType == null) {
                return super.call(resourceId, immutableConfig, originalInvocation, callable, args);
            }

            for (AsyncResultHandler<?> asyncResultHandler : handlers) {
                if (asyncResultHandler.supports(returnType)) {
                    final AbstractExecutionChain executionChain =
                            buildExecutionChain(resourceId, originalInvocation,
                                    immutableConfig, true, args);
                    if (executionChain == null) {
                        return callable.call();
                    }

                    return executionChain.asyncExecute(buildAsyncContext(resourceId, args),
                            originalInvocation, callable::call,
                            asyncResultHandler);
                }
            }

            final AbstractExecutionChain executionChain =
                    buildExecutionChain(resourceId, originalInvocation,
                            immutableConfig, false, args);
            if (executionChain == null) {
                return callable.call();
            }
            return executionChain.execute(buildAsyncContext(resourceId, args), originalInvocation,
                    callable::call);
        }
    }

    @Override
    public RequestHandle tryAsyncExecute(String resourceId, OriginalInvocation originalInvocation,
                                         Object... args) {
        return tryAsyncExecute(resourceId, null, originalInvocation, args);
    }

    @Override
    public RequestHandle tryAsyncExecute(String resourceId, CompositeServiceKeeperConfig immutableConfig,
                                         OriginalInvocation originalInvocation, Object... args) {
        AbstractExecutionChain executionChain = buildExecutionChain(resourceId, () -> originalInvocation,
                () -> immutableConfig, true, args);
        if (executionChain == null) {
            return DEFAULT_PASS_WITHOUT_OBSTACLE;
        }
        return executionChain.tryToExecute(buildAsyncContext(resourceId, args));
    }

    @Override
    protected ExternalConfig getExternalConfig(ResourceId resourceId) {
        final ExternalConfig config = super.getExternalConfig(resourceId);
        final ExternalConfig groupConfig = getGroupConfig(resourceId);
        if (groupConfig == null) {
            return config;
        }
        return overrideGroupConfig(config, groupConfig);
    }

    private AsyncContext buildAsyncContext(String resourceId, Object[] args) {
        return new AsyncContext(resourceId, args);
    }

    private ExternalConfig getGroupConfig(final ResourceId resourceId) {
        if (groupConfig == null) {
            return null;
        }
        GroupResourceId groupId = groupConfig.mappingGroupId(resourceId);
        if (groupId == null) {
            return null;
        }

        return groupConfig.config(groupId);
    }

    private ExternalConfig overrideGroupConfig(final ExternalConfig methodConfig, final ExternalConfig groupConfig) {
        if (methodConfig == null) {
            return groupConfig;
        }

        for (Field field : ReflectionUtils.getAllDeclaredFields(ExternalConfig.class)) {
            final Object value;
            if ((value = BeanUtils.getFieldValue(methodConfig, field.getName())) == null) {
                continue;
            }
            BeanUtils.setFieldValue(groupConfig, field.getName(), value);
        }

        return groupConfig;
    }
}
