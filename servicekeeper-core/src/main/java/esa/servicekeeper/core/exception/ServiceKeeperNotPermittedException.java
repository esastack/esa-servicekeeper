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
package esa.servicekeeper.core.exception;

import esa.servicekeeper.core.executionchain.Context;

public class ServiceKeeperNotPermittedException extends ServiceKeeperException {

    private static final String MSG = "Try to through moat failed, original call was rejected";

    private static final long serialVersionUID = -1353460740000085076L;

    private final Context ctx;

    public ServiceKeeperNotPermittedException(Context ctx) {
        this(MSG, ctx);
    }

    public ServiceKeeperNotPermittedException(String msg, Context ctx) {
        super(msg);
        this.ctx = ctx;
    }

    public Context getCtx() {
        return ctx;
    }

    public CauseType getCauseType() {
        return CauseType.UNKNOWN;
    }

    public enum CauseType {
        /**
         * ConcurrentLimitOverFlow
         */
        CONCURRENT_LIMIT_OVER_FLOW,

        /**
         * RateLimitOverFlow
         */
        RATE_LIMIT_OVER_FLOW,

        /**
         * CircuitBreakerNotPermit
         */
        CIRCUIT_BREAKER_NOT_PERMIT,

        /**
         * UnKnown
         */
        UNKNOWN
    }
}
