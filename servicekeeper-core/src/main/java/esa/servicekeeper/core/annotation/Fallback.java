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

import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Fallback {

    /**
     * @return fallbackMethod name
     */
    String fallbackMethod() default "";

    /**
     * targetClass
     */
    Class<?> fallbackClass() default Void.class;

    /**
     * specifiedException
     */
    Class<? extends Exception> fallbackExceptionClass() default ServiceKeeperNotPermittedException.class;

    /**
     * specifiedValue
     */
    String fallbackValue() default "";

}
