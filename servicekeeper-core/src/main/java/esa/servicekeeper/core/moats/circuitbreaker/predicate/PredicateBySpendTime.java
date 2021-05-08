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
package esa.servicekeeper.core.moats.circuitbreaker.predicate;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.listener.FondConfigListener;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

public class PredicateBySpendTime implements PredicateStrategy, PredicateConfigFilling,
        FondConfigListener<Long> {

    private static final Logger logger = LogUtils.logger();

    private static final ResourceId DEFAULT_NOT_NAMED_ID = ResourceId.from("Not Named");

    /**
     * This name is designed for dynamic configuration. You can change the maxSpendTimeMs by:
     * ${name}.maxSpendTimeMs: eg: com.servicekeeper.demos.DemoClass.method0.maxSpendTimeMs=10
     */
    private final ResourceId name;

    /**
     * Save the original maxSpendTimeMs for further use, When the dynamic config is null, use this as default.
     */
    private final long originalMaxSpendTimeMs;

    /**
     * Current maxSpendTimeMs.
     */
    private volatile long maxSpendTimeMs;

    public PredicateBySpendTime(long maxSpendTimeMs, long originalMaxSpendTimeMs, ResourceId name) {
        this.originalMaxSpendTimeMs = originalMaxSpendTimeMs <= 0 ? maxSpendTimeMs : originalMaxSpendTimeMs;
        if (originalMaxSpendTimeMs <= 0) {
            logger.info("The {}'s original maxSpendTimeMs is not greater than 0, use default value: {}", name,
                    maxSpendTimeMs);
        }
        this.maxSpendTimeMs = maxSpendTimeMs;
        this.name = name;
    }

    public PredicateBySpendTime(long maxSpendTimeMs) {
        this(maxSpendTimeMs, maxSpendTimeMs, DEFAULT_NOT_NAMED_ID);
    }

    @Override
    public boolean isSuccess(Context ctx) {
        if (maxSpendTimeMs < 0) {
            return true;
        }
        return ctx.getSpendTimeMs() <= maxSpendTimeMs;
    }

    @Override
    public Long getFond(ExternalConfig config) {
        if (config == null || config.getMaxSpendTimeMs() == null) {
            return null;
        } else {
            return config.getMaxSpendTimeMs();
        }
    }

    @Override
    public void updateWithNewestConfig(Long newestConfig) {
        logger.info("The {}'s newest maxSpendTimeMs: {}", name, newestConfig);
        maxSpendTimeMs = newestConfig;
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        logger.info("The {}'s newest maxSpendTimeMs got from dynamic config is null," +
                " the original value {} will be used", name, originalMaxSpendTimeMs);
        maxSpendTimeMs = originalMaxSpendTimeMs;
    }

    @Override
    public boolean isConfigEquals(Long newestConfig) {
        return newestConfig != null && newestConfig == maxSpendTimeMs;
    }

    @Override
    public ResourceId listeningKey() {
        return name;
    }

    @Override
    public String toString() {
        return "PredicateBySpendTime-" + name.getName();
    }

    @Override
    public void fill(CircuitBreakerConfig config) {
        config.updateMaxSpendTimeMs(maxSpendTimeMs);
    }

    public long getMaxSpendTimeMs() {
        return maxSpendTimeMs;
    }

}

