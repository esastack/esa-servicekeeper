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

import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.MoatLimitConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.utils.Ordered;

public interface ConfigSourcesFactory extends Ordered {

    /**
     * Obtains {@link PlainConfigSource}
     *
     * @return config source
     */
    PlainConfigSource plain();

    /**
     * Obtains {@link GroupConfigSource}
     *
     * @return config source
     */
    GroupConfigSource group();

    /**
     * Obtains {@link MoatLimitConfigSource}.
     *
     * @return config source
     */
    MoatLimitConfigSource limit();

}

