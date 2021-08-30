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
package io.esastack.servicekeeper.core.moats.circuitbreaker;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.Event;

public class CircuitBreakerSateTransitionEvent implements Event {

    private final CircuitBreaker.State preState;
    private final CircuitBreaker.State currentState;

    public CircuitBreakerSateTransitionEvent(CircuitBreaker.State preState,
                                             CircuitBreaker.State currentState) {
        Checks.checkNotNull(preState, "preState");
        Checks.checkNotNull(currentState, "currentState");
        this.preState = preState;
        this.currentState = currentState;
    }

    public final CircuitBreaker.State preState() {
        return preState;
    }

    public final CircuitBreaker.State currentState() {
        return currentState;
    }
}
