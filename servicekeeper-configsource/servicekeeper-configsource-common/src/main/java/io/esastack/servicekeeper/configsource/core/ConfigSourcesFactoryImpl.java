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
package io.esastack.servicekeeper.configsource.core;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.configsource.GroupConfigSource;
import io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource;
import io.esastack.servicekeeper.core.configsource.PlainConfigSource;
import io.esastack.servicekeeper.core.factory.ConfigSourcesFactory;
import io.esastack.servicekeeper.core.utils.SpiUtils;

import java.util.List;

public class ConfigSourcesFactoryImpl implements ConfigSourcesFactory {

    private final CompositeConfigSource configSource;

    public ConfigSourcesFactoryImpl() {
        this.configSource = Checks.checkNotNull(build(), "BaseConfigSources loaded by spi must not be" +
                " null");
    }

    @Override
    public PlainConfigSource plain() {
        return configSource;
    }

    @Override
    public GroupConfigSource group() {
        return configSource;
    }

    @Override
    public MoatLimitConfigSource limit() {
        return configSource;
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    private CompositeConfigSource build() {
        final List<BaseConfigSource> sources = SpiUtils.loadAll(BaseConfigSource.class);
        return new CompositeConfigSourceImpl(sources);
    }
}
