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
package esa.servicekeeper.adapter.restlight;

import esa.restlight.spring.shaded.org.springframework.web.bind.annotation.RequestMapping;
import esa.servicekeeper.adapter.restlight.aop.RestlightAutoSupportAop;
import esa.servicekeeper.adapter.restlight.conf.RestlightAutoSupportConfig;
import esa.servicekeeper.core.entry.ServiceKeeperEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static esa.servicekeeper.adapter.restlight.conf.RestlightAutoSupportConfig.RESTLIGHT_SUPPORT_AOP_PREFIX;

@Configuration
@ConditionalOnClass({ServiceKeeperEntry.class, RequestMapping.class})
@EnableConfigurationProperties(RestlightAutoSupportConfig.class)
public class RestlightAutoSupportConfigurator {

    @Bean
    @ConditionalOnProperty(prefix = RESTLIGHT_SUPPORT_AOP_PREFIX, name = "enable",
            havingValue = "true",
            matchIfMissing = true)
    public RestlightAutoSupportAop restlightAutoSupportAop() {
        return new RestlightAutoSupportAop();
    }
}