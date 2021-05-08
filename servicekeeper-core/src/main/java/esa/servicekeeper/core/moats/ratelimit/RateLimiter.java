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
package esa.servicekeeper.core.moats.ratelimit;

import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.metrics.RateLimitMetrics;

import java.time.Duration;

public interface RateLimiter {

    /**
     * Get the name of the rateLimiter.
     *
     * @return name
     */
    String name();

    /**
     * Try to get a permission from the rateLimiter.
     *
     * @param maxWaitTime the maximum time to wait.
     * @return whether acquires successfully.
     */
    boolean acquirePermission(Duration maxWaitTime);

    /**
     * Change the limitForPeriod
     *
     * @param limitForPeriod limit for period
     */
    void changeLimitForPeriod(int limitForPeriod);

    /**
     * Change the rateLimitConfig
     *
     * @param rateLimitConfig config
     */
    void changeConfig(RateLimitConfig rateLimitConfig);

    /**
     * Get rateLimit config
     *
     * @return config.
     */
    RateLimitConfig config();

    /**
     * Get the immutable configuration of current component.
     *
     * @return config.
     */
    RateLimitConfig immutableConfig();

    /**
     * Get the current collector.
     *
     * @return collector
     */
    RateLimitMetrics metrics();
}
