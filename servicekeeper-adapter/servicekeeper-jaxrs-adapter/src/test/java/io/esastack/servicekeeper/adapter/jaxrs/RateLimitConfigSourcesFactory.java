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
package io.esastack.servicekeeper.adapter.jaxrs;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.GroupConfigSource;
import io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource;
import io.esastack.servicekeeper.core.configsource.PlainConfigSource;
import io.esastack.servicekeeper.core.factory.ConfigSourcesFactory;

import java.util.Map;

public class RateLimitConfigSourcesFactory implements ConfigSourcesFactory {

    public static PlainConfigSource configSource = new PlainConfigSource() {
        @Override
        public ExternalConfig config(ResourceId key) {
            ExternalConfig config = new ExternalConfig();
            config.setLimitForPeriod(1);
            return config;
        }

        @Override
        public Map<ResourceId, ExternalConfig> all() {
            return null;
        }
    };

    @Override
    public PlainConfigSource plain() {
        return configSource;
    }

    @Override
    public GroupConfigSource group() {
        return null;
    }

    @Override
    public MoatLimitConfigSource limit() {
        return null;
    }
}
