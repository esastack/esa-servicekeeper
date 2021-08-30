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
package io.esastack.servicekeeper.core.utils;

import io.esastack.servicekeeper.core.configsource.DynamicConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class BeanUtilsTest {

    @Test
    void testNewAs() {
        then((ExternalConfig) BeanUtils.newAs(null)).isNull();

        // Just copy
        final DynamicConfig config = new DynamicConfig();
        int limitForPeriod = RandomUtils.randomInt(200);
        config.setLimitForPeriod(limitForPeriod);
        int maxConcurrentLimit = RandomUtils.randomInt(200);
        config.setMaxConcurrentLimit(maxConcurrentLimit);

        boolean forcedOpen = RandomUtils.randomInt(200) % 2 == 0;
        config.setForcedOpen(forcedOpen);
        boolean forcedDisable = RandomUtils.randomInt(200) % 2 == 0;
        config.setForcedDisabled(forcedDisable);
        float failureRateThreshold = RandomUtils.randomFloat(100);
        config.setFailureRateThreshold(failureRateThreshold);

        long maxSpendTimeMs = RandomUtils.randomLong();
        config.setMaxSpendTimeMs(maxSpendTimeMs);
        Class<? extends Throwable>[] ignoreExceptions = ClassCastUtils.cast(new Class[]{RuntimeException.class});
        config.setIgnoreExceptions(ignoreExceptions);

        int maxAttempts = RandomUtils.randomInt(200);
        config.setMaxAttempts(maxAttempts);
        Class<? extends Throwable>[] includeExceptions = ClassCastUtils
                .cast(new Class[]{IllegalArgumentException.class});
        config.setIncludeExceptions(includeExceptions);
        Class<? extends Throwable>[] excludeExceptions = ClassCastUtils
                .cast(new Class[]{IllegalAccessException.class});
        config.setExcludeExceptions(excludeExceptions);
        double multiplier = 1.0d;
        config.setMultiplier(multiplier);
        long delay = RandomUtils.randomLong();
        config.setDelay(delay);
        long maxDelay = RandomUtils.randomLong();
        config.setMaxDelay(maxDelay);

        final ExternalConfig extConfig = BeanUtils.newAs(config, ExternalConfig.class);
        then(extConfig.getMaxConcurrentLimit()).isEqualTo(maxConcurrentLimit);
        then(extConfig.getLimitForPeriod()).isEqualTo(limitForPeriod);
        then(extConfig.getForcedDisabled()).isEqualTo(forcedDisable);
        then(extConfig.getForcedOpen()).isEqualTo(forcedOpen);
        then(extConfig.getFailureRateThreshold()).isEqualTo(failureRateThreshold);
        then(extConfig.getMaxSpendTimeMs()).isEqualTo(maxSpendTimeMs);
        then(extConfig.getIgnoreExceptions()).isEqualTo(ignoreExceptions);
        then(extConfig.getIncludeExceptions()).isEqualTo(includeExceptions);
        then(extConfig.getExcludeExceptions()).isEqualTo(excludeExceptions);
        then(extConfig.getMaxAttempts()).isEqualTo(maxAttempts);
        then(extConfig.getMultiplier()).isEqualTo(multiplier);
        then(extConfig.getDelay()).isEqualTo(delay);
        then(extConfig.getMaxDelay()).isEqualTo(maxDelay);

        then(extConfig.getLimitRefreshPeriod()).isNull();
        then(extConfig.getRingBufferSizeInClosedState()).isNull();
        then(extConfig.getRingBufferSizeInHalfOpenState()).isNull();
        then(extConfig.getPredicateStrategy()).isNull();
        then(extConfig.getWaitDurationInOpenState()).isNull();
        then(extConfig.getFallbackClass()).isNull();
        then(extConfig.getFallbackMethodName()).isNull();
        then(extConfig.getFallbackValue()).isNull();
        then(extConfig.getFallbackExceptionClass()).isNull();
    }
}
