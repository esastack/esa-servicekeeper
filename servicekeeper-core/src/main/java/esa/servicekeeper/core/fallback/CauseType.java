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

import esa.servicekeeper.core.exception.ServiceKeeperException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;

enum CauseType {

    /**
     * Caused by rateLimit over flow
     */
    RATE_LIMIT,

    /**
     * Caused by concurrentLimit over flow
     */
    CONCURRENT_LIMIT,

    /**
     * Caused by circuitBreaker
     */
    CIRCUIT_BREAKER,

    /**
     * Caused by retry exception
     */
    RETRY,

    /**
     * General {@link ServiceKeeperNotPermittedException}
     */
    SERVICE_KEEPER_NOT_PERMIT,

    /**
     * General {@link ServiceKeeperException}
     */
    SERVICE_KEEPER,

    /**
     * Unknown reason
     */
    UNKNOWN
}
