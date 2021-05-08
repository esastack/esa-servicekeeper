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

import esa.servicekeeper.core.metrics.ConcurrentLimitMetrics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ConcurrentLimitMetricsPojoTest {

    @Test
    void testBasic() {
        final ConcurrentLimitMetrics metrics = new ConcurrentLimitMetrics() {
            @Override
            public int threshold() {
                return 99;
            }

            @Override
            public int currentCallCount() {
                return 66;
            }
        };

        final ConcurrentLimitMetricsPojo pojo = ConcurrentLimitMetricsPojo.from(metrics);
        then(pojo.getCurrentCallCounter()).isEqualTo(66);
        then(pojo.getMaxConcurrentLimit()).isEqualTo(99);
    }
}
