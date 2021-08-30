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
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class AtomicConcurrentLimiterTest {

    private final String name = "concurrentLimit-test";
    private final int maxConcurrentLimit = RandomUtils.randomInt(5);
    private final ConcurrentLimitConfig limitConfig = ConcurrentLimitConfig.builder()
            .threshold(maxConcurrentLimit).build();

    private AtomicConcurrentLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AtomicConcurrentLimiter(name, limitConfig, null);
    }

    @Test
    void testGetName() {
        then(limiter.name()).isEqualTo(name);
    }

    @Test
    void testAcquirePermission() {
        for (int i = 0; i < maxConcurrentLimit; i++) {
            then(limiter.acquirePermission()).isTrue();
        }
        then(limiter.acquirePermission()).isFalse();
        for (int i = 0; i < maxConcurrentLimit; i++) {
            limiter.release();
        }

        for (int i = 0; i < maxConcurrentLimit; i++) {
            then(limiter.acquirePermission()).isTrue();
            limiter.release();
        }
        for (int i = 0; i < maxConcurrentLimit; i++) {
            then(limiter.acquirePermission()).isTrue();
        }
    }

    @Test
    void testChangeThreshold() {
        int increaseCount = RandomUtils.randomInt(5);
        int newMaxConcurrentLimit = maxConcurrentLimit + increaseCount;
        for (int i = 0; i < maxConcurrentLimit; i++) {
            then(limiter.acquirePermission()).isTrue();
        }
        then(limiter.acquirePermission()).isFalse();
        limiter.changeThreshold(newMaxConcurrentLimit);
        for (int i = 0; i < increaseCount; i++) {
            then(limiter.acquirePermission()).isTrue();
        }
        then(limiter.acquirePermission()).isFalse();
    }

    @Test
    void testGetMetrics() {
        then(limiter.metrics().threshold()).isEqualTo(maxConcurrentLimit);
        then(limiter.metrics().currentCallCount()).isEqualTo(0);
        for (int i = 0; i < maxConcurrentLimit / 2; i++) {
            limiter.acquirePermission();
        }
        then(limiter.metrics().currentCallCount()).isEqualTo(maxConcurrentLimit / 2);
    }
}
