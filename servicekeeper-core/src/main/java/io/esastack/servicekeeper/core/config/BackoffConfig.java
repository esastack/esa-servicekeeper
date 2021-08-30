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

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

public class BackoffConfig implements Serializable {

    private static final long serialVersionUID = -6858402797779369553L;

    private final long delay;
    private final long maxDelay;
    private final double multiplier;

    private BackoffConfig(long delay, long maxDelay, double multiplier) {
        this.delay = delay;
        this.maxDelay = maxDelay;
        this.multiplier = multiplier;
    }

    public static Builder copyFrom(BackoffConfig backoffConfig) {
        Checks.checkNotNull(backoffConfig, "backoffConfig");
        return builder().delay(backoffConfig.getDelay())
                .maxDelay(backoffConfig.getMaxDelay())
                .multiplier(backoffConfig.getMultiplier());
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getDelay() {
        return delay;
    }

    public long getMaxDelay() {
        return maxDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BackoffConfig.class.getSimpleName() + "[", "]")
                .add("delay=" + delay)
                .add("maxDelay=" + maxDelay)
                .add("multiplier=" + multiplier)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BackoffConfig that = (BackoffConfig) o;
        return delay == that.delay &&
                maxDelay == that.maxDelay &&
                Double.compare(that.multiplier, multiplier) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delay, maxDelay, multiplier);
    }

    public static class Builder {

        private long delay = 0L;
        private long maxDelay = 0L;
        private double multiplier = 1.0d;

        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public Builder maxDelay(long maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public BackoffConfig build() {
            return new BackoffConfig(delay, maxDelay, multiplier);
        }
    }

}
