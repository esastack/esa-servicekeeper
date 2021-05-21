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

public class ConcurrentLimitConfig implements Serializable {

    private static final long serialVersionUID = 2869481529133507321L;

    private final int threshold;

    private ConcurrentLimitConfig(int threshold) {
        this.threshold = threshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ConcurrentLimitConfig ofDefault() {
        return builder().build();
    }

    public static Builder from(ConcurrentLimitConfig config) {
        Checks.checkNotNull(config, "config");
        return new Builder().threshold(config.getThreshold());
    }

    public int getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return "ConcurrentLimitConfig{" + "threshold=" + threshold +
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
        ConcurrentLimitConfig that = (ConcurrentLimitConfig) o;
        return threshold == that.threshold;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold);
    }

    public static final class Builder {
        private int threshold = Integer.MAX_VALUE;

        private Builder() {
        }

        public Builder threshold(int threshold) {
            this.threshold = threshold;
            return this;
        }

        public ConcurrentLimitConfig build() {
            return new ConcurrentLimitConfig(threshold);
        }
    }

}
