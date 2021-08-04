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
package esa.servicekeeper.core.config;

import esa.commons.Checks;

import java.io.Serializable;
import java.util.Objects;

public class FallbackConfig implements Serializable {

    private static final long serialVersionUID = -5270380007399714938L;

    private String methodName;
    private Class<?> targetClass;
    private final String specifiedValue;
    private final Class<? extends Exception> specifiedException;
    private final boolean applyToBizException;

    private FallbackConfig(String methodName, Class<?> targetClass, String specifiedValue,
                           Class<? extends Exception> specifiedException,
                           boolean applyToBizException) {
        this.methodName = methodName;
        this.targetClass = targetClass;
        this.specifiedValue = specifiedValue;
        this.specifiedException = specifiedException;
        this.applyToBizException = applyToBizException;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FallbackConfig ofDefault() {
        return builder().build();
    }

    public static Builder copyFrom(FallbackConfig config) {
        Checks.checkNotNull(config, "config");
        return new Builder()
                .methodName(config.getMethodName())
                .targetClass(config.getTargetClass())
                .specifiedValue(config.getSpecifiedValue())
                .specifiedException(config.getSpecifiedException())
                .applyToBizException(config.isApplyToBizException());
    }

    public String getMethodName() {
        return methodName;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getSpecifiedValue() {
        return specifiedValue;
    }

    public Class<? extends Exception> getSpecifiedException() {
        return specifiedException;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public boolean isApplyToBizException() {
        return applyToBizException;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FallbackConfig that = (FallbackConfig) o;
        return Objects.equals(methodName, that.methodName) &&
                Objects.equals(targetClass, that.targetClass) &&
                Objects.equals(specifiedValue, that.specifiedValue) &&
                Objects.equals(specifiedException, that.specifiedException);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, targetClass, specifiedValue, specifiedException);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FallbackConfig{");
        boolean isFirstOne = true;
        if (methodName != null) {
            sb.append("methodName='").append(methodName).append('\'');
            isFirstOne = false;
        }
        if (targetClass != null) {
            if (isFirstOne) {
                sb.append("targetClass=").append(targetClass);
            } else {
                sb.append(", targetClass=").append(targetClass);
                isFirstOne = false;
            }
        }
        if (specifiedValue != null) {
            if (isFirstOne) {
                sb.append("specifiedValue='").append(specifiedValue).append('\'');
            } else {
                sb.append(", specifiedValue='").append(specifiedValue).append('\'');
                isFirstOne = false;
            }
        }
        if (specifiedException != null) {
            if (isFirstOne) {
                sb.append("specifiedException=").append(specifiedException);
            } else {
                sb.append(", specifiedException=").append(specifiedException);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public static final class Builder {
        /**
         * If the configured methodName is empty, default to use the same name of original method's
         */
        private String methodName = "";
        /**
         * If the configured targetClass is null, default to use the same class of original class's
         */
        private Class<?> targetClass;
        private String specifiedValue = "";
        private Class<? extends Exception> specifiedException;
        private boolean applyToBizException;

        private Builder() {
        }

        public Builder methodName(String fallbackMethodName) {
            this.methodName = fallbackMethodName;
            return this;
        }

        public Builder targetClass(Class<?> fallbackClass) {
            this.targetClass = fallbackClass;
            return this;
        }

        public Builder specifiedValue(String fallbackValue) {
            this.specifiedValue = fallbackValue;
            return this;
        }

        public Builder applyToBizException(boolean applyToBizException) {
            this.applyToBizException = applyToBizException;
            return this;
        }

        public Builder specifiedException(Class<? extends Exception> fallbackExceptionClass) {
            this.specifiedException = fallbackExceptionClass;
            return this;
        }

        public FallbackConfig build() {
            return new FallbackConfig(methodName, targetClass, specifiedValue,
                    specifiedException, applyToBizException);
        }
    }
}
