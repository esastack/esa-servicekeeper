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
package esa.servicekeeper.core.mock;

import esa.servicekeeper.core.annotation.Alias;
import esa.servicekeeper.core.annotation.ArgsCircuitBreaker;
import esa.servicekeeper.core.annotation.ArgsConcurrentLimiter;
import esa.servicekeeper.core.annotation.ArgsRateLimiter;
import esa.servicekeeper.core.annotation.Backoff;
import esa.servicekeeper.core.annotation.CircuitBreaker;
import esa.servicekeeper.core.annotation.ConcurrentLimiter;
import esa.servicekeeper.core.annotation.Fallback;
import esa.servicekeeper.core.annotation.Group;
import esa.servicekeeper.core.annotation.RateLimiter;
import esa.servicekeeper.core.annotation.Retryable;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;

public class MockMethods {

    public void testGetQualifiedName() {

    }

    public void testArgsKeeperWithoutAnnotation(@Alias("name") String name) {

    }

    public void testArgsAlias(@Alias("name") String name) {

    }

    public void methodWithoutAnnotation() {

    }

    @Alias("method-alias-test")
    public void methodWithAnnotation() {

    }

    @Fallback(fallbackMethod = "fallbackMethod", fallbackClass = MockMethods.class,
            fallbackExceptionClass = RuntimeException.class, fallbackValue = "FallbackValue")
    public void methodOnlyFallback() {

    }

    @ConcurrentLimiter(threshold = 500)
    public void methodOnlyConcurrentLimit() {

    }

    @ConcurrentLimiter(500)
    public void methodConcurrentLimitAliasSet() {

    }

    @ConcurrentLimiter(value = 100, threshold = 500)
    public void methodConcurrentLimitAliasSetError() {

    }

    @RateLimiter(limitForPeriod = 500, limitRefreshPeriod = "2s")
    public void methodOnlyRateLimit() {

    }

    @RateLimiter(1)
    public String methodRateLimitAliasSet() {
        return "testAliasSet";
    }

    @RateLimiter(value = 2, limitForPeriod = 1, limitRefreshPeriod = "10s")
    public void methodRateLimitAliasSetError() {

    }

    @CircuitBreaker(ringBufferSizeInClosedState = 99, ringBufferSizeInHalfOpenState = 9,
            waitDurationInOpenState = "59s", predicateStrategy = PredicateByExceptionAndSpendTime.class,
            maxSpendTimeMs = 50, failureRateThreshold = 49.0f,
            ignoreExceptions = {IllegalStateException.class, IllegalArgumentException.class})
    public void methodOnlyCircuitBreaker() {

    }

    @CircuitBreaker(ringBufferSizeInClosedState = 99, ringBufferSizeInHalfOpenState = 9,
            waitDurationInOpenState = "59s", predicateStrategy = PredicateByExceptionAndSpendTime.class,
            maxSpendTimeMs = 50, value = 49.0f,
            ignoreExceptions = {IllegalStateException.class, IllegalArgumentException.class})
    public void methodCircuitBreakerAliasSet() {

    }

    @CircuitBreaker(ringBufferSizeInClosedState = 99, ringBufferSizeInHalfOpenState = 9,
            waitDurationInOpenState = "59s", predicateStrategy = PredicateByExceptionAndSpendTime.class,
            maxSpendTimeMs = 50, failureRateThreshold = 49.0f, value = 51.0f,
            ignoreExceptions = {IllegalStateException.class, IllegalArgumentException.class})
    public void methodCircuitBreakerAliasSetError() {

    }

    @Retryable(5)
    public void methodRetryAliasSet() {

    }

    @Retryable(value = 5, maxAttempts = 6)
    public void methodRetryAliasSetError() {

    }

    public void methodOnlyArgConcurrentLimit(@ArgsConcurrentLimiter(thresholdMap = "{ZhangSan: 56, LiSi: 23}")
                                                     String name) {

    }

    public void methodOnlyArgRateLimit(@ArgsRateLimiter(limitRefreshPeriod = "2s",
            limitForPeriodMap = "{LiMing: 20, ZhangSan: 60}") String name) {

    }

    public void methodOnlyArgCircuitBreaker(@ArgsCircuitBreaker(ringBufferSizeInHalfOpenState = 11,
            ringBufferSizeInClosedState = 101, waitDurationInOpenState = "61s",
            predicateStrategy = PredicateByExceptionAndSpendTime.class, maxSpendTimeMs = 10,
            ignoreExceptions = {RuntimeException.class},
            failureRateThresholdMap = "{LiMing: 20.0f, ZhangSan: 60.0f}") String name) {

    }

    @Alias("method-withAll")
    @Fallback(fallbackMethod = "fallbackMethod", fallbackClass = MockMethods.class,
            fallbackExceptionClass = RuntimeException.class, fallbackValue = "FallbackValue")
    @RateLimiter(limitForPeriod = 500, limitRefreshPeriod = "2s")
    @ConcurrentLimiter(threshold = 500)
    @CircuitBreaker(ringBufferSizeInClosedState = 99, ringBufferSizeInHalfOpenState = 9,
            waitDurationInOpenState = "59s", predicateStrategy = PredicateByExceptionAndSpendTime.class,
            maxSpendTimeMs = 50, failureRateThreshold = 49.0f,
            ignoreExceptions = {IllegalStateException.class, IllegalArgumentException.class})
    @Retryable(maxAttempts = 10, includeExceptions = RuntimeException.class,
            excludeExceptions = IllegalStateException.class,
            backoff = @Backoff(delay = 10L, maxDelay = 100L, multiplier = 2.0d))
    public void methodWithAll(@Alias("name") @ArgsConcurrentLimiter(thresholdMap = "{ZhangSan: 56, LiSi: 23}")
                                      String name,
                              @Alias("address") @ArgsRateLimiter(limitRefreshPeriod = "2s",
                                      limitForPeriodMap = "{LiMing: 20, ZhangSan: 60}") String address) {

    }

    @Group("abc")
    public void methodWithGroup() {

    }

    public SupClass toRecoverMethod0() {
        return null;
    }

    public SubClass toRecoverMethod1() {
        return null;
    }

    public Object toRecoverMethod2() {
        return null;
    }

    private static class SupClass {

    }

    private static class SubClass extends SupClass {

    }
}
