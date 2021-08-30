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

import static io.esastack.servicekeeper.core.internal.ImmutableConfigs.ConfigType.FALLBACK_CONFIG;

@Endpoint(id = "skconfig")
public class ConfigurationEndpoint {

    private final RealTimeConfigCollector collector;
    private final ImmutableConfigs immutables;

    public ConfigurationEndpoint(RealTimeConfigCollector collector,
                                 ImmutableConfigs immutables) {
        this.collector = collector;
        this.immutables = immutables;
    }

    @ReadOperation
    public ServiceKeeperConfigPojo skConfig(String resourceId) {
        final ResourceId id = ResourceId.from(resourceId);
        ServiceKeeperConfig config = collector.config(id);
        if (config == null) {
            return null;
        }

        return ServiceKeeperConfigPojo.from(config,
                (FallbackConfig) immutables.getConfig(id, FALLBACK_CONFIG));
    }
}

