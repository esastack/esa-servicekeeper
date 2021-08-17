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
package esa.servicekeeper.core.internal.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.SystemConfigUtils;
import esa.commons.logging.Logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.github.benmanes.caffeine.cache.RemovalCause.SIZE;
import static java.util.Collections.unmodifiableMap;

public class CacheMoatClusterImpl implements InternalMoatCluster {

    public static final String DEFAULT_CACHE_SIZE_KEY = "servicekeeper.moat-clusters.cache.size";
    public static final String DEFAULT_EXPIRE_TIME_SECONDS_KEY = "servicekeeper.moat-clusters." +
            "cache.expire.seconds";

    private static final Logger logger = LogUtils.logger();

    private static final int DEFAULT_CLUSTERS_SIZE = 5000;
    private static final int DEFAULT_EXPIRE_TIME_SECONDS = 3600;

    private final Cache<ResourceId, MoatCluster> cache = Caffeine.newBuilder()
            .maximumSize(getCacheSize())
            .expireAfterAccess(getExpireTimeSeconds(), TimeUnit.SECONDS)
            .removalListener((k, v, c) -> {
                if (SIZE == c) {
                    logger.error("Removed {}'s moat cluster: {} successfully," +
                            " caused by: {}", k, v, c);
                } else {
                    logger.info("Removed {}'s moat cluster: {} successfully," +
                            " caused by: {}", k, v, c);
                }
            })
            .build();

    @Override
    public MoatCluster get(ResourceId resourceId) {
        return cache.getIfPresent(resourceId);
    }

    @Override
    public Map<ResourceId, MoatCluster> getAll() {
        return unmodifiableMap(cache.asMap());
    }

    @Override
    public void remove(ResourceId resourceId) {
        if (resourceId == null) {
            return;
        }
        cache.invalidate(resourceId);
    }

    @Override
    public MoatCluster computeIfAbsent(ResourceId resourceId, Function<ResourceId, MoatCluster> function) {
        try {
            return cache.get(resourceId, function);
        } catch (Throwable th) {
            logger.error("Failed to create moat cluster, resourceId: {}", resourceId, th);
            return null;
        }
    }

    private static int getCacheSize() {
        final String cacheSize = SystemConfigUtils.getFromEnvAndProp(DEFAULT_CACHE_SIZE_KEY);
        try {
            return Integer.parseInt(cacheSize);
        } catch (NumberFormatException ex) {
            // ignore
        }

        return DEFAULT_CLUSTERS_SIZE;
    }

    private static int getExpireTimeSeconds() {
        final String expireTimeSeconds = SystemConfigUtils.getFromEnvAndProp(DEFAULT_EXPIRE_TIME_SECONDS_KEY);
        try {
            return expireTimeSeconds != null ? Integer.parseInt(expireTimeSeconds) : DEFAULT_EXPIRE_TIME_SECONDS;
        } catch (NumberFormatException ex) {
            // ignore
        }

        return DEFAULT_EXPIRE_TIME_SECONDS;
    }
}

