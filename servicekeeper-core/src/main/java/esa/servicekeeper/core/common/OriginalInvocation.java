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
package esa.servicekeeper.core.common;

import esa.commons.Checks;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public class OriginalInvocation {

    /**
     * If the returnType is null, that means don't check returnType while matching fallback method.
     */
    private final Class<?> returnType;
    /**
     * If the parameterTypes is null, that means don't check parameterTypes while matching fallback method.
     */
    private final Class<?>[] parameterTypes;
    /**
     * the target object.
     */
    private Object target;
    /**
     * the target method
     */
    private Method method;

    public OriginalInvocation(Class<?> returnType, Class<?>[] parameterTypes) {
        Checks.checkNotNull(returnType, "returnType");
        Checks.checkNotNull(parameterTypes, "parameterTypes");
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
    }

    public OriginalInvocation(Object target, Method method) {
        Checks.checkNotNull(target, "target");
        Checks.checkNotNull(method, "method");
        this.target = target;
        this.method = method;
        this.returnType = method.getReturnType();
        this.parameterTypes = method.getParameterTypes();
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OriginalInvocation that = (OriginalInvocation) o;
        return Objects.equals(returnType, that.returnType) &&
                Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(returnType);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OriginalInvocation{");
        boolean isFirstOne = true;
        if (returnType != null) {
            sb.append("returnType=").append(returnType);
            isFirstOne = false;
        }
        if (parameterTypes != null && parameterTypes.length > 0) {
            if (isFirstOne) {
                sb.append("parameterTypes=").append(Arrays.toString(parameterTypes));
            } else {
                sb.append(", parameterTypes=").append(Arrays.toString(parameterTypes));
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
