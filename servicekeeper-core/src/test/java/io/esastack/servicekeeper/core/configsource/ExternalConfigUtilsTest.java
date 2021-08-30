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
package io.esastack.servicekeeper.core.configsource;

import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateBySpendTime;
import io.esastack.servicekeeper.core.utils.ClassCastUtils;
import io.esastack.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.esastack.servicekeeper.core.configsource.ExternalConfigUtils.*;
import static io.esastack.servicekeeper.core.utils.ClassCastUtils.cast;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalConfigUtilsTest {

    @Test
    void testIsDynamicEquals() {
        then(isDynamicEquals(new ExternalConfig(), null)).isFalse();

        final ExternalConfig config0 = new ExternalConfig();
        final ExternalConfig config1 = new ExternalConfig();

        config0.setMaxConcurrentLimit(10);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setMaxConcurrentLimit(10);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setLimitForPeriod(20);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setLimitForPeriod(20);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setForcedOpen(true);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setForcedOpen(true);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setForcedDisabled(true);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setForcedDisabled(true);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setFailureRateThreshold(50.0f);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setFailureRateThreshold(50.0f);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setRingBufferSizeInClosedState(100);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setRingBufferSizeInClosedState(100);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setRingBufferSizeInHalfOpenState(10);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setRingBufferSizeInHalfOpenState(10);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setMaxSpendTimeMs(100L);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setMaxSpendTimeMs(100L);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setIncludeExceptions(cast(new Class[0]));
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setIncludeExceptions(cast(new Class[0]));
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setExcludeExceptions(cast(new Class[]{IllegalArgumentException.class}));
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setExcludeExceptions(cast(new Class[]{IllegalArgumentException.class}));
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setMaxAttempts(3);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setMaxAttempts(3);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setIgnoreExceptions(cast(new Class[]{IllegalStateException.class}));
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setIgnoreExceptions(cast(new Class[]{IllegalStateException.class}));
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setDelay(20L);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setDelay(20L);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setMaxDelay(100L);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setMaxDelay(100L);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setMultiplier(2.0d);
        then(isDynamicEquals(config0, config1)).isFalse();
        config1.setMultiplier(2.0d);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setFallbackExceptionClass(RuntimeException.class);
        config1.setFallbackExceptionClass(IllegalArgumentException.class);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setPredicateStrategy(PredicateByException.class);
        config1.setPredicateStrategy(PredicateBySpendTime.class);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setFallbackClass(Object.class);
        config1.setFallbackClass(RuntimeException.class);
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setFallbackMethodName("");
        config1.setFallbackMethodName("xx");
        then(isDynamicEquals(config0, config1)).isTrue();

        config0.setFallbackValue("x");
        config1.setFallbackValue("xx");
        then(isDynamicEquals(config0, config1)).isTrue();
    }

    @Test
    void testGetDynamicString() {
        then(ExternalConfigUtils.getDynamicString(null)).isEqualTo("null");

        final ExternalConfig config = mock(ExternalConfig.class);
        when(config.toString()).thenReturn("ABC");
        then(getDynamicString(config)).isEqualTo("ABC");
    }

    @Test
    void testHasBootstrapRate() {
        final ExternalConfig config0 = new ExternalConfig();
        then(hasBootstrapRate(config0)).isFalse();

        config0.setLimitForPeriod(RandomUtils.randomInt(100));
        then(hasBootstrapRate(config0)).isTrue();
    }

    @Test
    void testHasBootstrapConcurrent() {
        final ExternalConfig config0 = new ExternalConfig();
        then(hasBootstrapConcurrent(config0)).isFalse();

        config0.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(hasBootstrapConcurrent(config0)).isTrue();
    }

    @Test
    void testHasBootstrapRetry() {
        final ExternalConfig config0 = new ExternalConfig();
        config0.setMaxAttempts(RandomUtils.randomInt(100));
        then(hasBootstrapRetry(config0)).isTrue();

        config0.setMaxAttempts(null);
        then(hasBootstrapRetry(config0)).isFalse();

        config0.setMultiplier(1.0d);
        then(hasBootstrapRetry(config0)).isFalse();

        config0.setDelay(RandomUtils.randomLong());
        then(hasBootstrapRetry(config0)).isFalse();

        config0.setMaxDelay(RandomUtils.randomLong());
        then(hasBootstrapRetry(config0)).isFalse();

        config0.setIncludeExceptions(ClassCastUtils.cast(new Class[]{RuntimeException.class}));
        then(hasBootstrapRetry(config0)).isTrue();

        config0.setIncludeExceptions(null);
        config0.setExcludeExceptions(ClassCastUtils.cast(new Class[]{IllegalStateException.class}));
        then(hasBootstrapRetry(config0)).isTrue();

        config0.setExcludeExceptions(null);
        config0.setMaxAttempts(1);
        then(hasBootstrapRetry(config0)).isTrue();

        then(hasBootstrapRetry(config0)).isTrue();
    }

    @Test
    void testHasCircuitBreakerConfiguration() {
        ExternalConfig config0 = new ExternalConfig();
        then(hasBootstrapCircuitBreaker(config0)).isFalse();

        config0.setForcedDisabled(RandomUtils.randomInt(100) % 2 == 0);
        then(hasBootstrapCircuitBreaker(config0)).isTrue();

        config0.setForcedDisabled(null);
        config0.setForcedOpen(RandomUtils.randomInt(100) % 2 == 0);
        then(hasBootstrapCircuitBreaker(config0)).isTrue();

        config0.setForcedOpen(null);
        config0.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(hasBootstrapCircuitBreaker(config0)).isTrue();
    }

    @Test
    void testHasFallback() {
        final ExternalConfig config0 = new ExternalConfig();
        then(hasFallback(config0)).isFalse();
        then(hasFallback(null)).isFalse();

        config0.setFallbackExceptionClass(RuntimeException.class);
        then(hasFallback(config0)).isTrue();

        config0.setFallbackExceptionClass(null);
        config0.setFallbackMethodName("abc");
        then(hasFallback(config0)).isTrue();

        config0.setFallbackMethodName(null);
        config0.setFallbackValue("abc");
        then(hasFallback(config0)).isTrue();

        config0.setFallbackValue(null);
        config0.setFallbackExceptionClass(IllegalArgumentException.class);
        then(hasFallback(config0)).isTrue();
    }

    @Test
    void testHasConcurrent() {
        final ExternalConfig config = new ExternalConfig();
        then(hasConcurrent(config)).isFalse();
        config.setMaxConcurrentLimit(1);
        then(hasConcurrent(config)).isTrue();

        config.setMaxConcurrentLimit(null);
        then(hasConcurrent(config)).isFalse();
    }

    @Test
    void testHasRate() {
        final ExternalConfig config = new ExternalConfig();
        then(hasRate(config)).isFalse();

        config.setLimitForPeriod(1);
        then(hasRate(config)).isTrue();
        config.setLimitForPeriod(null);
        config.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        then(hasRate(config)).isTrue();

        config.setLimitRefreshPeriod(null);
        then(hasRate(config)).isFalse();
    }

    @Test
    void testHasCircuitBreaker() {
        final ExternalConfig config = new ExternalConfig();
        then(hasCircuitBreaker(config)).isFalse();

        config.setForcedOpen(true);
        then(hasCircuitBreaker(config)).isTrue();

        config.setForcedOpen(null);
        config.setForcedDisabled(true);
        then(hasCircuitBreaker(config)).isTrue();

        config.setForcedDisabled(null);
        config.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(hasCircuitBreaker(config)).isTrue();

        config.setWaitDurationInOpenState(null);
        config.setRingBufferSizeInClosedState(1);
        then(hasCircuitBreaker(config)).isTrue();

        config.setRingBufferSizeInClosedState(null);
        config.setRingBufferSizeInHalfOpenState(1);
        then(hasCircuitBreaker(config)).isTrue();

        config.setRingBufferSizeInHalfOpenState(null);
        config.setFailureRateThreshold(50.0f);
        then(hasCircuitBreaker(config)).isTrue();

        config.setFailureRateThreshold(null);
        then(hasCircuitBreaker(config)).isFalse();
    }

    @Test
    void testHasBootstrapDynamic() {
        ExternalConfig config0 = new ExternalConfig();
        then(hasBootstrapDynamic(config0)).isFalse();

        //has retry config
        config0.setMaxAttempts(RandomUtils.randomInt(100));
        then(hasBootstrapDynamic(config0)).isTrue();

        //has rateLimit config
        config0.setMaxAttempts(null);
        config0.setLimitForPeriod(RandomUtils.randomInt(100));
        then(hasBootstrapDynamic(config0)).isTrue();

        //has concurrentLimit config
        config0.setLimitForPeriod(null);
        config0.setMaxConcurrentLimit(RandomUtils.randomInt(100));
        then(hasBootstrapDynamic(config0)).isTrue();

        //has circuitBreaker config
        config0.setMaxConcurrentLimit(null);
        config0.setFailureRateThreshold(RandomUtils.randomFloat(100));
        then(hasBootstrapDynamic(config0)).isTrue();
    }

    @Test
    void testIsNotEmpty() {
        then(isEmpty(null)).isTrue();
        then(isEmpty(new ExternalConfig())).isTrue();
        then(isEmpty(new ExternalGroupConfig())).isTrue();

        final ExternalConfig config = new ExternalConfig();
        config.setForcedDisabled(current().nextBoolean());
        then(isEmpty(config)).isFalse();

        config.setForcedDisabled(null);
        config.setForcedOpen(current().nextBoolean());
        then(isEmpty(config)).isFalse();

        config.setForcedOpen(null);
        config.setFailureRateThreshold(current().nextFloat());
        then(isEmpty(config)).isFalse();

        config.setFailureRateThreshold(null);
        config.setRingBufferSizeInClosedState(current().nextInt());
        then(isEmpty(config)).isFalse();

        config.setRingBufferSizeInClosedState(null);
        config.setRingBufferSizeInHalfOpenState(current().nextInt());
        then(isEmpty(config)).isFalse();

        config.setRingBufferSizeInHalfOpenState(null);
        config.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(isEmpty(config)).isFalse();

        config.setWaitDurationInOpenState(null);
        config.setIgnoreExceptions(cast(new Class[0]));
        then(isEmpty(config)).isFalse();

        config.setIgnoreExceptions(null);
        config.setMaxSpendTimeMs(current().nextLong());
        then(isEmpty(config)).isFalse();

        config.setMaxSpendTimeMs(null);
        config.setPredicateStrategy(PredicateByException.class);
        then(isEmpty(config)).isFalse();

        config.setPredicateStrategy(null);
        config.setMaxConcurrentLimit(current().nextInt());
        then(isEmpty(config)).isFalse();

        config.setMaxConcurrentLimit(null);
        config.setLimitForPeriod(current().nextInt());
        then(isEmpty(config)).isFalse();

        config.setLimitForPeriod(null);
        config.setLimitRefreshPeriod(Duration.ofMillis(2L));
        then(isEmpty(config)).isFalse();

        config.setLimitRefreshPeriod(null);
        config.setMaxAttempts(current().nextInt());
        then(isEmpty(config)).isFalse();

        config.setMaxAttempts(null);
        config.setIncludeExceptions(cast(new Class[0]));
        then(isEmpty(config)).isFalse();

        config.setIncludeExceptions(null);
        config.setExcludeExceptions(cast(new Class[0]));
        then(isEmpty(config)).isFalse();

        config.setExcludeExceptions(null);
        config.setDelay(current().nextLong());
        then(isEmpty(config)).isFalse();

        config.setDelay(null);
        config.setMaxDelay(current().nextLong());
        then(isEmpty(config)).isFalse();

        config.setMaxDelay(null);
        config.setMultiplier(current().nextDouble());
        then(isEmpty(config)).isFalse();

        config.setMultiplier(null);
        then(isEmpty(config)).isTrue();
    }

}
