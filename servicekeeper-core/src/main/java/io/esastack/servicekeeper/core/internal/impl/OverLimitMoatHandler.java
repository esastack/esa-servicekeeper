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
package io.esastack.servicekeeper.core.internal.impl;

import esa.commons.Checks;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.core.internal.InternalMoatCluster;
import io.esastack.servicekeeper.core.internal.MoatLimitConfigListener;
import io.esastack.servicekeeper.core.moats.MoatCluster;
import io.esastack.servicekeeper.core.moats.RetryableMoatCluster;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.getMaxSizeLimit;

public class OverLimitMoatHandler implements MoatLimitConfigListener {

    private static final Logger logger = LogUtils.logger();

    private final InternalMoatCluster cluster;
    private final ImmutableConfigs configs;

    public OverLimitMoatHandler(InternalMoatCluster cluster, ImmutableConfigs configs) {
        Checks.checkNotNull(cluster, "cluster");
        Checks.checkNotNull(configs, "configs");
        this.cluster = cluster;
        this.configs = configs;
    }

    @Override
    public void onUpdate(ArgConfigKey key, Integer oldMaxSizeLimit, Integer newMaxSizeLimit) {
        final int newSizeLimit = (newMaxSizeLimit == null ? getImmutable(key) : newMaxSizeLimit);

        logger.info("Begin to handle over limit moats, key: {}, old max size limit:{}, new max size limit:{}",
                key, oldMaxSizeLimit, newMaxSizeLimit);

        final Map<ResourceId, MoatCluster> clusters = cluster.getAll();
        int currentCount = 0;
        final Set<ResourceId> toDeleteIds = new HashSet<>(0);
        for (Map.Entry<ResourceId, MoatCluster> entry : clusters.entrySet()) {
            if (!(entry.getKey() instanceof ArgResourceId)) {
                continue;
            }

            final ArgResourceId argId = (ArgResourceId) entry.getKey();
            if (!argId.getMethodAndArgId().getName().equals(key.getMethodId().getName() + "." + key.getArgName())) {
                continue;
            }

            final MoatCluster cluster0 = entry.getValue();
            if (cluster0.contains(key.getType())) {
                currentCount++;

                if (currentCount > newSizeLimit) {
                    cluster0.remove(key.getType());
                    logger.info("Removed {}'s {} moat, current index:{}, new size limit:{}",
                            entry.getKey(), key.getType(), currentCount, newSizeLimit);

                    if (cluster0.getAll().isEmpty() && !(RetryableMoatCluster.isInstance(cluster0))) {
                        toDeleteIds.add(entry.getKey());
                    }
                }
            }
        }

        toDeleteIds.forEach((id) -> {
            cluster.remove(id);
            logger.info("Removed {}'s moat cluster successfully", id);
        });
    }

    private Integer getImmutable(final ArgConfigKey key) {
        final Integer immutableMaxSizeLimit = configs
                .getMaxSizeLimit(key.getMethodId(), key.getArgName(), key.getType());
        if (immutableMaxSizeLimit != null) {
            return immutableMaxSizeLimit;
        }

        return getMaxSizeLimit(key);
    }

}
