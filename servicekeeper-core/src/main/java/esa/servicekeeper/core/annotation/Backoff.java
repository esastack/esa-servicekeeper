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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Backoff {

    /**
     * The delay in milliseconds,default value 0 means no delay
     */
    long delay() default 0L;

    /**
     * The maximum wait (in milliseconds) between retries.
     *
     * @return the maximum delay between retries (default 0 = ignored)
     */
    long maxDelay() default 0L;

    /**
     * If positive, then used as a multiplier for generating the next delay for backoff.
     *
     * @return a multiplier to use to calculate the next backoff delay (default 0 =
     * ignored)
     */
    double multiplier() default 1.0d;

}

