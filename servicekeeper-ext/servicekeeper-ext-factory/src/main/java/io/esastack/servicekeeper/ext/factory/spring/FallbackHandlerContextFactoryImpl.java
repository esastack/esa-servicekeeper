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
package io.esastack.servicekeeper.ext.factory.spring;

import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.factory.FallbackHandlerFactoryImpl;
import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.fallback.FallbackToException;
import io.esastack.servicekeeper.core.utils.LogUtils;
import io.esastack.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.springframework.context.ApplicationContext;

/**
 * Obtains {@link FallbackHandler} from {@link ApplicationContext} firstly, if fails then gets it by reflection
 * as fallback.
 */
public class FallbackHandlerContextFactoryImpl extends FallbackHandlerFactoryImpl {

    private static final Logger logger = LogUtils.logger();

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected FallbackToException doCreate(Class<? extends Exception> exception, FallbackConfig config) {
        final Exception bean = SpringContextUtils.getBean(exception);
        if (bean != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Got fallback exception: {} from application context successfully",
                        exception.getName());
            }
            return new FallbackToException(bean, config.isAlsoApplyToBizException());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Failed to get fallback exception: {} from application context, try to get it by reflection"
                    + " again", exception.getName());
        }
        return super.doCreate(exception, config);
    }

    @Override
    protected Object newInstance(Class<?> clazz) {
        final Object bean = SpringContextUtils.getBean(clazz);
        if (bean != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Got fallback bean: {} from application context successfully", clazz.getName());
            }
            return bean;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Failed to get fallback bean: {} from application context, try to get it by reflection again",
                    clazz.getName());
        }
        return super.newInstance(clazz);
    }

}
