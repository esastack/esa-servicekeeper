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

import esa.commons.Checks;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.factory.MoatClusterFactory;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.internal.MoatLimitConfigListener;
import esa.servicekeeper.core.listener.ExternalConfigListener;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.RetryableMoatCluster;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.retry.RetryOperations;
import esa.servicekeeper.core.retry.RetryOperationsImpl;
import esa.servicekeeper.core.retry.RetryableExecutor;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

public class InternalsUpdaterImpl implements InternalsUpdater {

    private static final Logger logger = LogUtils.logger();

    private final GlobalConfig globalConfig;
    private final InternalMoatCluster cluster;
    private final GroupConfigSource groupConfig;
    private final MoatClusterFactory factory;
    private final List<MoatLimitConfigListener> limitListeners;

    public InternalsUpdaterImpl(InternalMoatCluster cluster, GroupConfigSource groupConfig,
                                MoatClusterFactory factory, GlobalConfig globalConfig,
                                List<MoatLimitConfigListener> limitListeners) {
        Checks.checkNotNull(cluster, "InternalMoatCluster must not be null");
        Checks.checkNotNull(groupConfig, "GroupConfigSource must not be null");
        Checks.checkNotNull(factory, "MoatClusterFactory must not be null");
        Checks.checkNotNull(globalConfig, "GlobalConfig must not be null");
        this.globalConfig = globalConfig;
        this.cluster = cluster;
        this.groupConfig = groupConfig;
        this.factory = factory;
        this.limitListeners = limitListeners == null ? emptyList() : unmodifiableList(limitListeners);
    }

    @Override
    public void update(ResourceId resourceId, ExternalConfig config) {
        if (resourceId instanceof GroupResourceId) {
            updateGroup((GroupResourceId) resourceId, config);
            return;
        }

        doUpdate(resourceId, config);
    }

    @Override
    public void updateMatchAllConfig(ResourceId methodAndArgId,
                                     ExternalConfig config,
                                     Set<Object> values) {
        final Set<ResourceId> ids = cluster.getAll().keySet();
        for (ResourceId id : ids) {
            if ((id instanceof ArgResourceId) &&
                    ((ArgResourceId) id).getMethodAndArgId().equals(methodAndArgId)) {
                final ArgResourceId id0 = (ArgResourceId) id;
                if (values.contains(id0.getArgValue())) {
                    continue;
                }

                doUpdate(id, config);
            }
        }
    }

    @Override
    public void updateGlobalDisable(Boolean globalDisable) {
        globalConfig.updateGlobalDisable(globalDisable);
    }

    @Override
    public void updateArgLevelEnable(Boolean argLevelEnable) {
        globalConfig.updateArgLevelEnable(argLevelEnable);
    }

    @Override
    public void updateRetryEnable(Boolean retryEnable) {
        globalConfig.updateRetryEnable(retryEnable);
    }

    @Override
    public void updateMaxSizeLimit(ArgConfigKey key, Integer oldMaxSizeLimit, Integer newMaxSizeLimit) {
        limitListeners.forEach((listener) -> listener.onUpdate(key, oldMaxSizeLimit, newMaxSizeLimit));
    }

    private void doUpdate(ResourceId resourceId, ExternalConfig config) {
        final MoatCluster cluster0 = cluster.get(resourceId);
        List<ExternalConfigListener> listeners = detectListeners(cluster0);

        logger.info("Begin to update {}'s all dynamic configuration listeners(moats): {}, config: {}",
                resourceId, listeners, config);

        if (listeners == null || listeners.isEmpty()) {
            return;
        }

        for (ExternalConfigListener listener : listeners) {
            listener.onUpdate(config);

            if (listener instanceof LifeCycleSupport && ((LifeCycleSupport) listener).shouldDelete()) {
                if (listener instanceof Moat) {
                    cluster0.remove((Moat) listener);
                } else if (cluster0 instanceof RetryableMoatCluster && listener instanceof RetryOperationsImpl) {
                    ((RetryableMoatCluster) cluster0).updateRetryExecutor(null);
                }
                logger.info("Removed {}'s listener(moat): {} from moat cluster successfully",
                        resourceId, listener);
            }
        }

        if (shouldDestroy(cluster0)) {
            logger.info("Removed {}'s moat cluster", resourceId.getName());
            cluster.remove(resourceId);
        } else {
            factory.update(resourceId, cluster0, config);
        }
    }

    private synchronized void updateGroup(GroupResourceId key, ExternalConfig config) {
        final Set<ResourceId> groupItems = groupConfig.mappingResourceIds(key);
        if (groupItems == null || groupItems.isEmpty()) {
            logger.info("Begin to update group: {}'s all items: [{}], config: {}",
                    key.toString(), groupItems, config);
            return;
        }

        logger.info("Begin to update group: {}'s all items: [{}], config: {}", key.toString(), groupItems, config);
        groupItems.forEach(methodId -> doUpdate(methodId, config));
    }

    /**
     * Detect listeners from {@link MoatCluster}.
     *
     * @param cluster0 {@link MoatCluster}
     * @return listeners set
     */
    private List<ExternalConfigListener> detectListeners(final MoatCluster cluster0) {
        if (cluster0 == null) {
            return null;
        }

        final List<ExternalConfigListener> listeners = new ArrayList<>(5);
        RetryableExecutor executor;
        if (cluster0 instanceof RetryableMoatCluster &&
                (executor = ((RetryableMoatCluster) cluster0).retryExecutor()) != null) {
            final RetryOperations operations = executor.getOperations();
            if (operations instanceof ExternalConfigListener) {
                listeners.add((RetryOperationsImpl) operations);
            }
        }

        for (Moat moat : cluster0.getAll()) {
            if (moat instanceof ExternalConfigListener) {
                listeners.add((ExternalConfigListener) moat);
            }

            if (moat instanceof CircuitBreakerMoat) {
                final PredicateStrategy predicate = ((CircuitBreakerMoat) moat).getPredicate();
                if (predicate instanceof ExternalConfigListener) {
                    listeners.add((ExternalConfigListener) predicate);
                }
            }
        }

        return listeners;
    }

    /**
     * Whether should destroy the moatCluster. If the moats in the moat cluster is empty and the retry
     * executor is null, destroy the moat soon.
     *
     * @param cluster0 target moatCluster
     * @return true or false
     */
    private boolean shouldDestroy(MoatCluster cluster0) {
        if (!cluster0.getAll().isEmpty()) {
            return false;
        }

        final boolean retryable = cluster0 instanceof RetryableMoatCluster;
        return (!retryable) || ((RetryableMoatCluster) cluster0).retryExecutor() == null;
    }
}
