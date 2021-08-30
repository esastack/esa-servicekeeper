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
package io.esastack.servicekeeper.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class BackOffConfigTest {

    @Test
    void testEquals() {
        BackoffConfig backoffConfig1 = BackoffConfig.builder()
                .delay(1)
                .maxDelay(1)
                .multiplier(1).build();
        BackoffConfig backoffConfig2 = BackoffConfig.builder()
                .delay(1)
                .maxDelay(1)
                .multiplier(1).build();
        BackoffConfig backoffConfig3 = BackoffConfig.builder()
                .delay(2)
                .maxDelay(1)
                .multiplier(1).build();

        then(backoffConfig1.equals(null)).isEqualTo(false);
        then(backoffConfig1.equals(123)).isEqualTo(false);
        then(backoffConfig1.equals(backoffConfig2)).isEqualTo(true);
        then(backoffConfig1.equals(backoffConfig3)).isEqualTo(false);
    }

    @Test
    void testCopyFrom() {
        BackoffConfig backoffConfig1 = BackoffConfig.builder()
                .delay(1)
                .maxDelay(1)
                .multiplier(1).build();
        BackoffConfig backoffConfig2 = BackoffConfig.copyFrom(backoffConfig1).build();
        then(backoffConfig1.equals(backoffConfig2)).isEqualTo(true);
    }

    @Test
    void testHashCode() {
        BackoffConfig backoffConfig1 = BackoffConfig.builder()
                .delay(1)
                .maxDelay(1)
                .multiplier(1).build();
        then(backoffConfig1.hashCode()).isEqualTo(1072724031);
    }

}
