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
package esa.servicekeeper.metrics.actuator.endpoints;

import esa.servicekeeper.core.config.BackoffConfig;
import esa.servicekeeper.core.config.RetryConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.BDDAssertions.then;

class RetryConfigPojoTest {

    @Test
    void testBasic() {
        final RetryConfig config = RetryConfig.builder()
                .includeExceptions(new Class[]{RuntimeException.class})
                .excludeExceptions(new Class[]{IllegalStateException.class})
                .maxAttempts(4)
                .backoffConfig(BackoffConfig.builder()
                        .delay(1L)
                        .maxDelay(10L)
                        .multiplier(2.0d).build())
                .build();

        final RetryConfigPojo pojo = RetryConfigPojo.from(config);
        then(Arrays.equals(pojo.getIncludeExceptions(), new Class[]{RuntimeException.class}));
        then(Arrays.equals(pojo.getExcludeExceptions(), new Class[]{IllegalStateException.class}));
        then(pojo.getMaxAttempts()).isEqualTo(4);
        then(pojo.getBackoffConfig().getDelay()).isEqualTo(1L);
        then(pojo.getBackoffConfig().getMultiplier()).isEqualTo(2.0d);
        then(pojo.getBackoffConfig().getMaxDelay()).isEqualTo(10L);
    }
}
