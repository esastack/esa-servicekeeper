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
package esa.servicekeeper.core.moats.concurrentlimit;

import esa.commons.Checks;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.metrics.ConcurrentLimitMetrics;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicConcurrentLimiter implements ConcurrentLimiter {

    private final String name;
    private final AtomicInteger callCounter;
    private volatile int threshold;
    private final ConcurrentLimitConfig immutableConfig;

    public AtomicConcurrentLimiter(String name, ConcurrentLimitConfig config, ConcurrentLimitConfig immutableConfig) {
        Checks.checkNotNull(config, "config");
        this.name = name;
        this.threshold = config.getThreshold();
        this.callCounter = new AtomicInteger(0);
        this.immutableConfig = immutableConfig;
    }

    @Override
    public boolean acquirePermission() {
        if (callCounter.incrementAndGet() > threshold) {
            callCounter.decrementAndGet();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void release() {
        callCounter.decrementAndGet();
    }

    @Override
    public void changeThreshold(int newThreshold) {
        this.threshold = newThreshold;
    }

    @Override
    public ConcurrentLimitConfig immutableConfig() {
        return immutableConfig;
    }

    @Override
    public ConcurrentLimitConfig config() {
        return ConcurrentLimitConfig.builder().threshold(threshold).build();
    }

    @Override
    public ConcurrentLimitMetrics metrics() {
        return new Metrics();
    }

    private class Metrics implements ConcurrentLimitMetrics {

        private Metrics() {
        }

        @Override
        public int threshold() {
            return threshold;
        }

        @Override
        public int currentCallCount() {
            return callCounter.get();
        }
    }
}
