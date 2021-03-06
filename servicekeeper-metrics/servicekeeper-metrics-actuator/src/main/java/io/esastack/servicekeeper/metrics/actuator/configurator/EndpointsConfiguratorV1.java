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
package io.esastack.servicekeeper.metrics.actuator.configurator;

import io.esastack.servicekeeper.core.Bootstrap;
import io.esastack.servicekeeper.core.entry.ServiceKeeperEntry;
import io.esastack.servicekeeper.metrics.actuator.collector.MetricsCollector;
import io.esastack.servicekeeper.metrics.actuator.collector.RealTimeConfigCollector;
import io.esastack.servicekeeper.metrics.actuator.endpoints.ConfigurationEndpoint;
import io.esastack.servicekeeper.metrics.actuator.endpoints.ConfigurationsEndpoint;
import io.esastack.servicekeeper.metrics.actuator.endpoints.MetricsEndpoint;
import io.esastack.servicekeeper.metrics.actuator.endpoints.MetricsesEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import static io.esastack.servicekeeper.adapter.spring.constant.BeanNames.DEFAULT_SERVICE_KEEPER;

/**
 * EndpointsConfiguratorV1 is a configurator for earlier versions of springBoot before 2.3.0,because
 * {@link ConditionalOnEnabledEndpoint} was deleted after 2.3.0.
 */
@Configuration
@AutoConfigureAfter(EndpointAutoConfiguration.class)
@EnableConfigurationProperties(WebEndpointProperties.class)
@ConditionalOnMissingClass(
        "org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint")
@ConditionalOnClass({ServiceKeeperEntry.class, ConditionalOnEnabledEndpoint.class})
public class EndpointsConfiguratorV1 {

    @Bean
    @ConditionalOnMissingBean
    @DependsOn(DEFAULT_SERVICE_KEEPER)
    public RealTimeConfigCollector skRealTimeConfig() {
        return new RealTimeConfigCollector(Bootstrap.ctx().cluster());
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn(DEFAULT_SERVICE_KEEPER)
    public MetricsCollector skMetricsCollector() {
        return new MetricsCollector(Bootstrap.ctx().cluster());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint
    public ConfigurationEndpoint skConfigurationEndpoint(RealTimeConfigCollector realTimeConfigCollector) {
        return new ConfigurationEndpoint(realTimeConfigCollector, Bootstrap.ctx().immutableConfigs());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint
    public MetricsEndpoint skMetricsEndpoint(MetricsCollector metricsCollector) {
        return new MetricsEndpoint(metricsCollector);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint
    public ConfigurationsEndpoint skConfigurationsEndpoint(RealTimeConfigCollector realTimeConfigCollector) {
        return new ConfigurationsEndpoint(realTimeConfigCollector, Bootstrap.ctx().immutableConfigs());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint
    public MetricsesEndpoint skMetricsesEndpoint(MetricsCollector metricsCollector) {
        return new MetricsesEndpoint(metricsCollector);
    }
}
