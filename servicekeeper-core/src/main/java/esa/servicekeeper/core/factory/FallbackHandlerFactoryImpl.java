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

import esa.commons.StringUtils;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.exception.FallbackFailsException;
import esa.servicekeeper.core.exception.ServiceKeeperException;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.fallback.FallbackHandlerConfig;
import esa.servicekeeper.core.fallback.FallbackMethod;
import esa.servicekeeper.core.fallback.FallbackToException;
import esa.servicekeeper.core.fallback.FallbackToFunction;
import esa.servicekeeper.core.fallback.FallbackToValue;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FallbackHandlerFactoryImpl implements FallbackHandlerFactory {

    private static final Logger logger = LogUtils.logger();

    private final Map<FallbackHandlerConfig, FallbackHandler<?>> cachedHandlers =
            new ConcurrentHashMap<>(32);

    private final Map<Class<?>, Object> cachedInstances = new ConcurrentHashMap<>(8);

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    @Override
    public FallbackHandler<?> get(FallbackHandlerConfig config) {
        try {
            return cachedHandlers.computeIfAbsent(config, this::doCreate);
        } catch (FallbackFailsException ex) {
            logger.error("Failed to create fallback handler", ex);
            return null;
        }
    }

    private FallbackHandler<?> doCreate(FallbackHandlerConfig config) {
        final FallbackConfig fallbackConfig = config.getFallbackConfig();
        final OriginalInvocation invocation = config.getOriginalInvocation();

        final Class<?> returnType = invocation == null ? null : invocation.getReturnType();
        final Class<?>[] parameterTypes = invocation == null
                ? null : invocation.getParameterTypes();

        // Firstly: Try to create fallback function
        final FallbackToFunction<?> function = doCreate(fallbackConfig.getMethodName(),
                fallbackConfig.getTargetClass(), returnType,
                parameterTypes);
        if (function != null) {
            logger.info("Created fallback function handler successfully, config: {}", fallbackConfig);
            return function;
        }

        // Note: fallbackToValue is allowed only when original invocation's return value is String
        final boolean canFallbackToValue = returnType == null || returnType.isAssignableFrom(String.class);

        // Secondly: Try to create fallback to value
        if ((canFallbackToValue) && StringUtils.isNotEmpty(fallbackConfig.getSpecifiedValue())) {
            logger.info("Created fallback value handler successfully, config: {}", fallbackConfig);
            return new FallbackToValue(fallbackConfig.getSpecifiedValue());
        }

        // Finally: Try to crete fallback to exception
        return doCreate(fallbackConfig.getSpecifiedException(), fallbackConfig);
    }

    /**
     * Try to create {@link FallbackToFunction}
     *
     * @param methodName  method's name
     * @param targetClass target class
     */
    protected FallbackToFunction<?> doCreate(final String methodName,
                                             Class<?> targetClass,
                                             Class<?> originalReturnType,
                                             Class<?>[] originalParameterTypes) {
        if (StringUtils.isEmpty(methodName) || targetClass == null) {
            return null;
        }

        final Set<FallbackMethod> fallbackMethods = new HashSet<>(1);
        matchingMethods(targetClass, methodName, originalReturnType,
                originalParameterTypes, fallbackMethods);
        if (fallbackMethods.isEmpty()) {
            throw new FallbackFailsException(StringUtils.concat("Failed to find method: [",
                    methodName, "] in class: ", targetClass.getName(), ", target method's return type is: ",
                    originalReturnType == null ? "null" : originalReturnType.getName(),
                    ", parameterTypes are: ", originalParameterTypes == null
                            ? "null" : Arrays.toString(originalParameterTypes)));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Found fallback method: [" + methodName + "] in class: " + targetClass.getName()
                    + " successfully");
        }

        final boolean allStatic = fallbackMethods.stream().allMatch(FallbackMethod::isStatic);

        if (allStatic) {
            return new FallbackToFunction<>(null, fallbackMethods);
        } else {
            return new FallbackToFunction<>(getOrNewInstance(targetClass), fallbackMethods);
        }
    }

    /**
     * Try to get fallbackToException
     *
     * @param exception exception Class
     * @param config    config
     * @return FallbackToException
     */
    protected FallbackToException doCreate(Class<? extends Exception> exception, FallbackConfig config) {
        if (exception == null) {
            return null;
        }

        FallbackToException fallback = new FallbackToException((Exception) getOrNewInstance(exception));
        logger.info("Created fallback exception handler successfully, config: {}", config);
        return fallback;
    }

    /**
     * Get or doCreate a instance by target clazz.
     *
     * @param clazz clazz
     * @return instance
     */
    private Object getOrNewInstance(Class<?> clazz) {
        if (isSingleton()) {
            return cachedInstances.computeIfAbsent(clazz, this::newInstance);
        } else {
            return newInstance(clazz);
        }
    }

    /**
     * Get a instance of target clazz.
     *
     * @param clazz class
     * @return instance
     */
    protected Object newInstance(Class<?> clazz) {
        try {
            Object obj = clazz.getDeclaredConstructor().newInstance();
            logger.info("Instantiated {} by reflection successfully", clazz.getName());
            return obj;
        } catch (Exception ex) {
            throw new FallbackFailsException(StringUtils.concat("Failed to instantiate ",
                    clazz.getName()), ex);
        }
    }

    /**
     * Detect method from targetClass and it's super class, save detect result in matchedMethods.
     *
     * @param targetClass            targetClass
     * @param methodName             method's name
     * @param originalReturnType     original returnType
     * @param originalParameterTypes original parameterTypes
     * @param matchedMethods         fallback methods
     * @return found methods
     */
    private Set<FallbackMethod> matchingMethods(Class<?> targetClass, String methodName,
                                                Class<?> originalReturnType,
                                                Class<?>[] originalParameterTypes,
                                                final Set<FallbackMethod> matchedMethods) {
        boolean checkReturnType = originalReturnType != null;
        boolean checkParameterTypes = originalParameterTypes != null;

        for (Method method : targetClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (checkReturnType && !originalReturnType.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            if (checkParameterTypes && !isParamMatch(originalParameterTypes, method.getParameterTypes())) {
                continue;
            }

            if (!matchedMethods.add(new FallbackMethod(method))) {
                throw new FallbackFailsException("Duplicate fallback methods [" + methodName + "] in class: "
                        + targetClass);
            }
        }

        // Detect in the super class recursively.
        Class<?> superClass = targetClass.getSuperclass();
        if (superClass != null && !Object.class.equals(superClass)) {
            return matchingMethods(superClass, methodName, originalReturnType, originalParameterTypes, matchedMethods);
        } else {
            return matchedMethods;
        }
    }

    private boolean isParamMatch(Class<?>[] originalParameterTypes, Class<?>[] targetParameterTypes) {
        if (isCauseAtFirst(targetParameterTypes)) {
            return targetParameterTypes.length == 1 || Arrays.equals(originalParameterTypes,
                    Arrays.copyOfRange(targetParameterTypes, 1, targetParameterTypes.length));
        } else {
            return targetParameterTypes.length == 0 || Arrays.equals(originalParameterTypes, targetParameterTypes);
        }
    }

    private boolean isCauseAtFirst(Class<?>[] targetParameterTypes) {
        return targetParameterTypes.length > 0 &&
                ServiceKeeperException.class.isAssignableFrom(targetParameterTypes[0]);
    }
}

