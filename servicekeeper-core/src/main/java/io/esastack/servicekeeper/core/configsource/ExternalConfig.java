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
package io.esastack.servicekeeper.core.configsource;

import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

public class ExternalConfig extends DynamicConfig {

    private Class<? extends PredicateStrategy> predicateStrategy;

    private String fallbackMethodName;
    private Class<?> fallbackClass;
    private String fallbackValue;
    private Class<? extends Exception> fallbackExceptionClass;
    private Boolean alsoApplyFallbackToBizException;

    public Class<? extends PredicateStrategy> getPredicateStrategy() {
        return predicateStrategy;
    }

    public void setPredicateStrategy(Class<? extends PredicateStrategy> predicateStrategy) {
        this.predicateStrategy = predicateStrategy;
    }

    public String getFallbackMethodName() {
        return fallbackMethodName;
    }

    public void setFallbackMethodName(String fallbackMethodName) {
        this.fallbackMethodName = fallbackMethodName;
    }

    public Class<?> getFallbackClass() {
        return fallbackClass;
    }

    public void setFallbackClass(Class<?> fallbackClass) {
        this.fallbackClass = fallbackClass;
    }

    public String getFallbackValue() {
        return fallbackValue;
    }

    public void setFallbackValue(String fallbackValue) {
        this.fallbackValue = fallbackValue;
    }

    public Class<? extends Exception> getFallbackExceptionClass() {
        return fallbackExceptionClass;
    }

    public void setFallbackExceptionClass(Class<? extends Exception> fallbackExceptionClass) {
        this.fallbackExceptionClass = fallbackExceptionClass;
    }

    public Boolean getAlsoApplyFallbackToBizException() {
        return alsoApplyFallbackToBizException;
    }

    public void setAlsoApplyFallbackToBizException(Boolean alsoApplyFallbackToBizException) {
        this.alsoApplyFallbackToBizException = alsoApplyFallbackToBizException;
    }

    protected boolean isAllEmpty() {
        return getMaxConcurrentLimit() == null &&
                getLimitForPeriod() == null &&
                getLimitRefreshPeriod() == null &&
                getForcedOpen() == null &&
                getForcedDisabled() == null &&
                getFailureRateThreshold() == null &&
                getRingBufferSizeInHalfOpenState() == null &&
                getRingBufferSizeInClosedState() == null &&
                getWaitDurationInOpenState() == null &&
                getMaxSpendTimeMs() == null &&
                getIgnoreExceptions() == null &&
                getMaxAttempts() == null &&
                getIncludeExceptions() == null &&
                getExcludeExceptions() == null &&
                getDelay() == null &&
                getMaxDelay() == null &&
                getMultiplier() == null &&
                getPredicateStrategy() == null &&
                getFallbackMethodName() == null &&
                getFallbackClass() == null &&
                getFallbackValue() == null &&
                getFallbackExceptionClass() == null &&
                getAlsoApplyFallbackToBizException() == null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExternalConfig{");
        boolean isFirstOne = true;
        if (predicateStrategy != null) {
            sb.append("predicateStrategy=").append(predicateStrategy);
            isFirstOne = false;
        }

        if (fallbackMethodName != null) {
            if (isFirstOne) {
                sb.append("fallbackMethodName=").append(fallbackMethodName);
                isFirstOne = false;
            } else {
                sb.append(", fallbackMethodName=").append(fallbackMethodName);
            }
        }
        if (fallbackClass != null) {
            if (isFirstOne) {
                sb.append("fallbackClass=").append(fallbackClass);
                isFirstOne = false;
            } else {
                sb.append(", fallbackClass=").append(fallbackClass);
            }
        }
        if (fallbackValue != null) {
            if (isFirstOne) {
                sb.append("fallbackValue=").append(fallbackValue);
                isFirstOne = false;
            } else {
                sb.append(", fallbackValue=").append(fallbackValue);
            }
        }
        if (fallbackExceptionClass != null) {
            if (isFirstOne) {
                sb.append("fallbackExceptionClass=").append(fallbackExceptionClass);
                isFirstOne = false;
            } else {
                sb.append(", fallbackExceptionClass=").append(fallbackExceptionClass);
            }
        }
        if (alsoApplyFallbackToBizException != null) {
            if (isFirstOne) {
                sb.append("alsoApplyFallbackToBizException=").append(alsoApplyFallbackToBizException);
                isFirstOne = false;
            } else {
                sb.append(", alsoApplyFallbackToBizException=").append(alsoApplyFallbackToBizException);
            }
        }

        if (!isFirstOne) {
            sb.append(", ");
        }
        sb.append(super.toString());
        sb.append('}');
        return sb.toString();
    }

}
