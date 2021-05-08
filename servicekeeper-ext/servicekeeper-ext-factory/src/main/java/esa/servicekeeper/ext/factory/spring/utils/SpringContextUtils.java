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
package esa.servicekeeper.ext.factory.spring.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Collection;

/**
 * The utils to get bean from spring context. The static method will only be successful accessed after the context has
 * been initialized.
 */
public class SpringContextUtils implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    public static <T> T getBean(Class<T> requiredType) {
        if (context == null) {
            throw new IllegalStateException("Illegal to get bean from context which has not been initialized!");
        }

        T bean = null;
        try {
            bean = context.getBean(requiredType);
        } catch (Exception ignored) {
        }
        return bean;
    }

    public static <T> Collection<T> getBeans(Class<T> requiredType) {
        if (context == null) {
            throw new IllegalStateException("Illegal to get bean from internal which has not been initialized!");
        }

        Collection<T> beans = null;
        try {
            beans = context.getBeansOfType(requiredType).values();
        } catch (Exception ignored) {
        }
        return beans;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

}
