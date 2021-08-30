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
package io.esastack.servicekeeper.core.config;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.utils.DurationUtils;
import io.esastack.servicekeeper.core.utils.ParamCheckUtils;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

public class RateLimitConfig implements Serializable {

    private static final long serialVersionUID = -3909680082329150635L;

    private final Duration limitRefreshPeriod;
    private final int limitForPeriod;

    public RateLimitConfig(Duration limitRefreshPeriod,
                           int limitForPeriod) {
        this.limitRefreshPeriod = limitRefreshPeriod;
        this.limitForPeriod = limitForPeriod;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RateLimitConfig ofDefault() {
        return builder().build();
    }

    public static Builder from(RateLimitConfig config) {
        Checks.checkNotNull(config, "config");
        return new Builder()
                .limitForPeriod(config.getLimitForPeriod())
                .limitRefreshPeriod(config.getLimitRefreshPeriod());
    }

    public Duration getLimitRefreshPeriod() {
        return limitRefreshPeriod;
    }

    public int getLimitForPeriod() {
        return limitForPeriod;
    }

    public long getLimitRefreshPeriodInNanos() {
        return limitRefreshPeriod.toNanos();
    }

    @Override
    public String toString() {
        return "RateLimitConfig{" + "limitRefreshPeriod=" + DurationUtils.toString(limitRefreshPeriod) +
                ", limitForPeriod=" + limitForPeriod +
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
        RateLimitConfig that = (RateLimitConfig) o;
        return limitForPeriod == that.limitForPeriod &&
                Objects.equals(limitRefreshPeriod, that.limitRefreshPeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limitRefreshPeriod, limitForPeriod);
    }

    public static final class Builder {

        private int limitForPeriod = Integer.MAX_VALUE;
        private Duration limitRefreshPeriod = Duration.ofSeconds(1L);

        private Builder() {
        }

        public Builder limitForPeriod(int limitForPeriod) {
            ParamCheckUtils.positiveInt(limitForPeriod, "illegal limitForPeriod: "
                    + limitForPeriod + " (expected > 0)");
            this.limitForPeriod = limitForPeriod;
            return this;
        }

        public Builder limitRefreshPeriod(Duration limitRefreshPeriod) {
            this.limitRefreshPeriod = limitRefreshPeriod;
            return this;
        }

        public RateLimitConfig build() {
            return new RateLimitConfig(limitRefreshPeriod, limitForPeriod);
        }
    }
}
