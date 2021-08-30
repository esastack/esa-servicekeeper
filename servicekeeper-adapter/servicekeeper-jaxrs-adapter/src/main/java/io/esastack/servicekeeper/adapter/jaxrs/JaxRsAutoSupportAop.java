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
package io.esastack.servicekeeper.adapter.jaxrs;

import io.esastack.servicekeeper.adapter.spring.aop.AbstractServiceKeeperAop;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class JaxRsAutoSupportAop extends AbstractServiceKeeperAop {

    @Pointcut("@annotation(javax.ws.rs.Path)")
    private void path() {
    }

    @Around("path() && !(concurrentLimit() || rateLimit() || circuitBreaker() ||" +
            " enableServiceKeeper() || retry() || group() || fallback())")
    public Object doInvoke0(ProceedingJoinPoint pjp) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("ServiceKeeper's Jax-RS(@Path) aop is surrounding method: {}",
                    getQualifiedName(pjp));
        }
        return super.doInvoke(pjp);
    }

}
