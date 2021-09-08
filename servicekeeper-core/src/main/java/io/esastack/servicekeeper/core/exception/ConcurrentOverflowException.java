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
import io.esastack.servicekeeper.core.metrics.ConcurrentLimitMetrics;

/**
 * This exception will be thrown when current concurrent number has exceeds max concurrent limit.
 */
public class ConcurrentOverflowException extends ServiceKeeperNotPermittedException {

    private static final long serialVersionUID = 5315117470652086608L;

    private final transient ConcurrentLimitMetrics metrics;

    public ConcurrentOverflowException(String msg, Context ctx, ConcurrentLimitMetrics metrics) {
        super(msg, ctx);
        this.metrics = metrics;
    }

    public ConcurrentLimitMetrics getMetrics() {
        return metrics;
    }

    @Override
    public CauseType getCauseType() {
        return CauseType.CONCURRENT_LIMIT_OVER_FLOW;
    }
}
