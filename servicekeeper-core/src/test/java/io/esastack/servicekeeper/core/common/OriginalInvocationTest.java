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

import java.lang.reflect.Method;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OriginalInvocationTest {

    @Test
    void testConstructor() throws Exception {
        assertThrows(NullPointerException.class, () -> new OriginalInvocation(null, new Class[0]));
        assertThrows(NullPointerException.class, () -> new OriginalInvocation(String.class, (Class<?>[]) null));

        new OriginalInvocation(String.class, new Class[0]);

        assertThrows(NullPointerException.class, () -> new OriginalInvocation(new Object(), null));
        assertThrows(NullPointerException.class, () -> new OriginalInvocation(null,
                Object.class.getMethod("toString")));
        new OriginalInvocation(new Object(), Object.class.getMethod("toString"));
    }

    @Test
    void testBasic() throws Exception {
        final Object obj = new Hello();
        final Method method = Hello.class.getDeclaredMethod("sayHello");

        final OriginalInvocation invocation = new OriginalInvocation(obj, method);
        then(invocation.getReturnType()).isEqualTo(String.class);
        then(invocation.getParameterTypes().length).isEqualTo(0);
        then(invocation.getMethod()).isEqualTo(method);
        then(invocation.getTarget()).isSameAs(obj);

        then(new OriginalInvocation(obj, method)).isEqualTo(invocation);
        then(invocation.toString()).isEqualTo("OriginalInvocation{returnType=class java.lang.String}");
    }

    private static class Hello {

        private String sayHello() {
            return "hello";
        }

    }
}
