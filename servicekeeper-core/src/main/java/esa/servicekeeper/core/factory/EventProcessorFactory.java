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
package esa.servicekeeper.core.factory;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.moats.MoatEventProcessor;
import esa.servicekeeper.core.retry.RetryEventProcessor;

public interface EventProcessorFactory {

    /**
     * Obtains a new {@link MoatEventProcessor} by specified {@link ResourceId}.
     *
     * @param id        id
     * @return process
     */
    MoatEventProcessor circuitBreaker(ResourceId id);

    /**
     * Obtains a new {@link MoatEventProcessor} by specified {@link ResourceId}.
     *
     * @param id        id
     * @return process
     */
    MoatEventProcessor concurrentLimit(ResourceId id);

    /**
     * Obtains a new {@link MoatEventProcessor} by specified {@link ResourceId}.
     *
     * @param id        id
     * @return process
     */
    MoatEventProcessor rateLimit(ResourceId id);

    /**
     * Obtains a new {@link RetryEventProcessor} by specified {@link ResourceId}.
     *
     * @param id        id
     * @return process
     */
    RetryEventProcessor retry(ResourceId id);

}
