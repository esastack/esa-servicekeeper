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
import io.esastack.servicekeeper.core.metrics.RateLimitMetrics;

/**
 * This exception will be thrown when current qps number has exceeds rateLimit.
 */
public class RateLimitOverflowException extends ServiceKeeperNotPermittedException {

    private static final long serialVersionUID = -7708913444776710002L;

    private final transient RateLimitMetrics metrics;

    public RateLimitOverflowException(String msg, Context ctx, RateLimitMetrics metrics) {
        super(msg, ctx);
        this.metrics = metrics;
    }

    public RateLimitMetrics getMetrics() {
        return metrics;
    }

    @Override
    public CauseType getCauseType() {
        return CauseType.RATE_LIMIT_OVER_FLOW;
    }
}
