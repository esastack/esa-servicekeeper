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
package esa.servicekeeper.core.configsource;

import esa.commons.Checks;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

public class MoatLimitConfigSourceImpl implements MoatLimitConfigSource {

    private static final Logger logger = LogUtils.logger();

    private final MoatLimitConfigSource limitConfigSource;
    private final ImmutableConfigs immutableConfigs;

    public MoatLimitConfigSourceImpl(MoatLimitConfigSource limitConfigSource, ImmutableConfigs immutableConfigs) {
        Checks.checkNotNull(immutableConfigs, "immutableConfigs");
        this.immutableConfigs = immutableConfigs;
        this.limitConfigSource = limitConfigSource;
    }

    @Override
    public Integer maxSizeLimit(final ArgConfigKey key) {
        if (limitConfigSource == null) {
            logger.info("The external moat limit config source is null, try to get immutable" +
                    " maxSizeLimit of {} from immutable config", key);

            return getImmutable(key);
        }

        final Integer mutableMaxSizeLimit = limitConfigSource.maxSizeLimit(key);
        if (mutableMaxSizeLimit != null) {
            logger.info("The maxSizeLimit of {} got from external config source is {}", key, mutableMaxSizeLimit);

            return mutableMaxSizeLimit;
        }

        logger.info("The maxSizeLimit of {} got from external config source is null," +
                " try to get from immutable config", key);
        return getImmutable(key);
    }

    private Integer getImmutable(final ArgConfigKey key) {
        final Integer immutableMaxSizeLimit = immutableConfigs
                .getMaxSizeLimit(key.getMethodId(), key.getArgName(), key.getType());
        if (immutableMaxSizeLimit != null) {
            logger.info("The maxSizeLimit of {} which got from immutable config is {}", key, immutableMaxSizeLimit);
            return immutableMaxSizeLimit;
        }

        int defaultMaxSizeLimit = MoatLimitConfigSource.getMaxSizeLimit(key);
        logger.info("The maxSizeLimit of {} which got from immutable config is null, default value {} is returned",
                key, defaultMaxSizeLimit);
        return defaultMaxSizeLimit;
    }
}
