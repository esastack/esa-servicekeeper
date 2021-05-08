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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

class AtomicRateLimiterTest {

    private final String name = "rateLimitTest";

    private RateLimitConfig limitConfig;
    private AtomicRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        limitConfig = RateLimitConfig.builder()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMillis(50L)).build();
        rateLimiter = new AtomicRateLimiter(name, limitConfig, null);
    }

    @Test
    void testName() {
        then(rateLimiter.name()).isEqualTo(name);
        rateLimiter = new AtomicRateLimiter(limitConfig, null);
        then(rateLimiter.name()).isNull();
    }

    @Test
    void testAcquirePermission() {
        then(rateLimiter.acquirePermission(Duration.ZERO)).isTrue();
        then(rateLimiter.acquirePermission(Duration.ZERO)).isFalse();
        then(rateLimiter.acquirePermission(Duration.ofSeconds(1L))).isTrue();
    }

    @Test
    void testChangeLimitForPeriod() {
        rateLimiter.changeLimitForPeriod(2);
        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + 50L);
        then(rateLimiter.acquirePermission(Duration.ZERO)).isTrue();
        then(rateLimiter.acquirePermission(Duration.ZERO)).isTrue();
        then(rateLimiter.acquirePermission(Duration.ZERO)).isFalse();
    }

    @Test
    void testGetConfig() {
        then(rateLimiter.config()).isNotNull();
        then(rateLimiter.config()).isSameAs(limitConfig);
    }

    @Test
    void testGetImmutableConfig() {
        then(rateLimiter.immutableConfig()).isNull();
    }

    @Test
    void testGetMetrics() {
        rateLimiter.changeLimitForPeriod(10);
        long currentMillis = currentTimeMillis();
        await().until(() -> currentTimeMillis() > currentMillis + 50L);
        for (int i = 0; i < 20; i++) {
            rateLimiter.acquirePermission(Duration.ofSeconds(1L));
        }
        then(rateLimiter.metrics().availablePermissions()).isEqualTo(0);
        then(rateLimiter.metrics().numberOfWaitingThreads()).isEqualTo(0);
    }

}
