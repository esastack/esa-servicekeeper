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

import esa.servicekeeper.core.EventProcessor;
import esa.servicekeeper.core.MoatListener;
import esa.servicekeeper.core.executionchain.Context;

public interface MoatEventProcessor extends EventProcessor<MoatEvent>, MoatListener {

    /**
     * process MoatEvent
     *
     * @param name name
     * @param event event
     */
    @Override
    default void process(String name, MoatEvent event) {
        switch (event.type()) {
            case PERMITTED:
                onPermitted(event);
                break;
            case REJECTED_BY_RATE_LIMIT:
            case REJECTED_BY_CIRCUIT_BREAKER:
            case REJECTED_BY_CONCURRENT_LIMIT:
                onRejected(event);
                break;
            default:
                break;
        }
    }

    /**
     * To execute this method when {@link Moat#tryThrough(Context)} returns true.
     *
     * @param event event
     */
    default void onPermitted(MoatEvent event) {

    }

    /**
     * To execute this method when {@link Moat#tryThrough(Context)} returns false.
     *
     * @param event event
     */
    default void onRejected(MoatEvent event) {

    }
}
