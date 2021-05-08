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

import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryableExecutorTest {

    @Test
    void testConstructor() {
        assertThrows(NullPointerException.class, () -> new RetryableExecutor(null));
        new RetryableExecutor(mock(RetryOperations.class));
    }

    @Test
    void doExecute() throws Throwable {
        final RetryOperations operations = mock(RetryOperations.class);
        final RetryableExecutor executor = new RetryableExecutor(operations);
        executor.doExecute(mock(Context.class), mock(OriginalInvocation.class), () -> {});
        verify(operations).execute(any(), any());

        executor.doExecute(mock(Context.class), mock(OriginalInvocation.class), () -> "ABC");
        verify(operations, times(2)).execute(any(), any());
    }

    @Test
    void testToString() {
        final RetryOperations operations = mock(RetryOperations.class);
        final RetryableExecutor executor = new RetryableExecutor(operations);
        when(operations.toString()).thenReturn("ABC");
        then(executor.toString()).isEqualTo("RetryableExecutor[operations=ABC]");
    }
}
