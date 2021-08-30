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
package io.esastack.servicekeeper.core.retry.internal.impl;

import io.esastack.servicekeeper.core.retry.RetryContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExponentialBackOffPolicyTest {

    @Test
    void testComputeDelay() {
        final long delay = 30;
        final long maxDelay = 1_000;
        final double multiplier = 2.0;

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy(delay, maxDelay, multiplier);

        final RetryContext context = mock(RetryContext.class);
        when(context.getRetriedCount()).thenReturn(1, 2).thenReturn(100);

        then(backOff.computeDelay(context)).isEqualTo((long) (delay * Math.pow(multiplier, 0)));
        then(backOff.computeDelay(context)).isEqualTo((long) (delay * Math.pow(multiplier, 1)));
        then(backOff.computeDelay(context)).isEqualTo(maxDelay);
    }
}
