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

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.ConcurrentOverFlowException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.listener.FondConfigListener;
import esa.servicekeeper.core.metrics.ConcurrentLimitMetrics;
import esa.servicekeeper.core.moats.AbstractMoat;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.MoatEventImpl;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.moats.MoatType;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.TimerLogger;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static esa.servicekeeper.core.config.ConcurrentLimitConfig.builder;
import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasConcurrent;
import static esa.servicekeeper.core.utils.ConfigUtils.combine;

public class ConcurrentLimitMoat extends AbstractMoat<ConcurrentLimitConfig>
        implements FondConfigListener<ConcurrentLimitConfig>,
        LifeCycleSupport {

    private static final Logger logger = LogUtils.logger();

    private static final ConcurrentLimiterRegistry REGISTRY = ConcurrentLimiterRegistry.singleton();

    private final TimerLogger timerLogger = new TimerLogger();
    private final AtomicBoolean shouldDestroy = new AtomicBoolean(false);
    private final LifeCycleType lifeCycleType;
    private final ConcurrentLimiter limiter;

    public ConcurrentLimitMoat(MoatConfig config, ConcurrentLimitConfig limitConfig,
                               ConcurrentLimitConfig immutableConfig,
                               List<MoatEventProcessor> processors) {
        super(processors, config);
        Checks.checkNotNull(limitConfig, "limitConfig");
        this.lifeCycleType = immutableConfig == null ? LifeCycleType.TEMPORARY : LifeCycleType.PERMANENT;
        this.limiter = REGISTRY.getOrCreate(config.getResourceId().getName(), limitConfig, immutableConfig, processors);
    }

    @Override
    public void tryThrough(Context ctx) throws ServiceKeeperNotPermittedException {
        if (!hasProcessors) {
            if (!limiter.acquirePermission()) {
                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **
                timerLogger.logPeriodically("The concurrent exceeds limit {}, which name is {}",
                        limiter.config().getThreshold(), limiter.name());
                throw notPermittedException(ctx);
            }
        } else {
            if (limiter.acquirePermission()) {
                process(MoatEventImpl.PERMITTED);
            } else {
                process(MoatEventImpl.REJECTED_BY_CONCURRENT_LIMIT);
                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **

                timerLogger.logPeriodically("The concurrent exceeds limit {}, which name is {}",
                        limiter.config().getThreshold(), limiter.name());
                throw notPermittedException(ctx);
            }
        }
    }

    @Override
    public void exit(Context ctx) {
        limiter.release();
    }

    @Override
    public ConcurrentLimitConfig config() {
        int threshold = this.limiter.metrics().threshold();
        return builder().threshold(threshold).build();
    }

    @Override
    public ConcurrentLimitConfig getFond(ExternalConfig config) {
        if (limiter.immutableConfig() == null && (!hasConcurrent(config))) {
            return null;
        }
        return combine(limiter.immutableConfig(), config);
    }

    @Override
    public void updateWithNewestConfig(ConcurrentLimitConfig config) {
        // Update the limiter with config
        logger.info("Begin to update concurrentLimiter: {} with the newest threshold: {}", limiter.name(),
                config.getThreshold());
        limiter.changeThreshold(config.getThreshold());
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        if (lifeCycleType() == LifeCycleType.TEMPORARY) {
            preDestroy();
        } else {
            doReset();
        }
    }

    @Override
    public boolean isConfigEquals(ConcurrentLimitConfig newestConfig) {
        // If the new threshold equals the older one, do nothing.
        return limiter.config().equals(newestConfig);
    }

    @Override
    public LifeCycleType lifeCycleType() {
        return lifeCycleType;
    }

    @Override
    public boolean shouldDelete() {
        return shouldDestroy.get();
    }

    @Override
    public ResourceId listeningKey() {
        return ResourceId.from(limiter.name());
    }

    @Override
    public String toString() {
        return "ConcurrentLimitMoat-" + limiter.name();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public MoatType type() {
        return MoatType.CONCURRENT_LIMIT;
    }

    /**
     * Get concurrentLimiter of current moat
     *
     * @return concurrentLimiter
     */
    public ConcurrentLimiter getConcurrentLimiter() {
        return limiter;
    }

    @Override
    protected String name() {
        return limiter.name();
    }

    private ServiceKeeperNotPermittedException notPermittedException(Context ctx) {
        final int maxConcurrentLimit = limiter.metrics().threshold();
        final int currentCallCount = limiter.metrics().currentCallCount();
        return new ConcurrentOverFlowException(
                StringUtils.concat("The maxConcurrentLimit of ",
                        limiter.name(), ": " + maxConcurrentLimit), ctx,
                new ConcurrentLimitMetrics() {
                    @Override
                    public int threshold() {
                        return maxConcurrentLimit;
                    }

                    @Override
                    public int currentCallCount() {
                        return currentCallCount;
                    }
                }
        );
    }

    private void preDestroy() {
        if (logger.isDebugEnabled()) {
            logger.debug("Prepare to destroy the concurrentLimit moat: {}", limiter.name());
        }
        REGISTRY.unRegister(limiter.name());
        preDestroy0();
        shouldDestroy.getAndSet(true);
    }

    private void doReset() {
        updateWithNewestConfig(limiter.immutableConfig());
    }

}
