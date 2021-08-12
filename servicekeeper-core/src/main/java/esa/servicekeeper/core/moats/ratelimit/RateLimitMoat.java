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
package esa.servicekeeper.core.moats.ratelimit;

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.RateLimitOverflowException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.listener.FondConfigListener;
import esa.servicekeeper.core.metrics.RateLimitMetrics;
import esa.servicekeeper.core.moats.*;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.TimerLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasRate;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.PERMANENT;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.TEMPORARY;
import static esa.servicekeeper.core.utils.ConfigUtils.combine;

public class RateLimitMoat extends AbstractMoat<RateLimitConfig> implements
        FondConfigListener<RateLimitConfig>, LifeCycleSupport {

    private static final Logger logger = LogUtils.logger();

    private static final RateLimiterRegistry REGISTRY = RateLimiterRegistry.singleton();

    private final TimerLogger timerLogger = new TimerLogger();
    private final AtomicBoolean shouldDestroy = new AtomicBoolean(false);
    private final LifeCycleType lifeCycleType;
    private final RateLimiter limiter;

    public RateLimitMoat(MoatConfig config, RateLimitConfig limitConfig,
                         RateLimitConfig immutableConfig,
                         List<MoatEventProcessor> processors) {
        super(processors, config);
        Checks.checkNotNull(limitConfig, "limitConfig");
        this.lifeCycleType = immutableConfig == null ? TEMPORARY : PERMANENT;
        this.limiter = REGISTRY.getOrCreate(config.getResourceId().getName(), limitConfig,
                immutableConfig, processors);
    }

    @Override
    public void tryThrough(Context ctx) throws ServiceKeeperNotPermittedException {
        Duration maxWaitDuration = Duration.ZERO;

        if (!hasProcessors) {
            if (!limiter.acquirePermission(maxWaitDuration)) {
                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **
                timerLogger.logPeriodically("The rate limit exceeds threshold {}, which name is {}",
                        limiter.config().getLimitForPeriod(), limiter.name());
                throw notPermittedException(ctx);
            }
        } else {
            if (limiter.acquirePermission(maxWaitDuration)) {
                process(MoatEventImpl.PERMITTED);
            } else {
                process(MoatEventImpl.REJECTED_BY_RATE_LIMIT);

                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **
                timerLogger.logPeriodically("The rate limit exceeds threshold {}, which name is {}",
                        limiter.config().getLimitForPeriod(), limiter.name());
                throw notPermittedException(ctx);
            }
        }
    }

    @Override
    public void exit(Context ctx) {
        // Do nothing
    }

    @Override
    public RateLimitConfig config() {
        return this.rateLimiter().config();
    }

    @Override
    public RateLimitConfig getFond(ExternalConfig config) {
        if (limiter.immutableConfig() == null && (!hasRate(config))) {
            return null;
        }
        return combine(limiter.immutableConfig(), config);
    }

    @Override
    public void updateWithNewestConfig(RateLimitConfig config) {
        // Update the current limiter with config
        final String name = limiter.name();

        logger.info("Updating rateLimiter {} with the newest config: {}", name, config);
        limiter.changeConfig(config);
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        if (this.lifeCycleType() == TEMPORARY) {
            preDestroy();
        } else {
            doReset();
        }
    }

    @Override
    public boolean isConfigEquals(RateLimitConfig newestConfig) {
        // Whether the new limitForPeriod equals the older one
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
    public int getOrder() {
        return -1;
    }

    @Override
    public MoatType type() {
        return MoatType.RATE_LIMIT;
    }

    @Override
    public String toString() {
        return "RateLimitMoat-" + limiter.name();
    }

    /**
     * Get rateLimiter of current moat.
     *
     * @return rateLimiter
     */
    public RateLimiter rateLimiter() {
        return limiter;
    }

    @Override
    protected String name() {
        return limiter.name();
    }

    private ServiceKeeperNotPermittedException notPermittedException(Context ctx) {
        return new RateLimitOverflowException(StringUtils.concat("The limitForPeriod of rateLimiter ",
                limiter.name(), ": " + limiter.config().getLimitForPeriod()), ctx,
                new RateLimitMetrics() {
                    @Override
                    public int numberOfWaitingThreads() {
                        return limiter.metrics().numberOfWaitingThreads();
                    }

                    @Override
                    public int availablePermissions() {
                        return limiter.metrics().availablePermissions();
                    }
                });
    }

    private void preDestroy() {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing to destroy the rateLimit moat: {}", limiter.name());
        }
        REGISTRY.unRegister(limiter.name());
        preDestroy0();
        shouldDestroy.getAndSet(true);
    }

    private void doReset() {
        updateWithNewestConfig(limiter.immutableConfig());
    }
}
