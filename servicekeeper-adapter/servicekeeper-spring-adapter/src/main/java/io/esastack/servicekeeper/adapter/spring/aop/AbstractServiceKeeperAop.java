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
package io.esastack.servicekeeper.adapter.spring.aop;

import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.Bootstrap;
import io.esastack.servicekeeper.core.BootstrapContext;
import io.esastack.servicekeeper.core.annotation.CircuitBreaker;
import io.esastack.servicekeeper.core.annotation.ConcurrentLimiter;
import io.esastack.servicekeeper.core.annotation.EnableServiceKeeper;
import io.esastack.servicekeeper.core.annotation.RateLimiter;
import io.esastack.servicekeeper.core.annotation.Retryable;
import io.esastack.servicekeeper.core.asynchandle.AsyncResultHandler;
import io.esastack.servicekeeper.core.common.OriginalInvocation;
import io.esastack.servicekeeper.core.exception.ServiceKeeperWrapException;
import io.esastack.servicekeeper.core.utils.LogUtils;
import io.esastack.servicekeeper.core.utils.MethodUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.ConcurrentLimiter)")
    protected void concurrentLimit() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.RateLimiter)")
    protected void rateLimit() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.CircuitBreaker)")
    protected void circuitBreaker() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.EnableServiceKeeper)")
    protected void enableServiceKeeper() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.Retryable)")
    protected void retry() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.Fallback)")
    protected void fallback() {
    }

    @Pointcut("@annotation(io.esastack.servicekeeper.core.annotation.Group)")
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
