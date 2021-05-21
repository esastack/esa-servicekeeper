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

import esa.commons.Checks;
import esa.commons.StringUtils;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

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
        return getOrCreate(config);
    }

    private PredicateStrategy getOrCreate(PredicateStrategyConfig config) {
        if (isSingleton()) {
            return cachedPredicates.computeIfAbsent(config, (key) -> doCreate(config));
        } else {
            return doCreate(config);
        }
    }

    private PredicateStrategy doCreate(PredicateStrategyConfig config) {
        final Class<? extends PredicateStrategy> clazz = config.getPredicate();
        if (clazz.equals(PredicateByException.class)) {
            return doCreate0(config.getIgnoreExceptions(), config.getOriginIgnoreExceptions(), config.getName());
        } else if (clazz.equals(PredicateBySpendTime.class)) {
            return doCreate0(config.getMaxSpendTimeMs(), config.getOriginalMaxSpendTimeMs(),
                    config.getName());
        } else if (clazz.equals(PredicateByExceptionAndSpendTime.class)) {
            return doCreate0(config.getIgnoreExceptions(),
                    config.getMaxSpendTimeMs(), config.getOriginalMaxSpendTimeMs(),
                    config.getOriginIgnoreExceptions(), config.getName());
        } else {
            return doCreate0(clazz);
        }
    }

    /**
     * Produce a instance of predicateStrategy by refection.
     *
     * @param clazz clazz
     * @return instance
     */
    protected PredicateStrategy doCreate0(Class<? extends PredicateStrategy> clazz) {
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

    private PredicateByException doCreate0(Class<?>[] ignoreExceptions,
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

    private PredicateBySpendTime doCreate0(long maxSpendTimeMs, long originalMaxSpendTimeMs,
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

    private PredicateByExceptionAndSpendTime doCreate0(Class<?>[] ignoreExceptions,
                                                       long maxSpendTimeMs,
                                                       long originalMaxSpendTimeMs,
                                                       Class<?>[] originIgnoreExceptions,
                                                       ResourceId name) {
        final Class<PredicateByExceptionAndSpendTime> clazz = PredicateByExceptionAndSpendTime.class;
        try {
            final PredicateByExceptionAndSpendTime predicate = clazz
                    .getDeclaredConstructor(PredicateByException.class, PredicateBySpendTime.class)
                    .newInstance(doCreate0(ignoreExceptions, originIgnoreExceptions, name),
                            doCreate0(maxSpendTimeMs, originalMaxSpendTimeMs, name));
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
