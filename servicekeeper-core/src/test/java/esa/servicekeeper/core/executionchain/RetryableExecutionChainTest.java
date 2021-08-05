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
package esa.servicekeeper.core.executionchain;

import esa.servicekeeper.core.retry.RetryableExecutor;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetryableExecutionChainTest {

    @Test
    void testConstructor() {
        assertThrows(NullPointerException.class,
                () -> new RetryableExecutionChain(null, null, mock(RetryableExecutor.class)));

        assertThrows(NullPointerException.class,
                () -> new RetryableExecutionChain(Collections.emptyList(), null, null));

        new RetryableExecutionChain(Collections.emptyList(), null, mock(RetryableExecutor.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoExecute0() throws Throwable {
        final RetryableExecutor executor = mock(RetryableExecutor.class);
        final RetryableExecutionChain execution = new RetryableExecutionChain(Collections.emptyList(), null, executor);

        final Context ctx = mock(Context.class);
        final Runnable runnable = mock(Runnable.class);
        execution.doExecute(ctx, () -> null, runnable, true);
        verify(runnable).run();
        verify(executor, never()).doExecute(any(), any(), any(Executable.class));

        execution.doExecute(ctx, () -> null, runnable, false);
        verify(runnable).run();
        verify(executor, times(1)).doExecute(any(), any(), any(Executable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoExecute1() throws Throwable {
        final RetryableExecutor executor = mock(RetryableExecutor.class);
        final RetryableExecutionChain execution = new RetryableExecutionChain(Collections.emptyList(), null, executor);

        final Context ctx = mock(Context.class);
        final Executable<?> executable = mock(Executable.class);
        execution.doExecute(ctx, () -> null, executable, true);
        verify(executable).execute();
        verify(executor, never()).doExecute(any(), any(), any(Executable.class));

        execution.doExecute(ctx, () -> null, executable, false);
        verify(executable).execute();
        verify(executor, times(1)).doExecute(any(), any(), any(Executable.class));
    }
}
