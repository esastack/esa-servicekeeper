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
package io.esastack.servicekeeper.core.moats.concurrentlimit;

import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.metrics.ConcurrentLimitMetrics;

public interface ConcurrentLimiter {

    /**
     * Try to acquire an permission from this limiter.
     *
     * @return true if acquire successfully, otherwise in contrast.
     */
    boolean acquirePermission();

    /**
     * Release an permission.
     */
    void release();

    /**
     * Get the name of the limiter.
     *
     * @return name
     */
    String name();

    /**
     * Update the newThreshold of current limiter. This method is ThreadSafe.
     *
     * @param newThreshold newThreshold
     */
    void changeThreshold(int newThreshold);

    /**
     * Get the immutable config
     *
     * @return ConcurrentLimitConfig
     */
    ConcurrentLimitConfig immutableConfig();

    /**
     * Get current config
     *
     * @return current config
     */
    ConcurrentLimitConfig config();

    /**
     * Get current collector.
     *
     * @return collector
     */
    ConcurrentLimitMetrics metrics();
}
