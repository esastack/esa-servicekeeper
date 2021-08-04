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
package esa.servicekeeper.core.fallback;

import esa.servicekeeper.core.executionchain.Context;

public interface FallbackHandler<R> {

    /**
     * FallbackHandler
     *
     * @param ctx the ctx of current call
     * @return the result of handler.
     * @throws Throwable throwable
     */
    R handle(Context ctx) throws Throwable;

    /**
     * Get FallbackType.
     *
     * @return fallback FallbackType
     */
    FallbackType getType();

    boolean applyToBizException();

    enum FallbackType {
        /**
         * Fallback to exception
         */
        FALLBACK_TO_EXCEPTION,

        /**
         * Fallback to value
         */
        FALLBACK_TO_VALUE,

        /**
         * Fallback to function
         */
        FALLBACK_TO_FUNCTION
    }
}
