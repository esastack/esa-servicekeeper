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
package esa.servicekeeper.configsource.file;

import esa.servicekeeper.configsource.core.BaseConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;

import static esa.servicekeeper.configsource.file.SingletonFactory.cache;

/**
 * The {@link PlainConfigSource} to get config from {@link PropertyFileConfigCache}.
 */
public class PropertyFileConfigSource extends BaseConfigSource {

    private static final String NAME = "PROPERTIES-FILE";

    public PropertyFileConfigSource() {
        super(cache());
    }

    PropertyFileConfigSource(PropertyFileConfigCache cache) {
        super(cache);
    }

    @Override
    public String name() {
        return NAME;
    }
}
