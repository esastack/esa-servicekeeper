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

class ExceptionCausePredicateTest {

    @Test
    void testCanRetry0() {
        // Case1: examineCauses is false
        final boolean defaultValue = ThreadLocalRandom.current().nextBoolean();

        ExceptionCausePredicate predicate = new ExceptionCausePredicate(0,
                null, defaultValue, false, 1);
        then(predicate.canRetry0(new RuntimeException())).isEqualTo(defaultValue);
        then(predicate.canRetry0(new SupException())).isEqualTo(defaultValue);
        then(predicate.canRetry0(new SubException())).isEqualTo(defaultValue);

        // Case2: examineCauses is true
        predicate = new ExceptionCausePredicate(0, Collections.singletonMap(SubException.class, true),
                defaultValue, true, 1);

        then(predicate.canRetry0(new SupException())).isEqualTo(defaultValue);
        then(predicate.canRetry0(new SupException(new SubException()))).isTrue();
        then(predicate.canRetry0(new SupException(new SupException()))).isEqualTo(defaultValue);

        then(predicate.canRetry0(new SubException())).isTrue();
        then(predicate.canRetry0(new SubException(new SupException()))).isTrue();
    }

    private static class SupException extends RuntimeException {

        private SupException() {
        }

        private SupException(Throwable cause) {
            super(cause);
        }
    }

    private static class SubException extends SupException {
        private SubException() {
        }

        private SubException(Throwable cause) {
            super(cause);
        }
    }
}
