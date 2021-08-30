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
package io.esastack.servicekeeper.core;

import io.esastack.servicekeeper.core.asynchandle.AsyncResultHandler;
import io.esastack.servicekeeper.core.configsource.GroupConfigSource;
import io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource;
import io.esastack.servicekeeper.core.configsource.PlainConfigSource;
import io.esastack.servicekeeper.core.factory.MoatClusterFactory;
import io.esastack.servicekeeper.core.internal.GlobalConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.internal.MoatLimitConfigListener;

import java.util.List;

public interface BootstrapContext {

    /**
     * Return the singleton of {@link BootstrapContext}.
     *
     * @param handlers handlers
     * @return ctx
     */
    static BootstrapContext singleton(List<AsyncResultHandler<?>> handlers) {
        return BootstrapContextImpl.instance(handlers);
    }

    /**
     * Obtains the {@link MoatClusterFactory}.
     *
     * @return factory
     */
    MoatClusterFactory factory();

    /**
     * Obtains the {@link GlobalConfig}.
     *
     * @return global config
     */
    GlobalConfig globalConfig();

    /**
     * Obtains the {@link GroupConfigSource}
     *
     * @return group config
     */
    GroupConfigSource groupConfig();

    /**
     * Obtains the {@link AsyncResultHandler}s
     *
     * @return handlers
     */
    List<AsyncResultHandler<?>> handlers();

    /**
     * Obtains the {@link PlainConfigSource}.
     *
     * @return config source
     */
    PlainConfigSource config();

    /**
     * Get the {@link InternalMoatCluster} which is singleton.
     *
     * @return {@link InternalMoatCluster}.
     */
    InternalMoatCluster cluster();

    /**
     * Get the {@link ImmutableConfigs} which is singleton.
     *
     * @return {@link ImmutableConfigs}.
     */
    ImmutableConfigs immutableConfigs();

    /**
     * Get the {@link MoatLimitConfigSource} which is singleton.
     *
     * @return {@link MoatLimitConfigSource}.
     */
    MoatLimitConfigSource limitConfig();

    /**
     * Get the {@link MoatLimitConfigListener}s.
     *
     * @return {@link MoatLimitConfigListener}s.
     */
    List<MoatLimitConfigListener> listeners();

}
