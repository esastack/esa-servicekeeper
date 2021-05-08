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
package esa.servicekeeper.core.internal;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.ThreadLocalRandom.current;
import static org.assertj.core.api.BDDAssertions.then;

class GlobalConfigTest {

    @Test
    void testGlobalDisable() {
        final GlobalConfig config = new GlobalConfig();
        then(config.globalDisable()).isFalse();
    }

    @Test
    void testArgLevelEnable() {
        final GlobalConfig config = new GlobalConfig();
        then(config.argLevelEnable()).isTrue();
    }

    @Test
    void testRetryEnable() {
        final GlobalConfig config = new GlobalConfig();
        then(config.retryEnable()).isTrue();
    }

    @Test
    void testUpdateGlobalDisable() {
        final GlobalConfig config = new GlobalConfig();

        config.updateGlobalDisable(true);
        then(config.globalDisable()).isTrue();

        config.updateGlobalDisable(false);
        then(config.globalDisable()).isFalse();

        config.updateGlobalDisable(null);
        then(config.globalDisable()).isFalse();
    }

    @Test
    void testUpdateArgLevelEnable() {
        final GlobalConfig config = new GlobalConfig();

        config.updateArgLevelEnable(true);
        then(config.argLevelEnable()).isTrue();

        config.updateArgLevelEnable(false);
        then(config.argLevelEnable()).isFalse();

        config.updateArgLevelEnable(null);
        then(config.argLevelEnable()).isTrue();
    }

    @Test
    void testUpdateRetryEnable() {
        final GlobalConfig config = new GlobalConfig();

        config.updateRetryEnable(true);
        then(config.retryEnable()).isTrue();

        config.updateRetryEnable(false);
        then(config.retryEnable()).isFalse();

        config.updateRetryEnable(null);
        then(config.retryEnable()).isTrue();
    }

    @Test
    void testParallel() throws InterruptedException {
        final GlobalConfig config = new GlobalConfig();

        final AtomicInteger exceptionsCount = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(30);
        for (int i = 0; i < 30; i++) {
            if (i % 3 == 0) {
                new Thread(() -> {
                    try {
                        config.updateRetryEnable(current().nextBoolean());
                    } catch (Throwable th) {
                        exceptionsCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else if (i % 3 == 1) {
                new Thread(() -> {
                    try {
                        config.updateArgLevelEnable(current().nextBoolean());
                    } catch (Throwable th) {
                        exceptionsCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else {
                new Thread(() -> {
                    try {
                        config.updateGlobalDisable(current().nextBoolean());
                    } catch (Throwable th) {
                        exceptionsCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }
        }

        latch.await();
        then(exceptionsCount.get()).isEqualTo(0);
    }

}
