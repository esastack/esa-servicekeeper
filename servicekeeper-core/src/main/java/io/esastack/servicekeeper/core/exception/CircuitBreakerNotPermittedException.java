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
package io.esastack.servicekeeper.core.exception;

import io.esastack.servicekeeper.core.executionchain.Context;
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;

/**
 * This exception will be thrown when the circuit breaker is open and all invocation is not permitted.
 */
public class CircuitBreakerNotPermittedException extends ServiceKeeperNotPermittedException {

    private static final long serialVersionUID = -3588627945536570147L;

    private final transient CircuitBreakerMetrics metrics;

    public CircuitBreakerNotPermittedException(String msg, Context ctx, CircuitBreakerMetrics metrics) {
        super(msg, ctx);
        this.metrics = metrics;
    }

    public CircuitBreakerMetrics getMetrics() {
        return metrics;
    }

    @Override
    public CauseType getCauseType() {
        return CauseType.CIRCUIT_BREAKER_NOT_PERMIT;
    }
}
