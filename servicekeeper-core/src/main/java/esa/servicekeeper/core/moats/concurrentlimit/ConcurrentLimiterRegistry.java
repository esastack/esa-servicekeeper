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
package esa.servicekeeper.core.moats.concurrentlimit;

import esa.commons.StringUtils;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.moats.Registry;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentLimiterRegistry implements Registry<ConcurrentLimiter, ConcurrentLimitConfig> {

    private static final Logger logger = LogUtils.logger();

    private final Map<String, ConcurrentLimiter> limiterMap = new ConcurrentHashMap<>(64);

    private ConcurrentLimiterRegistry() {
    }

    public static ConcurrentLimiterRegistry singleton() {
        return ConcurrentLimiterRegistryHolder.INSTANCE;
    }

    @Override
    public ConcurrentLimiter getOrCreate(final String name, final ConcurrentLimitConfig config,
                                         final ConcurrentLimitConfig immutableConfig,
                                         final List<MoatEventProcessor> processors) {
        if (StringUtils.isEmpty(name)) {
            return new AtomicConcurrentLimiter(null, config, immutableConfig);
        }
        return limiterMap.computeIfAbsent(name,
                key -> new AtomicConcurrentLimiter(name, config, immutableConfig));
    }

    @Override
    public void unRegister(String name) {
        if (limiterMap.remove(name) != null && logger.isDebugEnabled()) {
            logger.info("Removed concurrentLimiter: {} from registry successfully", name);
        }
    }

    private static class ConcurrentLimiterRegistryHolder {
        private static final ConcurrentLimiterRegistry INSTANCE = new ConcurrentLimiterRegistry();
    }
}
