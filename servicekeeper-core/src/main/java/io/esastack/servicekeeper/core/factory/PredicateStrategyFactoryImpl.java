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
package io.esastack.servicekeeper.core.factory;

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class PredicateStrategyFactoryImpl implements PredicateStrategyFactory {

    private static final Logger logger = LogUtils.logger();

    private final ConcurrentHashMap<PredicateStrategyConfig, PredicateStrategy> cachedPredicates =
            new ConcurrentHashMap<>(32);

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public PredicateStrategy get(PredicateStrategyConfig config) {
        final Class<? extends PredicateStrategy> clazz = config.getPredicate();
        Checks.checkNotNull(clazz, "clazz");
        if (isSingleton()) {
            return cachedPredicates.computeIfAbsent(config, key -> doCreate(config));
        } else {
            return doCreate(config);
        }
    }

    protected PredicateStrategy doCreate(PredicateStrategyConfig config) {
        final Class<? extends PredicateStrategy> clazz = config.getPredicate();
        if (clazz.equals(PredicateByException.class)) {
            return predicateByException(config.getIgnoreExceptions(), config.getOriginIgnoreExceptions(),
                    config.getName());
        } else if (clazz.equals(PredicateBySpendTime.class)) {
            return predicateBySpendTime(config.getMaxSpendTimeMs(), config.getOriginalMaxSpendTimeMs(),
                    config.getName());
        } else if (clazz.equals(PredicateByExceptionAndSpendTime.class)) {
            return predicateByExceptionAndSpendTime(config.getIgnoreExceptions(),
                    config.getMaxSpendTimeMs(), config.getOriginalMaxSpendTimeMs(),
                    config.getOriginIgnoreExceptions(), config.getName());
        } else {
            return doCreate0(config);
        }
    }

    /**
     * Produce a instance of predicateStrategy by refection.
     *
     * @param config config
     * @return instance
     */
    protected PredicateStrategy doCreate0(PredicateStrategyConfig config) {
        Class<? extends PredicateStrategy> clazz = config.getPredicate();
        try {
            final PredicateStrategy predicate = clazz.getDeclaredConstructor().newInstance();
            if (logger.isDebugEnabled()) {
                logger.debug("Created custom predicate strategy: {} by reflection successfully", clazz.getName());
            }
            return predicate;
        } catch (Exception ex) {
            throw new RuntimeException(StringUtils.concat("Failed to instantiate {}"
                    + " by reflection", clazz.getName()), ex);
        }
    }

    protected PredicateByException predicateByException(Class<?>[] ignoreExceptions,
                                                        Class<?>[] originIgnoreExceptions,
                                                        ResourceId name) {
        final Class<PredicateByException> clazz = PredicateByException.class;
        if (ignoreExceptions == null) {
            ignoreExceptions = new Class[0];
        }

        try {
            PredicateByException predicate = clazz.getDeclaredConstructor(Class[].class,
                    Class[].class, ResourceId.class).newInstance(ignoreExceptions, originIgnoreExceptions, name);
            if (logger.isDebugEnabled()) {
                logger.debug("Created predicateByException strategy by reflection successfully" +
                        " it's ignoreExceptions is {}", Arrays.toString(ignoreExceptions));
            }
            return predicate;
        } catch (Exception ex) {
            logger.error("Failed to Create PredicateByException strategy", ex);
            return null;
        }
    }

    protected PredicateBySpendTime predicateBySpendTime(long maxSpendTimeMs,
                                                        long originalMaxSpendTimeMs,
                                                        ResourceId name) {
        final Class<PredicateBySpendTime> clazz = PredicateBySpendTime.class;
        try {
            final PredicateBySpendTime predicate =
                    clazz.getDeclaredConstructor(long.class, long.class, ResourceId.class)
                            .newInstance(maxSpendTimeMs, originalMaxSpendTimeMs, name);
            if (logger.isDebugEnabled()) {
                logger.debug("Created predicateBySendTime strategy by reflection successfully, maxSpendTimeMs: {}",
                        maxSpendTimeMs);
            }
            return predicate;
        } catch (Exception ex) {
            logger.error("Failed to create PredicateBySpendTime strategy", ex);
            return null;
        }
    }

    protected PredicateByExceptionAndSpendTime predicateByExceptionAndSpendTime(Class<?>[] ignoreExceptions,
                                                                                long maxSpendTimeMs,
                                                                                long originalMaxSpendTimeMs,
                                                                                Class<?>[] originIgnoreExceptions,
                                                                                ResourceId name) {
        final Class<PredicateByExceptionAndSpendTime> clazz = PredicateByExceptionAndSpendTime.class;
        try {
            final PredicateByExceptionAndSpendTime predicate = clazz
                    .getDeclaredConstructor(PredicateByException.class, PredicateBySpendTime.class)
                    .newInstance(predicateByException(ignoreExceptions, originIgnoreExceptions, name),
                            predicateBySpendTime(maxSpendTimeMs, originalMaxSpendTimeMs, name));
            if (logger.isDebugEnabled()) {
                logger.debug("Created predicateByExceptionAndSpendTime strategy by reflection successfully," +
                                " maxSpendTimeMs: {}, ignoreExceptions: {}", maxSpendTimeMs,
                        Arrays.toString(ignoreExceptions));
            }
            return predicate;
        } catch (Exception ex) {
            logger.error("Failed to create PredicateByExceptionAndSpendTime strategy", ex);
            return null;
        }
    }
}
