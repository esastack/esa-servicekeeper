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
package io.esastack.servicekeeper.core.fallback;

import esa.commons.StringUtils;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import io.esastack.servicekeeper.core.exception.ConcurrentOverflowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.exception.ServiceKeeperException;
import io.esastack.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import io.esastack.servicekeeper.core.exception.ServiceRetryException;
import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.esastack.servicekeeper.core.fallback.FallbackHandler.FallbackType.FALLBACK_TO_FUNCTION;

/**
 * The class is designed to fallback original invocation to a specified function and cause of fallback is also
 * used to detect fallback method, which means you can defined many methods any the we will find the best match one
 * to handle fallback, eg:
 *
 * <pre>
 *     {@code
 *         public class CustomFallback {
 *              public String fallback(CircuitBreakerNotPermitException ex) {
 *
 *              }
 *
 *             public String fallback(RateLimitOverflowException ex) {
 *
 *             }
 *
 *             public String fallback() {
 *
 *             }
 *         }
 *     }
 * </pre>
 * <p>
 * As show above, when the rejection is caused by {@link CircuitBreakerNotPermittedException} the method first method
 * will be used to handle fallback, and when the rejection is caused by {@link RateLimitOverflowException} the second
 * method will be used. In other scenes, the last one will be used.
 */
public class FallbackToFunction<R> implements FallbackHandler<R> {

    private static final Logger logger = LogUtils.logger();

    private final Object obj;
    private final Set<FallbackMethod> fallbackMethods;
    private final Map<CauseType, FallbackMethod> fallbackMethodMap;
    private final boolean alsoApplyToBizException;

    public FallbackToFunction(Object obj, Set<FallbackMethod> fallbackMethods,
                              boolean alsoApplyToBizException) {
        this.obj = obj;
        this.fallbackMethods = fallbackMethods;
        this.fallbackMethodMap = initFallbackMethodMap();
        this.alsoApplyToBizException = alsoApplyToBizException;
    }

    @Override
    @SuppressWarnings("unchecked")
    public R handle(Context ctx) throws Throwable {
        //ThroughFailsCause and bizException never exist meanwhile
        Throwable th = ctx.getNotPermittedCause();
        if (th == null) {
            th = ctx.getBizException();
        }

        final FallbackMethod fallbackMethod = matchingMethod(th);

        if (fallbackMethod == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Couldn't find method to handle exception: "
                        + th.getMessage());
            }
            throw th;
        }

        final Method method = fallbackMethod.getMethod();
        final Object object = fallbackMethod.isStatic() ? null : obj;

        try {
            if (logger.isDebugEnabled()) {
                logger.debug(StringUtils.concat(ctx.getResourceId(),
                        " fallback to function, fallbackClass: ",
                        object == null ? method.getDeclaringClass().getName() : object.getClass().getName(),
                        ", and fallbackMethod: ", method.getName()));
            }
            R result;
            if (fallbackMethod.isCauseAtFirst()) {
                if (fallbackMethod.isMatchFullArgs()) {
                    result = (R) method.invoke(object,
                            combiningFailsCause(th, ctx.getArgs()));
                } else {
                    result = (R) method.invoke(object, new Object[]{th});
                }
            } else {
                if (fallbackMethod.isMatchFullArgs()) {
                    result = (R) method.invoke(object, ctx.getArgs());
                } else {
                    result = (R) method.invoke(object, new Object[0]);
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug(ctx.getResourceId() + " fallback return value: " + result);
            }
            return result;
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    @Override
    public FallbackType getType() {
        return FALLBACK_TO_FUNCTION;
    }

    @Override
    public boolean alsoApplyToBizException() {
        return alsoApplyToBizException;
    }

    @Override
    public String toString() {
        return "FallbackToFunction{" +
                "obj=" + (obj == null ? "null" : obj.getClass().getName()) +
                ", fallbackMethods=" + fallbackMethods +
                ", alsoApplyToBizException=" + alsoApplyToBizException +
                '}';
    }

    private FallbackMethod matchingMethod(final Throwable th) {
        if (th == null) {
            return fallbackMethodMap.get(CauseType.UNKNOWN);
        }

        if (th instanceof CircuitBreakerNotPermittedException) {
            return fallbackMethodMap.get(CauseType.CIRCUIT_BREAKER);
        }
        if (th instanceof RateLimitOverflowException) {
            return fallbackMethodMap.get(CauseType.RATE_LIMIT);
        }
        if (th instanceof ConcurrentOverflowException) {
            return fallbackMethodMap.get(CauseType.CONCURRENT_LIMIT);
        }
        if (th instanceof ServiceRetryException) {
            return fallbackMethodMap.get(CauseType.RETRY);
        }
        if (th instanceof ServiceKeeperNotPermittedException) {
            return fallbackMethodMap.get(CauseType.SERVICE_KEEPER_NOT_PERMIT);
        }
        if (th instanceof ServiceKeeperException) {
            return fallbackMethodMap.get(CauseType.SERVICE_KEEPER);
        }

        return fallbackMethodMap.get(CauseType.BIZ);
    }

    private Map<CauseType, FallbackMethod> initFallbackMethodMap() {
        final Map<CauseType, FallbackMethod> fallbackMethodMap = new HashMap<>(7);

        /*
         * The rule to decide the order of many fallback methods are:
         * 1. the matchFullArgs has higher order than not
         * 2. the closer exception has higher order than others
         *
         */
        for (CauseType type : CauseType.values()) {
            for (FallbackMethod method : fallbackMethods) {
                if (!method.canApplyTo(type)) {
                    continue;
                }
                final FallbackMethod pre = fallbackMethodMap.putIfAbsent(type, method);
                if (pre == null) {
                    continue;
                }

                if (pre.isMatchFullArgs() && !method.isMatchFullArgs()) {
                    continue;
                }

                if (!pre.isMatchFullArgs() && method.isMatchFullArgs()) {
                    fallbackMethodMap.put(type, method);
                    continue;
                }

                // Pre and current methods are all match full args or neither not
                // to compare order by exception
                final boolean moreCloser = method.isCauseAtFirst() &&
                        (!pre.isCauseAtFirst() ||
                                pre.getMethod().getParameterTypes()[0]
                                        .isAssignableFrom(method.getMethod().getParameterTypes()[0]));
                if (moreCloser) {
                    fallbackMethodMap.put(type, method);
                }
            }
        }

        return fallbackMethodMap;
    }

    private Object[] combiningFailsCause(Throwable th, Object[] realArgs) {
        Object[] combinedArgs = new Object[realArgs == null ? 1 : realArgs.length + 1];

        combinedArgs[0] = th;
        if (combinedArgs.length != 1) {
            for (int i = 0, length = realArgs.length; i < length; i++) {
                combinedArgs[i + 1] = realArgs[i];
            }
        }
        return combinedArgs;
    }

}
