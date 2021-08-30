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

import static org.assertj.core.api.BDDAssertions.then;

public class RetryConfigTest {

    @Test
    void testCopyFrom() {
        RetryConfig retryConfig1 = RetryConfig.builder()
                .backoffConfig(BackoffConfig.builder().maxDelay(1).build())
                .maxAttempts(2)
                .build();
        RetryConfig retryConfig2 = RetryConfig.copyFrom(retryConfig1).build();
        then(retryConfig1.equals(retryConfig2)).isTrue();
    }
}
