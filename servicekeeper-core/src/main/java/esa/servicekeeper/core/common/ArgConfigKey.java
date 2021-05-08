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

import esa.servicekeeper.core.moats.MoatType;

import java.util.Objects;

import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;

public final class ArgConfigKey {

    private static final String PERIOD_EN = ".";

    private final ResourceId methodId;
    private final String argName;
    private final MoatType type;

    public ArgConfigKey(ResourceId methodId, String argName, MoatType type) {
        this.methodId = methodId;
        this.argName = argName;
        this.type = type;
    }

    public ArgConfigKey(ArgResourceId argId, MoatType type) {
        this(argId.getMethodId(), argId.getArgName(), type);
    }

    public ResourceId getMethodId() {
        return methodId;
    }

    public String getArgName() {
        return argName;
    }

    public MoatType getType() {
        return type;
    }

    public String toMaxSizeLimitKey() {
        final String prefix = methodId.toString() + PERIOD_EN + argName;

        switch (type) {
            case RATE_LIMIT:
                return prefix + PERIOD_EN + MAX_RATE_LIMIT_VALUE_SIZE;
            case CONCURRENT_LIMIT:
                return prefix + PERIOD_EN + MAX_CONCURRENT_LIMIT_VALUE_SIZE;
            case CIRCUIT_BREAKER:
                return prefix + PERIOD_EN + MAX_CIRCUIT_BREAKER_VALUE_SIZE;
            default:
                return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArgConfigKey key = (ArgConfigKey) o;
        return Objects.equals(methodId, key.methodId) &&
                Objects.equals(argName, key.argName) &&
                type == key.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodId, argName, type);
    }

    @Override
    public String toString() {
        return methodId.getName() + "." + argName + "." + type.getValue();
    }
}
