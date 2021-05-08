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

import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ConcurrentLimiterRegistryTest {

    private ConcurrentLimiterRegistry registry = ConcurrentLimiterRegistry.singleton();

    @Test
    void testGetOrCreate() {
        final ConcurrentLimiter limiter = registry.getOrCreate("test", ConcurrentLimitConfig.ofDefault(),
                null);
        then(registry.getOrCreate("test", null, null)).isSameAs(limiter);
    }

    @Test
    void testUnRegister() {
        final ConcurrentLimiter limiter = registry.getOrCreate("test",
                ConcurrentLimitConfig.ofDefault(), null);
        then(registry.getOrCreate("test", null, null)).isSameAs(limiter);
        registry.unRegister("test");
        then(registry.getOrCreate("test", ConcurrentLimitConfig.ofDefault(), null))
                .isNotSameAs(limiter);
    }
}
