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
import io.esastack.servicekeeper.core.common.LimitableKey;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource;
import io.esastack.servicekeeper.core.internal.MoatCreationLimit;
import io.esastack.servicekeeper.core.moats.MoatStatistics;
import io.esastack.servicekeeper.core.utils.LogUtils;

public class MoatCreationLimitImpl implements MoatCreationLimit {

    private static final Logger logger = LogUtils.logger();

    private final MoatStatistics statistics;
    private final MoatLimitConfigSource config;

    public MoatCreationLimitImpl(MoatStatistics statistics,
                                 MoatLimitConfigSource config) {
        Checks.checkNotNull(statistics, "statistics");
        Checks.checkNotNull(config, "config");
        this.statistics = statistics;
        this.config = config;
    }

    @Override
    public boolean canCreate(LimitableKey key) {
        final ResourceId id = key.getId();
        if (!(id instanceof ArgResourceId)) {
            if (logger.isDebugEnabled()) {
                logger.debug("MethodId {} is permitted to create a new moat", key.getId());
            }
            return true;
        }

        final ArgConfigKey argKey = new ArgConfigKey((ArgResourceId) id, key.getType());
        final Integer maxSizeLimit = config.maxSizeLimit(argKey);

        if (maxSizeLimit == null || maxSizeLimit <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Creating {}'s moat isn't permitted, maxSizeLimit: {}", argKey,
                        maxSizeLimit == null ? "null" : maxSizeLimit);
            }
            return false;
        }

        final int count = statistics.countOf(argKey);
        if (count <= maxSizeLimit) {
            logger.info("Creating {}'s moat is permitted, current statistics count: {}, maxSizeLimit: {}", argKey,
                    count, maxSizeLimit);
            return true;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Creating {}'s moat isn't permitted, current statistics count: {}, maxSizeLimit: {}",
                    argKey, count, maxSizeLimit);
        }
        return false;
    }

}

