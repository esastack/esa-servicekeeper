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
package io.esastack.servicekeeper.configsource.file;

import io.esastack.servicekeeper.core.BootstrapContext;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static io.esastack.servicekeeper.core.moats.MoatType.CIRCUIT_BREAKER;
import static io.esastack.servicekeeper.core.moats.MoatType.CONCURRENT_LIMIT;
import static io.esastack.servicekeeper.core.moats.MoatType.RATE_LIMIT;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.fail;

class InternalConfigsHandlerTest {

    private static final BootstrapContext ctx = BootstrapContext.singleton(null);
    private static final InternalConfigsHandler HANDLER = SingletonFactory.handler(null);

    @Test
    void testHandleGlobalConfigs() {
        then(ctx.globalConfig().globalDisable()).isTrue();
        then(ctx.globalConfig().argLevelEnable()).isTrue();
        then(ctx.globalConfig().retryEnable()).isTrue();

        HANDLER.updateGlobalConfigs(false, false, false);

        then(ctx.globalConfig().globalDisable()).isFalse();
        then(ctx.globalConfig().retryEnable()).isFalse();
        then(ctx.globalConfig().argLevelEnable()).isFalse();

        HANDLER.updateGlobalConfigs(null, null, null);

        then(ctx.globalConfig().globalDisable()).isFalse();
        then(ctx.globalConfig().argLevelEnable()).isTrue();
        then(ctx.globalConfig().retryEnable()).isTrue();
    }

    @Test
    void testUpdateMaxSizeLimits() {
        // It's not permitted to create when max size LIMIT is null
        for (int i = 0; i < 200; i++) {
            ctx.factory().getOrCreate(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> null, false);
        }
        then(ctx.cluster().getAll().size()).isEqualTo(0);

        final ExternalConfig config = new ExternalConfig();
        config.setFailureRateThreshold(60.0f);
        config.setLimitForPeriod(100);
        config.setMaxConcurrentLimit(20);
        for (int i = 0; i < 200; i++) {
            ctx.factory().getOrCreate(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> config, false);
        }
        then(ctx.cluster().getAll().size()).isEqualTo(101);
        for (int i = 0; i < 101; i++) {
            then(ctx.cluster().get(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i))
                    .getAll().size()).isEqualTo(3);
        }

        // It's permitted to create when max size LIMIT is not null
        final Map<ArgConfigKey, Integer> maxSizeLimits = new HashMap<>(2);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CIRCUIT_BREAKER), 200);
        HANDLER.updateMaxSizeLimits(maxSizeLimits);

        for (int i = 0; i < 200; i++) {
            ctx.factory().getOrCreate(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i),
                    () -> null, () -> null, () -> config, false);
        }
        then(ctx.cluster().getAll().size()).isEqualTo(200);

        for (int i = 101; i < 200; i++) {
            then(ctx.cluster().get(new ArgResourceId(ResourceId.from("testUpdateMaxSizeLimits"), "arg0", i))
                    .getAll().size()).isEqualTo(1);
        }

        // Decrement the max size LIMIT
        maxSizeLimits.clear();
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CIRCUIT_BREAKER), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", RATE_LIMIT), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testUpdateMaxSizeLimits"),
                "arg0", CONCURRENT_LIMIT), 0);

        HANDLER.updateMaxSizeLimits(maxSizeLimits);
        then(ctx.cluster().getAll().size()).isEqualTo(0);
    }

    @Test
    void testParallel() throws InterruptedException {
        final Map<ArgConfigKey, Integer> maxSizeLimits = new HashMap<>(3);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", CIRCUIT_BREAKER), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", RATE_LIMIT), 0);
        maxSizeLimits.put(new ArgConfigKey(ResourceId.from("testParallel"),
                "arg0", CONCURRENT_LIMIT), 0);

        final CountDownLatch latch = new CountDownLatch(6);
        for (int i = 0; i < 6; i++) {
            if (i % 2 == 0) {
                new Thread(() -> {
                    try {
                        HANDLER.updateMaxSizeLimits(maxSizeLimits);
                    } catch (Throwable th) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        HANDLER.updateMaxSizeLimits(emptyMap());
                    } catch (Throwable th) {
                        fail();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
        }

        latch.await();
    }

}
