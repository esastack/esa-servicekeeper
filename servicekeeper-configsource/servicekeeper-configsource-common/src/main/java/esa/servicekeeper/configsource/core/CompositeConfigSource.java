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
package esa.servicekeeper.configsource.core;

import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.MoatLimitConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.utils.Ordered;

interface CompositeConfigSource extends PlainConfigSource, GroupConfigSource, MoatLimitConfigSource, Ordered {

    /**
     * Get the order of current source.
     *
     * @return order
     */
    @Override
    default int getOrder() {
        return 0;
    }

    /**
     * Obtains the id of current source.
     *
     * @return id
     */
    @Override
    default String name() {
        return "COMPOSITE-CONFIG-SOURCE";
    }
}
