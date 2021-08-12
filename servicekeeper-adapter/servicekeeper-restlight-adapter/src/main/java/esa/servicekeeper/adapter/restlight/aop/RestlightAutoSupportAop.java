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
package esa.servicekeeper.adapter.restlight.aop;

import esa.servicekeeper.adapter.spring.aop.AbstractServiceKeeperAop;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class RestlightAutoSupportAop extends AbstractServiceKeeperAop {

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.RequestMapping)")
    private void request() {
    }

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.GetMapping)")
    private void get0() {
    }

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PostMapping)")
    private void post() {
    }

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PutMapping)")
    private void put() {
    }

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.DeleteMapping)")
    private void delete() {
    }

    @Pointcut("@annotation(esa.restlight.spring.shaded.org.springframework.web.bind.annotation.PatchMapping)")
    private void path() {
    }

    @Around("(request() || get0() || post() || put() || delete() || path())" +
            "&& !(concurrentLimit() || rateLimit() || circuitBreaker() || enableServiceKeeper() || retry() || group() || fallback())")
    public Object doInvoke0(ProceedingJoinPoint pjp) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug("ServiceKeeper's restlight aop is surrounding method: {}",
                    getQualifiedName(pjp));
        }
        return super.doInvoke(pjp);
    }
}
