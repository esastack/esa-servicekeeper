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

import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;

class ExternalConfigTest {

    @Test
    void testGetFallbackMethodName() {
        final ExternalConfig config = new ExternalConfig();
        then(config.getFallbackMethodName()).isNull();
        config.setFallbackMethodName("ABC");
        then(config.getFallbackMethodName()).isEqualTo("ABC");
    }

    @Test
    void testGetFallbackClass() {
        final ExternalConfig config = new ExternalConfig();
        then(config.getFallbackClass()).isNull();
        config.setFallbackClass(Object.class);
        then(config.getFallbackClass()).isEqualTo(Object.class);
    }

    @Test
    void testGetFallbackValue() {
        final ExternalConfig config = new ExternalConfig();
        then(config.getFallbackValue()).isNull();
        config.setFallbackValue("ABC");
        then(config.getFallbackValue()).isEqualTo("ABC");
    }

    @Test
    void testGetFallbackExceptionClass() {
        final ExternalConfig config = new ExternalConfig();
        then(config.getFallbackExceptionClass()).isNull();
        config.setFallbackExceptionClass(RuntimeException.class);
        then(config.getFallbackExceptionClass()).isEqualTo(RuntimeException.class);
    }

    @Test
    void testGetAlsoApplyFallbackToBizException() {
        final ExternalConfig config = new ExternalConfig();
        then(config.getAlsoApplyFallbackToBizException()).isNull();
        config.setAlsoApplyFallbackToBizException(true);
        then(config.getAlsoApplyFallbackToBizException()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIsAllEmpty() {
        final ExternalConfig config = new ExternalConfig();
        then(config.isAllEmpty()).isTrue();

        config.setMaxConcurrentLimit(1);
        then(config.isAllEmpty()).isFalse();

        config.setMaxConcurrentLimit(null);
        config.setLimitForPeriod(1);
        then(config.isAllEmpty()).isFalse();

        config.setLimitForPeriod(null);
        config.setLimitRefreshPeriod(Duration.ofSeconds(1L));
        then(config.isAllEmpty()).isFalse();

        config.setLimitRefreshPeriod(null);
        config.setForcedOpen(true);
        then(config.isAllEmpty()).isFalse();

        config.setForcedOpen(null);
        config.setForcedDisabled(true);
        then(config.isAllEmpty()).isFalse();

        config.setForcedDisabled(null);
        config.setFailureRateThreshold(50.0f);
        then(config.isAllEmpty()).isFalse();

        config.setFailureRateThreshold(null);
        config.setRingBufferSizeInHalfOpenState(1);
        then(config.isAllEmpty()).isFalse();

        config.setRingBufferSizeInHalfOpenState(null);
        config.setRingBufferSizeInClosedState(1);
        then(config.isAllEmpty()).isFalse();

        config.setRingBufferSizeInClosedState(null);
        config.setWaitDurationInOpenState(Duration.ofSeconds(1L));
        then(config.isAllEmpty()).isFalse();

        config.setWaitDurationInOpenState(null);
        config.setMaxSpendTimeMs(1L);
        then(config.isAllEmpty()).isFalse();

        config.setMaxSpendTimeMs(null);
        config.setIgnoreExceptions(new Class[]{RuntimeException.class});
        then(config.isAllEmpty()).isFalse();

        config.setIgnoreExceptions(null);
        config.setMaxAttempts(1);
        then(config.isAllEmpty()).isFalse();

        config.setMaxAttempts(null);
        config.setIncludeExceptions(new Class[]{RuntimeException.class});
        then(config.isAllEmpty()).isFalse();

        config.setIncludeExceptions(null);
        config.setExcludeExceptions(new Class[]{RuntimeException.class});
        then(config.isAllEmpty()).isFalse();

        config.setExcludeExceptions(null);
        config.setDelay(1L);
        then(config.isAllEmpty()).isFalse();

        config.setDelay(null);
        config.setMaxDelay(1L);
        then(config.isAllEmpty()).isFalse();

        config.setMaxDelay(null);
        config.setMultiplier(2.0d);
        then(config.isAllEmpty()).isFalse();

        config.setMultiplier(null);
        config.setPredicateStrategy(PredicateByException.class);
        then(config.isAllEmpty()).isFalse();

        config.setPredicateStrategy(null);
        config.setFallbackClass(Object.class);
        then(config.isAllEmpty()).isFalse();

        config.setFallbackClass(null);
        config.setFallbackMethodName("ABC");
        then(config.isAllEmpty()).isFalse();

        config.setFallbackMethodName(null);
        config.setFallbackValue("ABC");
        then(config.isAllEmpty()).isFalse();

        config.setFallbackValue(null);
        config.setFallbackExceptionClass(RuntimeException.class);
        then(config.isAllEmpty()).isFalse();

        config.setFallbackExceptionClass(null);
        then(config.isAllEmpty()).isTrue();
    }

    @Test
    void testToString() {
        final ExternalConfig config = new ExternalConfig();
        config.setAlsoApplyFallbackToBizException(true);
        then(config.toString()).isEqualTo("ExternalConfig{alsoApplyFallbackToBizException=true, null}");
        config.setFallbackExceptionClass(RuntimeException.class);
        then(config.toString()).isEqualTo(
                "ExternalConfig{fallbackExceptionClass=class java.lang.RuntimeException, " +
                        "alsoApplyFallbackToBizException=true, null}");
        config.setFallbackValue("aaa");
        then(config.toString()).isEqualTo("ExternalConfig{fallbackValue=aaa, " +
                "fallbackExceptionClass=class java.lang.RuntimeException, " +
                "alsoApplyFallbackToBizException=true, null}");
        config.setFallbackClass(ExternalConfigTest.class);
        then(config.toString()).isEqualTo("ExternalConfig{" +
                "fallbackClass=class esa.servicekeeper.core.configsource.ExternalConfigTest, " +
                "fallbackValue=aaa, fallbackExceptionClass=class java.lang.RuntimeException, " +
                "alsoApplyFallbackToBizException=true, null}");
        config.setFallbackMethodName("bbb");
        then(config.toString()).isEqualTo("ExternalConfig{fallbackMethodName=bbb, " +
                "fallbackClass=class esa.servicekeeper.core.configsource.ExternalConfigTest, " +
                "fallbackValue=aaa, " +
                "fallbackExceptionClass=class java.lang.RuntimeException, " +
                "alsoApplyFallbackToBizException=true, null}");
        config.setPredicateStrategy(PredicateByException.class);
        then(config.toString()).isEqualTo("ExternalConfig{" +
                "predicateStrategy=" +
                "class esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException, " +
                "fallbackMethodName=bbb, " +
                "fallbackClass=class esa.servicekeeper.core.configsource.ExternalConfigTest, " +
                "fallbackValue=aaa, fallbackExceptionClass=class java.lang.RuntimeException, " +
                "alsoApplyFallbackToBizException=true, null}");
    }

}
