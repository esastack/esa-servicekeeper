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
package esa.servicekeeper.core.moats.circuitbreaker.predicate;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PredicateByExceptionAndSpendTimeTest {

    @Test
    void testConstructor() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);

        assertThrows(NullPointerException.class,
                () -> new PredicateByExceptionAndSpendTime(null, time));
        assertThrows(NullPointerException.class,
                () -> new PredicateByExceptionAndSpendTime(ex, null));
        new PredicateByExceptionAndSpendTime(ex, time);
    }

    @Test
    void testIsSuccess() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        when(ex.isSuccess(any())).thenReturn(false).thenReturn(true).thenReturn(true);
        when(time.isSuccess(any())).thenReturn(false).thenReturn(true);

        assertFalse(predicate.isSuccess(any()));
        assertFalse(predicate.isSuccess(any()));
        assertTrue(predicate.isSuccess(any()));
    }

    @Test
    void testGetFond() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        when(time.getFond(any())).thenReturn(1L);
        then(predicate.getFond(any())).isEqualTo(1L);
    }

    @Test
    void testUpdateWithNewestConfig() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        predicate.updateWithNewestConfig(ThreadLocalRandom.current().nextLong());
        verify(time).updateWithNewestConfig(any());
    }

    @Test
    void testUpdateWhenNewestConfigIsNull() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        predicate.updateWhenNewestConfigIsNull();
        verify(time).updateWhenNewestConfigIsNull();
    }

    @Test
    void testIsConfigEquals() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        predicate.isConfigEquals(any());
        verify(time).isConfigEquals(any());
    }

    @Test
    void testListeningKey() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        predicate.listeningKey();
        verify(time).listeningKey();
    }

    @Test
    void testToString() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        when(time.toString()).thenReturn("ABC");
        then(predicate.toString()).isEqualTo("ABC");
    }

    @Test
    void testFill() {
        final PredicateByException ex = mock(PredicateByException.class);
        final PredicateBySpendTime time = mock(PredicateBySpendTime.class);
        final PredicateByExceptionAndSpendTime predicate = new PredicateByExceptionAndSpendTime(ex, time);

        predicate.fill(any());
        verify(ex).fill(any());
        verify(time).fill(any());
    }
}
