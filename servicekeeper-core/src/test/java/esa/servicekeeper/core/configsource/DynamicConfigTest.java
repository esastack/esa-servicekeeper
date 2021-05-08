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
package esa.servicekeeper.core.configsource;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.BDDAssertions.then;

class DynamicConfigTest {

    @Test
    void testGetterAndSetter() {
        final int maxConcurrentLimit = 1;
        final int limitForPeriod = 2;
        final Duration limitRefreshPeriod = Duration.ofSeconds(1L);
        final boolean forcedOpen = ThreadLocalRandom.current().nextBoolean();
        final boolean forcedDisabled = ThreadLocalRandom.current().nextBoolean();
        final float failureRateThreshold = 99.0f;
        final int ringBufferSizeInHalfOpenState = 20;
        final int ringBufferSizeInClosedState = 30;
        final Duration waitDurationInOpenState = Duration.ofSeconds(60L);
        final long maxSpendTimeMs = 200L;
        final Class<? extends Throwable>[] ignoreExceptions = new Class[]{RuntimeException.class};
        final int maxAttempts = 10;
        final Class<? extends Throwable>[] includeExceptions = new Class[]{RuntimeException.class};
        final Class<? extends Throwable>[] excludeExceptions = new Class[]{IllegalArgumentException.class};
        final long delay = 10L;
        final long maxDelay = 20L;
        final double multiplier = 2.0d;

        final DynamicConfig config = new DynamicConfig();
        config.setMaxConcurrentLimit(maxConcurrentLimit);
        config.setLimitForPeriod(limitForPeriod);
        config.setLimitRefreshPeriod(limitRefreshPeriod);
        config.setForcedOpen(forcedOpen);
        config.setForcedDisabled(forcedDisabled);
        config.setFailureRateThreshold(failureRateThreshold);
        config.setRingBufferSizeInClosedState(ringBufferSizeInClosedState);
        config.setRingBufferSizeInHalfOpenState(ringBufferSizeInHalfOpenState);
        config.setWaitDurationInOpenState(waitDurationInOpenState);
        config.setMaxSpendTimeMs(maxSpendTimeMs);
        config.setIgnoreExceptions(ignoreExceptions);
        config.setMaxAttempts(maxAttempts);
        config.setIncludeExceptions(includeExceptions);
        config.setExcludeExceptions(excludeExceptions);
        config.setDelay(delay);
        config.setMaxDelay(maxDelay);
        config.setMultiplier(multiplier);

        then(config.getMaxConcurrentLimit()).isEqualTo(maxConcurrentLimit);
        then(config.getLimitForPeriod()).isEqualTo(limitForPeriod);
        then(config.getForcedOpen()).isEqualTo(forcedOpen);
        then(config.getForcedDisabled()).isEqualTo(forcedDisabled);
        then(config.getFailureRateThreshold()).isEqualTo(failureRateThreshold);
        then(config.getRingBufferSizeInClosedState()).isEqualTo(ringBufferSizeInClosedState);
        then(config.getRingBufferSizeInHalfOpenState()).isEqualTo(ringBufferSizeInHalfOpenState);
        then(config.getWaitDurationInOpenState()).isEqualTo(waitDurationInOpenState);
        then(config.getMaxSpendTimeMs()).isEqualTo(maxSpendTimeMs);
        then(config.getIgnoreExceptions()).isEqualTo(ignoreExceptions);
        then(config.getExcludeExceptions()).isEqualTo(excludeExceptions);
        then(config.getDelay()).isEqualTo(delay);
        then(config.getMaxDelay()).isEqualTo(maxDelay);
        then(config.getMultiplier()).isEqualTo(multiplier);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testToString() {
        final DynamicConfig config = new DynamicConfig();
        config.setMaxConcurrentLimit(1);
        // maxConcurrentLimit is first
        then(config.toString()).isEqualTo("DynamicConfig{maxConcurrentLimit=1}");
        config.setMaxConcurrentLimit(null);

        // limitRefreshPeriod is first
        config.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        then(config.toString()).isEqualTo("DynamicConfig{limitRefreshPeriod=1s}");
        config.setLimitRefreshPeriod(null);

        // limitForPeriod is first
        config.setLimitForPeriod(1);
        then(config.toString()).isEqualTo("DynamicConfig{limitForPeriod=1}");
        config.setLimitForPeriod(null);

        // ringBufferSizeInHalfOpenState is first
        config.setRingBufferSizeInHalfOpenState(1);
        then(config.toString()).isEqualTo("DynamicConfig{ringBufferSizeInHalfOpenState=1}");
        config.setRingBufferSizeInHalfOpenState(null);

        // ringBufferSizeInClosedState is first
        config.setRingBufferSizeInClosedState(1);
        then(config.toString()).isEqualTo("DynamicConfig{ringBufferSizeInClosedState=1}");
        config.setRingBufferSizeInClosedState(null);

        // waitDurationInOpenState is first
        config.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(config.toString()).isEqualTo("DynamicConfig{waitDurationInOpenState=1s}");
        config.setWaitDurationInOpenState(null);

        // forcedOpen is first
        config.setForcedOpen(true);
        then(config.toString()).isEqualTo("DynamicConfig{forcedOpen=true}");
        config.setForcedOpen(null);

        // forcedDisabled is first
        config.setForcedDisabled(true);
        then(config.toString()).isEqualTo("DynamicConfig{forcedDisabled=true}");
        config.setForcedDisabled(null);

        // maxSpendTimeMs is first
        config.setMaxSpendTimeMs(1L);
        then(config.toString()).isEqualTo("DynamicConfig{maxSpendTimeMs=1}");
        config.setMaxSpendTimeMs(null);

        // failureRateThreshold is first
        config.setFailureRateThreshold(50.0f);
        then(config.toString()).isEqualTo("DynamicConfig{failureRateThreshold=50.0}");
        config.setFailureRateThreshold(null);

        // ignoreExceptions is first
        config.setIgnoreExceptions(new Class[]{RuntimeException.class});
        then(config.toString()).isEqualTo("DynamicConfig{ignoreExceptions=[class java.lang.RuntimeException]}");
        config.setIgnoreExceptions(null);

        // maxAttempts is first
        config.setMaxAttempts(1);
        then(config.toString()).isEqualTo("DynamicConfig{maxAttempts=1}");
        config.setMaxAttempts(null);

        // includeExceptions is first
        config.setIncludeExceptions(new Class[]{RuntimeException.class});
        then(config.toString()).isEqualTo("DynamicConfig{includeExceptions=[class java.lang.RuntimeException]}");
        config.setIncludeExceptions(null);

        // excludeExceptions is first
        config.setExcludeExceptions(new Class[]{RuntimeException.class});
        then(config.toString()).isEqualTo("DynamicConfig{excludeExceptions=[class java.lang.RuntimeException]}");
        config.setExcludeExceptions(null);

        // delay is first
        config.setDelay(1L);
        then(config.toString()).isEqualTo("DynamicConfig{delay=1}");
        config.setDelay(null);

        // maxDelay is first
        config.setMaxDelay(1L);
        then(config.toString()).isEqualTo("DynamicConfig{maxDelay=1}");
        config.setMaxDelay(null);

        // multiplier is first
        config.setMultiplier(1.0d);
        then(config.toString()).isEqualTo("DynamicConfig{multiplier=1.0}");
        config.setMultiplier(null);

        then(config.toString()).isEqualTo("null");

        // composite
        config.setMaxConcurrentLimit(1);
        config.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        config.setLimitForPeriod(1);
        config.setRingBufferSizeInHalfOpenState(1);
        config.setRingBufferSizeInClosedState(1);
        config.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        config.setForcedOpen(true);
        config.setForcedDisabled(true);
        config.setMaxSpendTimeMs(1L);
        config.setFailureRateThreshold(50.0f);
        config.setIgnoreExceptions(new Class[]{RuntimeException.class});
        config.setMaxAttempts(1);
        config.setIncludeExceptions(new Class[]{RuntimeException.class});
        config.setExcludeExceptions(new Class[]{RuntimeException.class});
        config.setDelay(1L);
        config.setMaxDelay(1L);
        config.setMultiplier(1.0d);

        then(config.toString()).isEqualTo("DynamicConfig{maxConcurrentLimit=1," +
                " limitRefreshPeriod=1s," +
                " limitForPeriod=1," +
                " ringBufferSizeInHalfOpenState=1," +
                " ringBufferSizeInClosedState=1," +
                " waitDurationInOpenState=1s," +
                " forcedOpen=true," +
                " forcedDisabled=true," +
                " maxSpendTimeMs=1," +
                " failureRateThreshold=50.0," +
                " ignoreExceptions=[class java.lang.RuntimeException]," +
                " maxAttempts=1," +
                " includeExceptions=[class java.lang.RuntimeException]," +
                " excludeExceptions=[class java.lang.RuntimeException]," +
                " delay=1," +
                " maxDelay=1," +
                " multiplier=1.0}");
    }
}
