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
package esa.servicekeeper.adapter.spring.aop;

import esa.servicekeeper.core.Bootstrap;
import esa.servicekeeper.core.BootstrapContext;
import esa.servicekeeper.core.annotation.CircuitBreaker;
import esa.servicekeeper.core.annotation.ConcurrentLimiter;
import esa.servicekeeper.core.annotation.EnableServiceKeeper;
import esa.servicekeeper.core.annotation.RateLimiter;
import esa.servicekeeper.core.annotation.Retryable;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.exception.ServiceKeeperWrapException;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import esa.commons.logging.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The abstract aop of service keeper, the method which annotated with {@link ConcurrentLimiter},
 * or {@link RateLimiter}, or {@link CircuitBreaker}, or {@link Retryable}, or {@link EnableServiceKeeper}
 * will be proxied and the original invocation will be wrapped with service keeper,
 * see {@link #doInvoke(ProceedingJoinPoint)} for more details. When you want to define a custom aop for
 * using service keeper, you'd better extend from this class and define your own pointcuts,
 * see {@link WebAutoSupportAop}, {@link DefaultServiceKeeperAop}.
 */
public abstract class AbstractServiceKeeperAop implements Ordered, InitializingBean {

    protected static final Logger logger = LogUtils.logger();

    private int order = LOWEST_PRECEDENCE;

    @Autowired(required = false)
    protected List<AsyncResultHandler<?>> asyncResultHandlers;

    @Override
    public void afterPropertiesSet() {
        Bootstrap.init(BootstrapContext.singleton(asyncResultHandlers == null
                ? Collections.emptyList() : Collections.unmodifiableList(asyncResultHandlers)));
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.ConcurrentLimiter)")
    protected void concurrentLimit() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.RateLimiter)")
    protected void rateLimit() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.CircuitBreaker)")
    protected void circuitBreaker() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.EnableServiceKeeper)")
    protected void enableServiceKeeper() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.Retryable)")
    protected void retry() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.Fallback)")
    protected void fallback() {
    }

    @Pointcut("@annotation(esa.servicekeeper.core.annotation.Group)")
    protected void group() {
    }

    protected final String getQualifiedName(ProceedingJoinPoint pjp) {
        final Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    protected final Object doInvoke(ProceedingJoinPoint pjp) throws Throwable {
        final Callable<Object> callable = () -> {
            try {
                return pjp.proceed();
            } catch (Exception ex) {
                throw ex;
            } catch (Throwable throwable) {
                // Ignore the error.
                throw new ServiceKeeperWrapException(throwable);
            }
        };

        final Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        return Bootstrap.entry().call(MethodUtils.getMethodAlias(method),
                () -> MethodUtils.getCompositeConfig(method),
                () -> new OriginalInvocation(pjp.getTarget(), method),
                callable, pjp.getArgs());
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Set aop order
     *
     * @param order order
     */
    public void setOrder(int order) {
        this.order = order;
    }
}
