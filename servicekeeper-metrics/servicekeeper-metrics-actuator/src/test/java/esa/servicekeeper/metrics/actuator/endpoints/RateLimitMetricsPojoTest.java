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

import esa.servicekeeper.core.metrics.RateLimitMetrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class RateLimitMetricsPojoTest {

    @Test
    void testBasic() {
        final RateLimitMetrics metrics = new RateLimitMetrics() {
            @Override
            public int numberOfWaitingThreads() {
                return 1;
            }

            @Override
            public int availablePermissions() {
                return 2;
            }
        };

        final RateLimitMetricsPojo pojo = RateLimitMetricsPojo.from(metrics);
        then(pojo.getAvailablePermissions()).isEqualTo(2);
        then(pojo.getWaitingThreads()).isEqualTo(1);
    }

}
