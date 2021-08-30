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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionPredicateImplTest {

    @Test
    void testConstruct() {
        assertThrows(NullPointerException.class,
                () -> new ExceptionPredicateImpl(0, null, null));
    }

    @Test
    void testWhenThrowableIsNull() {
        ExceptionPredicateImpl predicate =
                new ExceptionPredicateImpl(0, null, true);
        then(predicate.canRetry0(null)).isTrue();

        predicate = new ExceptionPredicateImpl(0, null, false);
        then(predicate.canRetry0(null)).isFalse();
    }

    @Test
    void testCanRetry0() {
        final boolean defaultValue = ThreadLocalRandom.current().nextBoolean();

        ExceptionPredicateImpl predicate = new ExceptionPredicateImpl(3,
                Collections.singletonMap(SupException.class, true), defaultValue);
        then(predicate.canRetry0(new RuntimeException())).isEqualTo(defaultValue);
        then(predicate.canRetry0(new SupException())).isTrue();
        then(predicate.canRetry0(new SubException())).isTrue();
    }

    private static class SupException extends RuntimeException {

    }

    private static class SubException extends SupException {

    }
}
