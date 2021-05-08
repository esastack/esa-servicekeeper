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
package esa.servicekeeper.core.retry;

import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.executionchain.Context;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

class RetryContextTest {

    @Test
    void testCount() {
        final RetryContext context = mockContext();
        context.registerThrowable(new Throwable());
        then(context.getRetriedCount()).isEqualTo(1);

        context.registerThrowable(null);
        then(context.getRetriedCount()).isEqualTo(2);
    }

    @Test
    void testGetThrowable() {
        final RetryContext context = mockContext();
        context.registerThrowable(new Throwable());
        then(context.getLastThrowable()).isInstanceOf(Throwable.class);

        context.registerThrowable(null);
        then(context.getLastThrowable()).isNull();
    }

    private RetryContext mockContext() {
        final Context context = mock(Context.class);
        final OriginalInvocation invocation = mock(OriginalInvocation.class);
        return new RetryContext(context, invocation);
    }
}
