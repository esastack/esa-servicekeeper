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
package io.esastack.servicekeeper.core.internal;

import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import io.esastack.servicekeeper.core.moats.MoatType;

import java.util.Set;
import java.util.function.Supplier;

public interface ImmutableConfigs {

    /**
     * Get config by specified {@link ResourceId} and {@link ConfigType}.
     *
     * @param resourceId resourceId
     * @param type       type
     * @return {@link RetryConfig}, {@link ConcurrentLimitConfig} and so on, and possible be null.
     */
    Object getConfig(ResourceId resourceId, ConfigType type);

    /**
     * Get {@link GroupResourceId} of specified {@link ResourceId}.
     *
     * @param resourceId resourceId
     * @return {@link GroupResourceId} or null.
     */
    GroupResourceId getGroupId(ResourceId resourceId);

    /**
     * Get {@link ResourceId} which belongs to specified {@link GroupResourceId}.
     *
     * @param groupId groupId
     * @return {@link ResourceId}s or null.
     */
    Set<ResourceId> getGroupItems(GroupResourceId groupId);

    /**
     * Get maxSizeLimit of specified args.
     *
     * @param methodId methodId
     * @param argName  argName
     * @param type     type
     * @return Integer or null.
     */
    Integer getMaxSizeLimit(final ResourceId methodId, String argName, MoatType type);

    /**
     * Get the {@link CompositeServiceKeeperConfig} and return, and just calculate if not available.
     *
     * @param resourceId      resourceId
     * @param immutableConfig immutable config
     * @return {@link CompositeServiceKeeperConfig} or null.
     */
    CompositeServiceKeeperConfig getOrCompute(ResourceId resourceId,
                                              Supplier<CompositeServiceKeeperConfig> immutableConfig);

    enum ConfigType {
        /**
         * RateLimit config
         */
        RATELIMIT_CONFIG,

        /**
         * CircuitBreaker config
         */
        CIRCUITBREAKER_CONFIG,

        /**
         * ConcurrentLimit config
         */
        CONCURRENTLIMIT_CONFIG,

        /**
         * Retry config
         */
        RETRY_CONFIG,

        /**
         * Fall back config.
         */
        FALLBACK_CONFIG
    }
}
