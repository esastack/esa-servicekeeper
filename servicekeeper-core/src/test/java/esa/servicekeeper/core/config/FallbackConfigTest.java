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
package esa.servicekeeper.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6BDDAssertions.then;

class FallbackConfigTest {

    @Test
    void testEquals() {
        FallbackConfig config1 = FallbackConfig.builder().specifiedValue("ABC")
                .specifiedException(RuntimeException.class)
                .targetClass(Object.class)
                .methodName("ABC")
                .build();
        then(config1.equals(FallbackConfig.copyFrom(config1).build())).isTrue();
    }

    @Test
    void testToString() {
        FallbackConfig config = FallbackConfig.builder().specifiedValue("ABC")
                .specifiedException(RuntimeException.class)
                .targetClass(Object.class)
                .methodName("ABC")
                .build();
        then(config.toString()).isEqualTo("FallbackConfig{methodName='ABC', targetClass=class java.lang.Object," +
                " specifiedValue='ABC', specifiedException=class java.lang.RuntimeException, " +
                "alsoApplyToBizException=false}");

        config = FallbackConfig.builder().specifiedValue("ABC")
                .specifiedException(RuntimeException.class)
                .targetClass(Object.class)
                .methodName("ABC")
                .alsoApplyToBizException(true)
                .build();
        then(config.toString()).isEqualTo("FallbackConfig{methodName='ABC', targetClass=class java.lang.Object," +
                " specifiedValue='ABC', specifiedException=class java.lang.RuntimeException, " +
                "alsoApplyToBizException=true}");
    }
}
