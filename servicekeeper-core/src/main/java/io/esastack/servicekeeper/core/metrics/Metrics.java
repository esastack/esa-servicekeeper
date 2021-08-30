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
package io.esastack.servicekeeper.core.metrics;

public interface Metrics {

    /**
     * Get the type of the collector.
     *
     * @return type
     */
    Type type();

    enum Type {
        /**
         * ConcurrentLimit
         */
        CONCURRENT_LIMIT("ConcurrentLimit"),

        /**
         * RateLimit
         */
        RATE_LIMIT("RateLimit"),

        /**
         * CircuitBreaker
         */
        CIRCUIT_BREAKER("CircuitBreaker"),

        /**
         * Retry
         */
        RETRY("Retry");

        /**
         * type name
         */
        final String name;

        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
