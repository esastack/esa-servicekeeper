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
package esa.servicekeeper.core.factory;

import esa.servicekeeper.core.common.LimitableKey;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.fallback.FallbackHandlerConfig;
import esa.servicekeeper.core.internal.MoatCreationLimit;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.moats.MoatType;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

abstract class LimitableMoatFactory<CNF, M> extends AbstractMoatFactory<CNF, M> {

    private static final Logger logger = LogUtils.logger();

    private final MoatCreationLimit limiter;
    private final LimitableMoatFactoryContext context;

    LimitableMoatFactory(LimitableMoatFactoryContext context) {
        super(context);
        this.context = context;
        this.limiter = context.limit();
    }

    @Override
    public final M doCreate(ResourceId id, OriginalInvocation invocation,
                            CNF compositeConfig, CNF immutableConfig) {
        if (limiter.canCreate(buildKey(id))) {
            return doCreate0(id, invocation, compositeConfig, immutableConfig);
        } else {
            return handleLimited(id, invocation, compositeConfig, immutableConfig);
        }
    }

    @Override
    protected LimitableMoatFactoryContext context() {
        return context;
    }

    private M handleLimited(ResourceId id, OriginalInvocation config1,
                            CNF config2, CNF immutableConfig2) {
        logger.info("The moat of {} is null(due to not permitted), config: {}; immutable config: {}",
                id, config2, immutableConfig2);

        return null;
    }

    /**
     * Build {@link LimitableKey}.
     *
     * @param id resourceId
     * @return key
     */
    private LimitableKey buildKey(ResourceId id) {
        return new LimitableKey(id, getType());
    }

    /**
     * Get {@link MoatType}.
     *
     * @return type
     */
    protected abstract MoatType getType();

    /**
     * Create instance by supplied all.
     *
     * @param id               resourceId
     * @param config1          config1
     * @param config2          config2
     * @param immutableConfig2 immutable config
     * @return instance
     */
    protected abstract M doCreate0(ResourceId id, OriginalInvocation config1,
                                   CNF config2, CNF immutableConfig2);

    MoatConfig buildConfig(ResourceId id, OriginalInvocation config1) {
        return new MoatConfig(id);
    }

    static class LimitableConcurrentMoatFactory extends
            LimitableMoatFactory<ConcurrentLimitConfig, ConcurrentLimitMoat> {

        LimitableConcurrentMoatFactory(LimitableMoatFactoryContext context) {
            super(context);
        }

        @Override
        protected ConcurrentLimitMoat doCreate0(ResourceId id,
                                                OriginalInvocation config1, ConcurrentLimitConfig config2,
                                                ConcurrentLimitConfig immutableConfig) {
            List<MoatEventProcessor> processors = new ArrayList<>(1);
            for (EventProcessorFactory factory : context().processors()) {
                MoatEventProcessor processor = factory.concurrentLimit(id);
                if (processor != null) {
                    processors.add(processor);
                }
            }

            final ConcurrentLimitMoat moat = new ConcurrentLimitMoat(buildConfig(id, config1), config2,
                    immutableConfig, processors);
            logger.info("Created concurrent limiter moat successfully, resourceId: {}," +
                    " config: {}, immutable config: {}", id, config2, immutableConfig);

            processors.forEach(processor -> processor.onInitialization(moat));
            return moat;
        }

        @Override
        protected MoatType getType() {
            return MoatType.CONCURRENT_LIMIT;
        }
    }

    static class LimitableRateMoatFactory extends LimitableMoatFactory<RateLimitConfig, RateLimitMoat> {

        LimitableRateMoatFactory(LimitableMoatFactoryContext context) {
            super(context);
        }

        @Override
        protected RateLimitMoat doCreate0(ResourceId id, OriginalInvocation config1,
                                          RateLimitConfig config2, RateLimitConfig immutableConfig) {
            List<MoatEventProcessor> processors = new ArrayList<>(1);
            for (EventProcessorFactory factory : context().processors()) {
                MoatEventProcessor processor = factory.rateLimit(id);
                if (processor != null) {
                    processors.add(processor);
                }
            }

            final RateLimitMoat moat = new RateLimitMoat(buildConfig(id, config1), config2,
                    immutableConfig, processors);
            logger.info("Created rate limiter moat successfully, resourceId: {}," +
                    " config: {}, immutable config: {}", id, config2, immutableConfig);

            processors.forEach(processor -> processor.onInitialization(moat));
            return moat;
        }

        @Override
        protected MoatType getType() {
            return MoatType.RATE_LIMIT;
        }
    }

    static class LimitableCircuitBreakerMoatFactory extends
            LimitableMoatFactory<CircuitBreakerConfig, CircuitBreakerMoat> {

        LimitableCircuitBreakerMoatFactory(LimitableMoatFactoryContext context) {
            super(context);
        }

        @Override
        protected CircuitBreakerMoat doCreate0(ResourceId id,
                                               OriginalInvocation config1,
                                               CircuitBreakerConfig config2, CircuitBreakerConfig immutableConfig) {
            final PredicateStrategy predicateStrategy = context().strategy().get(
                    PredicateStrategyConfig.from(id, config2, immutableConfig));

            List<MoatEventProcessor> processors = new ArrayList<>(1);
            for (EventProcessorFactory factory : context().processors()) {
                MoatEventProcessor processor = factory.circuitBreaker(id);
                if (processor != null) {
                    processors.add(processor);
                }
            }

            final CircuitBreakerMoat moat = new CircuitBreakerMoat(buildConfig(id, config1),
                    config2, immutableConfig, predicateStrategy, processors, context().cProcessors().all());
            logger.info("Created circuit breaker moat successfully, resourceId: {}," +
                    " config: {}, immutable config: {}", id, config2, immutableConfig);

            processors.forEach(processor -> processor.onInitialization(moat));
            return moat;
        }

        @Override
        protected MoatType getType() {
            return MoatType.CIRCUIT_BREAKER;
        }
    }

}

