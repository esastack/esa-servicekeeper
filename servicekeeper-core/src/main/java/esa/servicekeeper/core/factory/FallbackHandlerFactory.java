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
package esa.servicekeeper.core.factory;

import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.fallback.FallbackHandlerConfig;
import esa.servicekeeper.core.utils.Ordered;

/**
 * Try to get {@link FallbackHandler} by specified {@link FallbackHandlerConfig}.
 */
public interface FallbackHandlerFactory extends Factory<FallbackHandler<?>, FallbackHandlerConfig>, Ordered {

    /**
     * Whether the current factory {@link Factory#get(Object)}'s result is singleton.
     *
     * @return true or false. True means every {@link Factory#get(Object)} will generate a new object, false
     * means only one object will be constructed.
     */
    default boolean isSingleton() {
        return true;
    }
}

