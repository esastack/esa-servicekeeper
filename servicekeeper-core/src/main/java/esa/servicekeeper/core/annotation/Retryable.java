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

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retryable {
    /**
     * Exception types that are retryable. Defaults to empty (and if excludes is also empty all exceptions are retried).
     * <p>
     * NOTE: Try to match throwable if value is null.
     *
     * @return exception types to retry
     */
    Class<? extends Throwable>[] includeExceptions() default {Exception.class};

    /**
     * Exception types that are not retryable. Defaults to empty (and if includes is also
     * empty allGroups exceptions are retried). If includes is empty but excludes is not, all
     * not excluded exceptions are retried
     *
     * @return exception types not to retry
     */
    Class<? extends Throwable>[] excludeExceptions() default {};

    /**
     * @return the maximum number of attempts (NOTE: including the first failure), defaults to 3
     */
    int maxAttempts() default 3;

    /**
     * Specify the backoff properties for retrying this operation. The default is a simple
     * {@link Backoff} specification with no properties - see it's documentation for
     * defaults.
     *
     * @return a backoff specification
     */
    Backoff backoff() default @Backoff;

}
