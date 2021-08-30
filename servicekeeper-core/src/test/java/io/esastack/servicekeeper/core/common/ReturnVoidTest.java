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
package io.esastack.servicekeeper.core.common;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.BDDAssertions.then;

class ReturnVoidTest {

    @Test
    void testReturnTypeIsVoid() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = this.getClass().getDeclaredMethod("method1");
        then(method.getReturnType()).isEqualTo(void.class);

        Object result = method.invoke(this);
        then(result).isNull();
    }

    void method1() {

    }
}
