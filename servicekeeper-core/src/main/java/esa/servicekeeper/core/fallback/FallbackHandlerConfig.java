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
package esa.servicekeeper.core.fallback;

import esa.commons.Checks;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.config.FallbackConfig;

import java.util.Objects;

public class FallbackHandlerConfig {

    private final FallbackConfig fallbackConfig;
    private final OriginalInvocation originalInvocation;

    public FallbackHandlerConfig(FallbackConfig fallbackConfig, OriginalInvocation originalInvocation) {
        Checks.checkNotNull(fallbackConfig, "fallbackConfig");
        this.fallbackConfig = fallbackConfig;
        this.originalInvocation = originalInvocation;
    }

    public FallbackConfig getFallbackConfig() {
        return fallbackConfig;
    }

    public OriginalInvocation getOriginalInvocation() {
        return originalInvocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FallbackHandlerConfig that = (FallbackHandlerConfig) o;
        return Objects.equals(fallbackConfig, that.fallbackConfig) &&
                Objects.equals(originalInvocation, that.originalInvocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fallbackConfig, originalInvocation);
    }

    @Override
    public String toString() {
        return "FallbackHandlerConfig{" + "fallbackConfig=" + fallbackConfig +
                ", originalInvocation=" + originalInvocation +
                '}';
    }
}
