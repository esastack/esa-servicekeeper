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
package esa.servicekeeper.core.asynchandle;

import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.AsyncContext;
import esa.servicekeeper.core.executionchain.Context;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotAllowedRequestHandleTest {

    private static final String VALUE = "fallback";
    private static final Context CTX = new AsyncContext("abc");

    private final NotAllowedRequestHandle handle = new NotAllowedRequestHandle(VALUE,
            new ServiceKeeperNotPermittedException(CTX), false, new IllegalArgumentException());

    @Test
    void testIsAllowed() {
        then(handle.isAllowed()).isFalse();
    }

    @Test
    void testGetFallbackResult() {
        then(handle.getFallbackResult()).isEqualTo(VALUE);
    }

    @Test
    void testGetNotAllowedCause() {
        then(handle.getNotAllowedCause()).isInstanceOf(ServiceKeeperNotPermittedException.class);
    }

    @Test
    void tesEndWithSuccess() {
        assertThrows(IllegalStateException.class, handle::endWithSuccess);
    }

    @Test
    void testEndWithResult() {
        assertThrows(IllegalStateException.class, () -> handle.endWithResult(null));
    }

    @Test
    void testEndWithError() {
        assertThrows(IllegalStateException.class, () -> handle.endWithError(new RuntimeException()));
    }

    @Test
    void testIsFallbackSucceed() {
        then(handle.isFallbackSucceed()).isFalse();
    }

    @Test
    void testGetFallbackFailsCause() {
        then(handle.getFallbackFailsCause()).isInstanceOf(IllegalArgumentException.class);
    }

}
