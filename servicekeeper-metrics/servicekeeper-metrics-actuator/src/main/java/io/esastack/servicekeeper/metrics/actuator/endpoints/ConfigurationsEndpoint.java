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
package io.esastack.servicekeeper.metrics.actuator.endpoints;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.FallbackConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.internal.ImmutableConfigs;
import io.esastack.servicekeeper.metrics.actuator.collector.RealTimeConfigCollector;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.FALLBACK_CONFIG;

@Endpoint(id = "skconfigs")
public class ConfigurationsEndpoint {

    private final RealTimeConfigCollector collector;
    private final ImmutableConfigs immutables;

    public ConfigurationsEndpoint(RealTimeConfigCollector collector, ImmutableConfigs immutables) {
        this.collector = collector;
        this.immutables = immutables;
    }

    @ReadOperation
    public Map<String, ServiceKeeperConfigPojo> skConfigs() {
        Map<ResourceId, ServiceKeeperConfig> originalConfigs = collector.configs();
        if (originalConfigs == null) {
            return Collections.emptyMap();
        }
        Map<String, ServiceKeeperConfigPojo> result = new LinkedHashMap<>(originalConfigs.size());
        for (Map.Entry<ResourceId, ServiceKeeperConfig> entry : originalConfigs.entrySet()) {
            result.putIfAbsent(entry.getKey().getName(),
                    ServiceKeeperConfigPojo.from(entry.getValue(),
                            (FallbackConfig) immutables.getConfig(entry.getKey(), FALLBACK_CONFIG)));
        }
        return result;
    }
}

