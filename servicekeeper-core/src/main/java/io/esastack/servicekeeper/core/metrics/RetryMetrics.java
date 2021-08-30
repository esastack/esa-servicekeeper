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

public interface RetryMetrics extends Metrics {

    /**
     * Obtains current maxAttempts per retry.
     *
     * @return maxAttempts
     */
    int maxAttempts();

    /**
     * The times which retry has real happened,if the method is run succeed directly,this number will not increase
     * if the method thrown Exception a lot of times, this number will only increase once.
     *
     * @return times
     */
    long retriedTimes();

    /**
     * The times which retry has do retry,same to {@link #retriedTimes()}
     * but if the method thrown Exception a lot of times with real retry, like n, this number will increase n
     *
     * @return totalRetryCount
     */
    long totalRetriedCount();

    /**
     * Get the type of current collector.
     *
     * @return type
     */
    @Override
    default Type type() {
        return Type.RETRY;
    }

}

