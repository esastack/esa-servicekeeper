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
package esa.servicekeeper.core.configsource;

import esa.servicekeeper.core.utils.DurationUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

public class DynamicConfig {

    private Integer maxConcurrentLimit;

    private Integer limitForPeriod;
    private Duration limitRefreshPeriod;

    private Boolean forcedOpen;
    private Boolean forcedDisabled;
    private Float failureRateThreshold;
    private Integer ringBufferSizeInHalfOpenState;
    private Integer ringBufferSizeInClosedState;
    private Duration waitDurationInOpenState;

    private Long maxSpendTimeMs;
    private Class<? extends Throwable>[] ignoreExceptions;

    private Integer maxAttempts;
    private Class<? extends Throwable>[] includeExceptions;
    private Class<? extends Throwable>[] excludeExceptions;
    private Long delay;
    private Long maxDelay;
    private Double multiplier;


    public Integer getMaxConcurrentLimit() {
        return maxConcurrentLimit;
    }

    public void setMaxConcurrentLimit(Integer maxConcurrentLimit) {
        this.maxConcurrentLimit = maxConcurrentLimit;
    }

    public Integer getLimitForPeriod() {
        return limitForPeriod;
    }

    public void setLimitForPeriod(Integer limitForPeriod) {
        this.limitForPeriod = limitForPeriod;
    }

