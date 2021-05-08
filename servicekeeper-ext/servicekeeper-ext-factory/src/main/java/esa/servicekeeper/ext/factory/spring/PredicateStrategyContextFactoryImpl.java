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
package esa.servicekeeper.ext.factory.spring;

import esa.servicekeeper.core.factory.PredicateStrategyFactoryImpl;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Obtains {@link PredicateStrategy} from {@link ApplicationContext} and by reflection orderly.
 */
public class PredicateStrategyContextFactoryImpl extends PredicateStrategyFactoryImpl {

    private static final Logger logger = LogUtils.logger();

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected PredicateStrategy doCreate0(Class<? extends PredicateStrategy> clazz) {
        final PredicateStrategy strategy = SpringContextUtils.getBean(clazz);
        if (strategy != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Got custom predicate strategy: {} from application context successfully",
                        clazz.getName());
            }
            return strategy;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Failed to get custom predicate strategy: {} from application context," +
                    " try to get it by reflection again", clazz.getName());
        }
        return super.doCreate0(clazz);
    }

}
