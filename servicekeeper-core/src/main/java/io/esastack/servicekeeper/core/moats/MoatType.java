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
package io.esastack.servicekeeper.core.moats;

public enum MoatType {

    /**
     * Circuit breaker moat
     */
    CIRCUIT_BREAKER("CircuitBreaker"),

    /**
     * RateLimit moat
     */
    RATE_LIMIT("RateLimit"),

    /**
     * ConcurrentLimit moat
     */
    CONCURRENT_LIMIT("ConcurrentLimit"),

    /**
     * Retry moat
     */
    RETRY("Retry");

    private final String value;

    MoatType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