    public Float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(Float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public Long getMaxSpendTimeMs() {
        return maxSpendTimeMs;
    }

    public void setMaxSpendTimeMs(Long maxSpendTimeMs) {
        this.maxSpendTimeMs = maxSpendTimeMs;
    }

    public Boolean getForcedOpen() {
        return forcedOpen;
    }

    public void setForcedOpen(Boolean forcedOpen) {
        this.forcedOpen = forcedOpen;
    }

    public Boolean getForcedDisabled() {
        return forcedDisabled;
    }

    public void setForcedDisabled(Boolean forcedDisabled) {
        this.forcedDisabled = forcedDisabled;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Class<? extends Throwable>[] getIncludeExceptions() {
        return includeExceptions;
    }

    public void setIncludeExceptions(Class<? extends Throwable>[] includeExceptions) {
        this.includeExceptions = includeExceptions;
    }

    public Class<? extends Throwable>[] getExcludeExceptions() {
        return excludeExceptions;
    }

    public void setExcludeExceptions(Class<? extends Throwable>[] excludeExceptions) {
        this.excludeExceptions = excludeExceptions;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Long getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(Long maxDelay) {
        this.maxDelay = maxDelay;
    }

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public Class<? extends Throwable>[] getIgnoreExceptions() {
        return ignoreExceptions;
    }

    public void setIgnoreExceptions(Class<? extends Throwable>[] ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public void setLimitRefreshPeriod(Duration limitRefreshPeriod) {
        this.limitRefreshPeriod = limitRefreshPeriod;
    }

    public Integer getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    public void setRingBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
        this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
    }

    public Integer getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public void setRingBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public void setWaitDurationInOpenState(Duration waitDurationInOpenState) {
        this.waitDurationInOpenState = waitDurationInOpenState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynamicConfig that = (DynamicConfig) o;
        return Objects.equals(maxConcurrentLimit, that.maxConcurrentLimit) &&
                Objects.equals(limitForPeriod, that.limitForPeriod) &&
                Objects.equals(limitRefreshPeriod, that.limitRefreshPeriod) &&
                Objects.equals(forcedOpen, that.forcedOpen) &&
                Objects.equals(forcedDisabled, that.forcedDisabled) &&
                Objects.equals(failureRateThreshold, that.failureRateThreshold) &&
                Objects.equals(ringBufferSizeInHalfOpenState, that.ringBufferSizeInHalfOpenState) &&
                Objects.equals(ringBufferSizeInClosedState, that.ringBufferSizeInClosedState) &&
                Objects.equals(waitDurationInOpenState, that.waitDurationInOpenState) &&
                Objects.equals(maxSpendTimeMs, that.maxSpendTimeMs) &&
                Arrays.equals(ignoreExceptions, that.ignoreExceptions) &&
                Objects.equals(maxAttempts, that.maxAttempts) &&
                Arrays.equals(includeExceptions, that.includeExceptions) &&
                Arrays.equals(excludeExceptions, that.excludeExceptions) &&
                Objects.equals(delay, that.delay) &&
                Objects.equals(maxDelay, that.maxDelay) &&
                Objects.equals(multiplier, that.multiplier);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(maxConcurrentLimit, limitForPeriod, limitRefreshPeriod, forcedOpen,
                forcedDisabled, failureRateThreshold, ringBufferSizeInHalfOpenState,
                ringBufferSizeInClosedState, waitDurationInOpenState, maxSpendTimeMs,
                maxAttempts, delay, maxDelay, multiplier);
        result = 31 * result + Arrays.hashCode(ignoreExceptions);
        result = 31 * result + Arrays.hashCode(includeExceptions);
        result = 31 * result + Arrays.hashCode(excludeExceptions);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DynamicConfig{");

        boolean isFirstOne = appendConcurrent(sb);
        isFirstOne = appendRate(sb, isFirstOne);
        isFirstOne = appendCircuitBreaker(sb, isFirstOne);
        isFirstOne = appendRetry(sb, isFirstOne);
        sb.append('}');

        return isFirstOne ? "null" : sb.toString();
    }

    private boolean appendConcurrent(final StringBuilder sb) {
        if (maxConcurrentLimit != null) {
            sb.append("maxConcurrentLimit=").append(maxConcurrentLimit);
            return false;
        }
        return true;
    }

    private boolean appendRate(final StringBuilder sb, boolean isFirst) {
        if (limitRefreshPeriod != null) {
            if (isFirst) {
                sb.append("limitRefreshPeriod=").append(DurationUtils.toString(limitRefreshPeriod));
                isFirst = false;
            } else {
                sb.append(", limitRefreshPeriod=").append(DurationUtils.toString(limitRefreshPeriod));
            }
        }

        if (limitForPeriod != null) {
            if (isFirst) {
                sb.append("limitForPeriod=").append(limitForPeriod);
                isFirst = false;
            } else {
                sb.append(", limitForPeriod=").append(limitForPeriod);
            }
        }

        return isFirst;
    }

    private boolean appendCircuitBreaker(final StringBuilder sb, boolean isFirst) {
        if (ringBufferSizeInHalfOpenState != null) {
            if (isFirst) {
                sb.append("ringBufferSizeInHalfOpenState=").append(ringBufferSizeInHalfOpenState);
                isFirst = false;
            } else {
                sb.append(", ringBufferSizeInHalfOpenState=").append(ringBufferSizeInHalfOpenState);
            }
        }
        if (ringBufferSizeInClosedState != null) {
            if (isFirst) {
                sb.append("ringBufferSizeInClosedState=").append(ringBufferSizeInClosedState);
                isFirst = false;
            } else {
                sb.append(", ringBufferSizeInClosedState=").append(ringBufferSizeInClosedState);
            }
        }

        if (waitDurationInOpenState != null) {
            if (isFirst) {
                sb.append("waitDurationInOpenState=").append(DurationUtils.toString(waitDurationInOpenState));
                isFirst = false;
            } else {
                sb.append(", waitDurationInOpenState=").append(DurationUtils.toString(waitDurationInOpenState));
            }
        }

        if (forcedOpen != null) {
            if (isFirst) {
                sb.append("forcedOpen=").append(forcedOpen);
                isFirst = false;
            } else {
                sb.append(", forcedOpen=").append(forcedOpen);
            }
        }
        if (forcedDisabled != null) {
            if (isFirst) {
                sb.append("forcedDisabled=").append(forcedDisabled);
                isFirst = false;
            } else {
                sb.append(", forcedDisabled=").append(forcedDisabled);
            }
        }

        if (maxSpendTimeMs != null) {
            if (isFirst) {
                sb.append("maxSpendTimeMs=").append(maxSpendTimeMs);
                isFirst = false;
            } else {
                sb.append(", maxSpendTimeMs=").append(maxSpendTimeMs);
            }
        }
        if (failureRateThreshold != null) {
            if (isFirst) {
                sb.append("failureRateThreshold=").append(failureRateThreshold);
                isFirst = false;
            } else {
                sb.append(", failureRateThreshold=").append(failureRateThreshold);
            }
        }
        if (ignoreExceptions != null && ignoreExceptions.length > 0) {
            if (isFirst) {
                sb.append("ignoreExceptions=").append(Arrays.toString(ignoreExceptions));
                isFirst = false;
            } else {
                sb.append(", ignoreExceptions=").append(Arrays.toString(ignoreExceptions));
            }
        }

        return isFirst;
    }

    private boolean appendRetry(final StringBuilder sb, boolean isFirst) {
        if (maxAttempts != null) {
            if (isFirst) {
                sb.append("maxAttempts=").append(maxAttempts);
                isFirst = false;
            } else {
                sb.append(", maxAttempts=").append(maxAttempts);
            }
        }
        if (includeExceptions != null && includeExceptions.length > 0) {
            if (isFirst) {
                sb.append("includeExceptions=").append(Arrays.toString(includeExceptions));
                isFirst = false;
            } else {
                sb.append(", includeExceptions=").append(Arrays.toString(includeExceptions));
            }
        }
        if (excludeExceptions != null && excludeExceptions.length > 0) {
            if (isFirst) {
                sb.append("excludeExceptions=").append(Arrays.toString(excludeExceptions));
                isFirst = false;
            } else {
                sb.append(", excludeExceptions=").append(Arrays.toString(excludeExceptions));
            }
        }
        if (delay != null) {
            if (isFirst) {
                sb.append("delay=").append(delay);
                isFirst = false;
            } else {
                sb.append(", delay=").append(delay);
            }
        }
        if (maxDelay != null) {
            if (isFirst) {
                sb.append("maxDelay=").append(maxDelay);
                isFirst = false;
            } else {
                sb.append(", maxDelay=").append(maxDelay);
            }
        }
        if (multiplier != null) {
            if (isFirst) {
                sb.append("multiplier=").append(multiplier);
                isFirst = false;
            } else {
                sb.append(", multiplier=").append(multiplier);
            }
        }

        return isFirst;
    }
}
