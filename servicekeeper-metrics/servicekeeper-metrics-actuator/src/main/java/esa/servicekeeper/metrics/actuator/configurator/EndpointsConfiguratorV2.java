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
package esa.servicekeeper.metrics.actuator.configurator;

import esa.servicekeeper.core.Bootstrap;
import esa.servicekeeper.core.entry.ServiceKeeperEntry;
import esa.servicekeeper.metrics.actuator.collector.MetricsCollector;
import esa.servicekeeper.metrics.actuator.collector.RealTimeConfigCollector;
import esa.servicekeeper.metrics.actuator.endpoints.ConfigurationEndpoint;
import esa.servicekeeper.metrics.actuator.endpoints.ConfigurationsEndpoint;
import esa.servicekeeper.metrics.actuator.endpoints.MetricsEndpoint;
import esa.servicekeeper.metrics.actuator.endpoints.MetricsesEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import static esa.servicekeeper.adapter.spring.constant.BeanNames.DEFAULT_SERVICE_KEEPER;

/**
 * EndpointsConfiguratorV2 is a configurator for later versions of springBoot after 2.3.0, because
 * {@link ConditionalOnEnabledEndpoint} was deleted and {@link ConditionalOnAvailableEndpoint} was
 * added after 2.3.0.
 */
@Configuration
@AutoConfigureAfter(EndpointAutoConfiguration.class)
@EnableConfigurationProperties(WebEndpointProperties.class)
@ConditionalOnClass({ServiceKeeperEntry.class, ConditionalOnAvailableEndpoint.class})
public class EndpointsConfiguratorV2 {

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
    @ConditionalOnAvailableEndpoint
    public ConfigurationEndpoint skConfigurationEndpoint(RealTimeConfigCollector realTimeConfigCollector) {
        return new ConfigurationEndpoint(realTimeConfigCollector, Bootstrap.ctx().immutableConfigs());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    public MetricsEndpoint skMetricsEndpoint(MetricsCollector metricsCollector) {
        return new MetricsEndpoint(metricsCollector);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    public ConfigurationsEndpoint skConfigurationsEndpoint(RealTimeConfigCollector realTimeConfigCollector) {
        return new ConfigurationsEndpoint(realTimeConfigCollector, Bootstrap.ctx().immutableConfigs());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    public MetricsesEndpoint skMetricsesEndpoint(MetricsCollector metricsCollector) {
        return new MetricsesEndpoint(metricsCollector);
    }
}
