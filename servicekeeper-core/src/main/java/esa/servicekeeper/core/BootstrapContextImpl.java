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
package esa.servicekeeper.core;

import esa.commons.Checks;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.GroupConfigSourceImpl;
import esa.servicekeeper.core.configsource.MoatLimitConfigSource;
import esa.servicekeeper.core.configsource.MoatLimitConfigSourceImpl;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.factory.ConfigSourcesFactory;
import esa.servicekeeper.core.factory.EventProcessorFactory;
import esa.servicekeeper.core.factory.FallbackHandlerFactory;
import esa.servicekeeper.core.factory.LimitableMoatFactoryContext;
import esa.servicekeeper.core.factory.MoatClusterFactory;
import esa.servicekeeper.core.factory.MoatClusterFactoryImpl;
import esa.servicekeeper.core.factory.PredicateStrategyFactory;
import esa.servicekeeper.core.factory.SateTransitionProcessorFactory;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.internal.MoatLimitConfigListener;
import esa.servicekeeper.core.internal.impl.CacheMoatClusterImpl;
import esa.servicekeeper.core.internal.impl.ImmutableConfigsImpl;
import esa.servicekeeper.core.internal.impl.MoatCreationLimitImpl;
import esa.servicekeeper.core.internal.impl.OverLimitMoatHandler;
import esa.servicekeeper.core.moats.MoatStatisticsImpl;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.SpiUtils;
import esa.commons.logging.Logger;

import java.util.Collections;
import java.util.List;

class BootstrapContextImpl implements BootstrapContext {

    private static final Logger logger = LogUtils.logger();

    private static volatile BootstrapContext INSTANCE;

    private final List<AsyncResultHandler<?>> handlers;
    private final PlainConfigSource config;
    private final MoatClusterFactory factory;
    private final GroupConfigSource groupConfig;
    private final MoatLimitConfigSource limitConfig;

    private BootstrapContextImpl(List<AsyncResultHandler<?>> handlers,
                                 ConfigSourcesFactory sourcesFactory) {
        this.handlers = handlers;
        this.config = sourcesFactory == null ? null : sourcesFactory.plain();
        this.groupConfig = new GroupConfigSourceImpl(sourcesFactory == null ? null : sourcesFactory.group(),
                Internals.IMMUTABLE_CONFIGS);
        this.limitConfig = new MoatLimitConfigSourceImpl(sourcesFactory == null ? null : sourcesFactory.limit(),
                Internals.IMMUTABLE_CONFIGS);
        this.factory = buildFactory();
    }

    static BootstrapContext instance(List<AsyncResultHandler<?>> handlers) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (BootstrapContextImpl.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new BootstrapContextImpl(handlers,
                    SpiUtils.loadByPriority(ConfigSourcesFactory.class));

            logger.info("Instantiated Bootstrap successfully, async handlers: " + handlers);
            SpiUtils.loadAll(BootstrapContextListener.class)
                    .forEach((listener) -> listener.onInitialization(INSTANCE));
        }
        return INSTANCE;
    }

    @Override
    public MoatClusterFactory factory() {
        return Checks.checkNotNull(factory, "MoatClusterFactory is null");
    }

    @Override
    public GlobalConfig globalConfig() {
        return Internals.GLOBAL_CONFIG;
    }

    @Override
    public GroupConfigSource groupConfig() {
        return Checks.checkNotNull(groupConfig, "GroupConfigSource is null");
    }

    @Override
    public PlainConfigSource config() {
        return config;
    }

    @Override
    public List<AsyncResultHandler<?>> handlers() {
        return handlers;
    }

    @Override
    public InternalMoatCluster cluster() {
        return Internals.INTERNAL_CLUSTER;
    }

    @Override
    public ImmutableConfigs immutableConfigs() {
        return Internals.IMMUTABLE_CONFIGS;
    }

    @Override
    public MoatLimitConfigSource limitConfig() {
        return Checks.checkNotNull(limitConfig, "MoatLimitConfigSource is null");
    }

    @Override
    public List<MoatLimitConfigListener> listeners() {
        return Collections.singletonList(Internals.LISTENER);
    }

    private MoatClusterFactory buildFactory() {
        final LimitableMoatFactoryContext ctx = LimitableMoatFactoryContext.builder()
                .fallbackHandler(SpiUtils.loadByPriority(FallbackHandlerFactory.class))
                .processors(SpiUtils.loadAll(EventProcessorFactory.class))
                .strategy(SpiUtils.loadByPriority(PredicateStrategyFactory.class))
                .listeners(Collections.singletonList(Internals.MOAT_STATISTICS))
                .limite(new MoatCreationLimitImpl(Internals.MOAT_STATISTICS, limitConfig))
                .cProcessors(SpiUtils.loadByPriority(SateTransitionProcessorFactory.class)).build();

        return new MoatClusterFactoryImpl(ctx, cluster(), immutableConfigs());
    }

    private static class Internals {
        private static final GlobalConfig GLOBAL_CONFIG = new GlobalConfig();

        private static final MoatStatisticsImpl MOAT_STATISTICS = new MoatStatisticsImpl();

        private static final InternalMoatCluster INTERNAL_CLUSTER = new CacheMoatClusterImpl();

        private static final ImmutableConfigs IMMUTABLE_CONFIGS = new ImmutableConfigsImpl();

        private static final MoatLimitConfigListener LISTENER = new OverLimitMoatHandler(INTERNAL_CLUSTER,
                IMMUTABLE_CONFIGS);
    }
}
