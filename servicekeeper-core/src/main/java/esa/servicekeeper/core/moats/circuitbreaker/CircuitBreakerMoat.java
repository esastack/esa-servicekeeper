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
package esa.servicekeeper.core.moats.circuitbreaker;

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.listener.FondConfigListener;
import esa.servicekeeper.core.metrics.CircuitBreakerMetrics;
import esa.servicekeeper.core.moats.AbstractMoat;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.moats.MoatEventImpl;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.moats.MoatType;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateConfigFilling;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.TimerLogger;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static esa.servicekeeper.core.configsource.ExternalConfigUtils.hasCircuitBreaker;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.PERMANENT;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.TEMPORARY;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_DISABLED;
import static esa.servicekeeper.core.moats.circuitbreaker.CircuitBreaker.State.FORCED_OPEN;
import static esa.servicekeeper.core.utils.ConfigUtils.combine;

public class CircuitBreakerMoat extends AbstractMoat<CircuitBreakerConfig>
        implements FondConfigListener<CircuitBreakerConfig>, LifeCycleSupport {

    private static final Logger logger = LogUtils.logger();

    private static final CircuitBreakerRegistry REGISTRY = CircuitBreakerRegistry.singleton();

    private final TimerLogger timerLogger = new TimerLogger();
    private final AtomicBoolean shouldDestroy = new AtomicBoolean(false);
    private final LifeCycleType lifeCycleType;
    private final AtomicReference<CircuitBreaker> breaker;
    private final PredicateStrategy predicate;
    private final List<CircuitBreakerSateTransitionProcessor> processors;

    /**
     * Designed for unit test purpose.
     */
    public CircuitBreakerMoat(MoatConfig config, CircuitBreakerConfig breakerConfig,
                              CircuitBreakerConfig immutableConfig, PredicateStrategy predicate) {
        this(config, breakerConfig, immutableConfig, predicate, null, null);
    }

    public CircuitBreakerMoat(MoatConfig config, CircuitBreakerConfig breakerConfig,
                              CircuitBreakerConfig immutableConfig, PredicateStrategy predicate,
                              List<MoatEventProcessor> mProcessors,
                              List<CircuitBreakerSateTransitionProcessor> processors) {
        super(mProcessors, config);
        Checks.checkNotNull(predicate, "predicate");
        this.lifeCycleType = immutableConfig == null ? TEMPORARY : PERMANENT;
        this.breaker = new AtomicReference<>(REGISTRY.getOrCreate(config.getResourceId().getName(),
                breakerConfig, immutableConfig, processors));
        this.predicate = predicate;
        this.processors = processors;
    }

    @Override
    public void tryThrough(Context ctx) throws ServiceKeeperNotPermittedException {
        final CircuitBreaker breaker = this.breaker.get();
        if (!hasProcessors) {
            if (!breaker.isCallPermitted()) {
                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **

                timerLogger.logPeriodically("The circuitBreaker doesn't permit request" +
                        " to through, which name is {} and current state is {}", breaker.name(), breaker.getState());
                throw notPermittedException(ctx);
            }
        } else {
            if (breaker.isCallPermitted()) {
                process(MoatEventImpl.PERMITTED);
            } else {
                process(MoatEventImpl.REJECTED_BY_CIRCUIT_BREAKER);

                // ***  Note: Mustn't modify the log content which is used for keyword alarms.  **
                timerLogger.logPeriodically("The circuitBreaker doesn't permit request" +
                        " to through, which name is {} and current state is {}", breaker.name(), breaker.getState());
                throw notPermittedException(ctx);
            }
        }
    }

    @Override
    public void exit(Context ctx) {
        if (predicate.isSuccess(ctx)) {
            breaker.get().onSuccess();
        } else {
            breaker.get().onFailure();
        }
    }

    @Override
    public ResourceId listeningKey() {
        return ResourceId.from(breaker.get().name());
    }

    @Override
    public CircuitBreakerConfig config() {
        CircuitBreakerConfig config = breaker.get().config();
        if (predicate instanceof PredicateConfigFilling) {
            ((PredicateConfigFilling) predicate).fill(config);
        }
        return config;
    }

    @Override
    public CircuitBreakerConfig getFond(ExternalConfig config) {
        if (breaker.get().immutableConfig() == null && (!hasCircuitBreaker(config))) {
            return null;
        }

        return combine(breaker.get().immutableConfig(), config);
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        final String name = breaker.get().name();
        if (lifeCycleType() == TEMPORARY) {
            if (logger.isDebugEnabled()) {
                logger.debug("The {}'s newest failureRateThreshold got from dynamic config is null" +
                        " and it's a temporary moat, so it will be destroyed soon", name);
            }
            preDestroy();
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("The {}'s newest failureRateThreshold got from dynamic config is null " +
                        "and it's a permanent moat, so it will be reset with original failureRateThreshold", name);
            }
            doReset();
        }
    }

    @Override
    public boolean isConfigEquals(CircuitBreakerConfig newestConfig) {
        /*
         * Whether the new failureRateThreshold equals the older one
         *
         * The logical to predicate whether the newestConfig has been updated is: If all the concerned config is not
         * updated, it will return true, otherwise return false.
         *
         * All concerned config not updated contains:
         * 1. failureRateThreshold doesn't onUpdate
         *
         * 2. forcedOpen doesn't onUpdate: If the circuitBreaker's current state is FORCED_OPEN that means pre
         *    forcedOpen is true, so the current forcedOpen is true suggested the forcedOpen does't onUpdate; If the
         *    circuitBreaker's current state is not FORCED_OPEN that means the pre forceOpen is not true, so the
         *    current forcedOpen is false too suggested the forcedOpen does't onUpdate.
         *
         * 3. forcedDisable doesn't onUpdate: the logical is same as part2.
         *
         */
        final CircuitBreaker breaker = this.breaker.get();

        boolean stateEquals = true;
        if (breaker.getState().equals(FORCED_OPEN)) {
            if (!FORCED_OPEN.equals(newestConfig.getState())) {
                stateEquals = false;
            }
        }

        if (breaker.getState().equals(FORCED_DISABLED)) {
            if (!FORCED_DISABLED.equals(newestConfig.getState())) {
                stateEquals = false;
            }
        }

        return stateEquals && breaker.config().equals(newestConfig);
    }

    @Override
    public void updateWithNewestConfig(CircuitBreakerConfig config) {
        final String name = breaker.get().name();
        final CircuitBreaker breaker = this.breaker.get();

        if (!breaker.getState().equals(FORCED_OPEN) && FORCED_OPEN.equals(config.getState())) {
            logger.info("The circuitBreaker: {} will transition to forced_open state", name);
            this.breaker.getAndUpdate(pre -> {
                pre.forceToForcedOpenState();
                return pre;
            });
            shouldDestroy.getAndSet(false);
        }

        if (!breaker.getState().equals(FORCED_DISABLED) && FORCED_DISABLED.equals(config.getState())) {
            logger.info("The circuitBreaker: {} will transition to forced_disabled state", name);
            this.breaker.getAndUpdate(pre -> {
                pre.forceToDisabledState();
                return pre;
            });
            shouldDestroy.getAndSet(false);
        }

        logger.info("Begin to update circuit breaker {} with the newest config: {}", name, config);
        REGISTRY.unRegister(name);

        this.breaker.getAndUpdate(pre -> REGISTRY.getOrCreate(pre.name(), config, pre.immutableConfig(),
                processors));
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
    public int getOrder() {
        return 1;
    }

    @Override
    public MoatType type() {
        return MoatType.CIRCUIT_BREAKER;
    }

    @Override
    public String toString() {
        return "CircuitBreakerMoat-" + breaker.get().name();
    }

    @Override
    protected String name() {
        return getCircuitBreaker().name();
    }

    public PredicateStrategy getPredicate() {
        return predicate;
    }

    /**
     * Get the circuitBreaker of current moat
     *
     * @return circuitBreaker
     */
    public CircuitBreaker getCircuitBreaker() {
        return breaker.get();
    }

    private ServiceKeeperNotPermittedException notPermittedException(Context ctx) {
        final CircuitBreaker breaker = this.breaker.get();
        return new CircuitBreakerNotPermittedException(StringUtils.concat("Current state of" +
                " circuitBreaker ", breaker.name(), ": ", breaker.getState().toString()), ctx,
                new CircuitBreakerMetrics() {
                    @Override
                    public float failureRateThreshold() {
                        return breaker.metrics().failureRateThreshold();
                    }

                    @Override
                    public int numberOfBufferedCalls() {
                        return breaker.metrics().numberOfBufferedCalls();
                    }

                    @Override
                    public int numberOfFailedCalls() {
                        return breaker.metrics().numberOfFailedCalls();
                    }

                    @Override
                    public long numberOfNotPermittedCalls() {
                        return breaker.metrics().numberOfNotPermittedCalls();
                    }

                    @Override
                    public int maxNumberOfBufferedCalls() {
                        return breaker.metrics().maxNumberOfBufferedCalls();
                    }

                    @Override
                    public int numberOfSuccessfulCalls() {
                        return breaker.metrics().numberOfSuccessfulCalls();
                    }

                    @Override
                    public CircuitBreaker.State state() {
                        return breaker.getState();
                    }
                });
    }

    private void preDestroy() {
        if (logger.isDebugEnabled()) {
            logger.debug("Prepare to destroy the circuitBreaker moat: " + breaker.get().name());
        }
        REGISTRY.unRegister(breaker.get().name());
        preDestroy0();
        shouldDestroy.getAndSet(true);
    }

    private void doReset() {
        updateWithNewestConfig(breaker.get().immutableConfig());
    }
}
