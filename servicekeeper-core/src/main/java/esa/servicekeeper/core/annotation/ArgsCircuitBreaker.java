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
package esa.servicekeeper.core.annotation;

import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ArgsCircuitBreaker {

    String DEFAULT_FAILURE_RATE_THRESHOLD_MAP = "";

    /**
     * ringBufferSize while CircuitBreaker is closed
     */
    int ringBufferSizeInClosedState() default 100;

    /**
     * ringBufferSize while CircuitBreaker is half open
     */
    int ringBufferSizeInHalfOpenState() default 10;

    /**
     * waitDuration time(s) from open to half open
     */
    String waitDurationInOpenState() default "60s";

    /**
     * the predicateStrategy to predicate whether a call is success.
     */
    Class<? extends PredicateStrategy> predicateStrategy() default PredicateByException.class;

    /**
     * the maxSpendTimeMs is used to predicate whether a  call is successful.
     */
    int maxSpendTimeMs() default -1;

    /**
     * exceptions to be ignored when predicate whether a call is successful by business Exception.
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};

    /**
     * alias for failureRateThresholdMap
     * eg: "{{LiMing: 10.0f, WangWu: 70.0f, ZhangSan: 20.0f, *: 80.0f}}"
     */
    String value() default DEFAULT_FAILURE_RATE_THRESHOLD_MAP;

    /**
     * @return mapping from value to failureRateThreshold,
     * eg: "{{LiMing: 10.0f, WangWu: 70.0f, ZhangSan: 20.0f, *: 80.0f}}"
     */
    String failureRateThresholdMap() default DEFAULT_FAILURE_RATE_THRESHOLD_MAP;

    /**
     * @return max size of governed value number, the value which exceeds the number will be ignored to
     * use circuit breaker.
     */
    int maxValueSize() default 100;
}
