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
package esa.servicekeeper.core.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.assertj.core.api.BDDAssertions.then;

class GenericTypeUtilsTest {

    @Test
    void testGetType() {
        Callable<String> callable = new InnerClass();
        Class<?> clazz = GenericTypeUtils.getSuperClassGenericType(callable.getClass());
        then(clazz).isEqualTo(String.class);
        callable = () -> "Hello World!";
        then(GenericTypeUtils.getSuperClassGenericType(callable.getClass())).isEqualTo(Object.class);
    }

    static class InnerClass implements Callable<String> {
        @Override
        public String call() {
            return "String";
        }
    }

}
