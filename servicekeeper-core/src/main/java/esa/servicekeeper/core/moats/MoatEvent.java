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
package esa.servicekeeper.core.moats;

import esa.servicekeeper.core.Event;

public interface MoatEvent extends Event {

    /**
     * Get eventType
     *
     * @return EventType
     */
    EventType type();

    enum EventType {
        /**
         * Call was permitted.
         */
        PERMITTED,

        /**
         * Call was rejected caused by concurrent limit exceeds.
         */
        REJECTED_BY_CONCURRENT_LIMIT,

        /**
         * Call was rejected caused by rate limit exceeds.
         */
        REJECTED_BY_RATE_LIMIT,

        /**
         * Call was rejected caused by circuitBreaker not permits.
         */
        REJECTED_BY_CIRCUIT_BREAKER
    }
}
