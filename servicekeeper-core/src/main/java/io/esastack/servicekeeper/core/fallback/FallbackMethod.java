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

import esa.commons.Checks;
import io.esastack.servicekeeper.core.exception.CircuitBreakerNotPermittedException;
import io.esastack.servicekeeper.core.exception.ConcurrentOverFlowException;
import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import io.esastack.servicekeeper.core.exception.ServiceKeeperException;
import io.esastack.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import io.esastack.servicekeeper.core.exception.ServiceRetryException;
import io.esastack.servicekeeper.core.utils.FallbackMethodUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class FallbackMethod {

    private final Method method;
    private final boolean isStatic;
    private final boolean causeAtFirst;
    private final boolean matchFullArgs;

    public FallbackMethod(Method method) {
        Checks.checkNotNull(method, "method");
        this.method = method;
        this.isStatic = Modifier.isStatic(method.getModifiers());
        method.setAccessible(true);
        this.causeAtFirst = FallbackMethodUtils.isCauseAtFirst(method.getParameterTypes());
        this.matchFullArgs = isMatchFullArgs(causeAtFirst, method.getParameterCount());
    }

    public Method getMethod() {
        return method;
    }

    public boolean isStatic() {
        return isStatic;
    }

    boolean isCauseAtFirst() {
        return causeAtFirst;
    }

    boolean isMatchFullArgs() {
        return matchFullArgs;
    }

    boolean canApplyTo(CauseType type) {
        if (!causeAtFirst) {
            return true;
        }
        final Class<?> causeClass = method.getParameterTypes()[0];
        switch (type) {
            case CONCURRENT_LIMIT:
                return causeClass.isAssignableFrom(ConcurrentOverFlowException.class);
            case RATE_LIMIT:
                return causeClass.isAssignableFrom(RateLimitOverflowException.class);
            case CIRCUIT_BREAKER:
                return causeClass.isAssignableFrom(CircuitBreakerNotPermittedException.class);
            case RETRY:
                return causeClass.isAssignableFrom(ServiceRetryException.class);
            case SERVICE_KEEPER_NOT_PERMIT:
                return causeClass.isAssignableFrom(ServiceKeeperNotPermittedException.class);
            case SERVICE_KEEPER:
                return causeClass.isAssignableFrom(ServiceKeeperException.class);
            case BIZ:
                return causeClass.equals(Throwable.class);
            default:
                return false;
        }
    }

    private boolean isMatchFullArgs(boolean isCauseAtFirst, int parameterLength) {
        if (parameterLength == 0) {
            return false;
        }

        return !isCauseAtFirst || parameterLength != 1;
    }

    @Override
    public String toString() {
        return "FallbackMethod{" + "method=" + method.getName() +
                ", isStatic=" + isStatic +
                ", causeAtFirst=" + causeAtFirst +
                ", matchFullArgs=" + matchFullArgs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FallbackMethod method1 = (FallbackMethod) o;
        return isStatic == method1.isStatic &&
                causeAtFirst == method1.causeAtFirst &&
                Objects.equals(method, method1.method);
    }

    @Override
    public int hashCode() {
        int result = method != null ? method.hashCode() : 0;
        result = 31 * result + (isStatic ? 1 : 0);
        result = 31 * result + (causeAtFirst ? 1 : 0);
        return result;
    }

}
