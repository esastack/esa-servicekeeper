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
package io.esastack.servicekeeper.adapter.spring;

import io.esastack.servicekeeper.adapter.spring.aop.DefaultServiceKeeperAop;
import io.esastack.servicekeeper.core.asynchandle.AsyncResultHandler;
import io.esastack.servicekeeper.core.asynchandle.CompletableStageHandler;
import io.esastack.servicekeeper.ext.factory.spring.utils.SpringContextUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static io.esastack.servicekeeper.adapter.spring.constant.BeanNames.DEFAULT_SERVICE_KEEPER;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ServiceKeeperConfigurator {

    @Bean
    public AsyncResultHandler<?> completableFutureHandler() {
        return new CompletableStageHandler<>();
    }

    @Bean
    public SpringContextUtils springContextUtils() {
        return new SpringContextUtils();
    }

    @Bean(name = DEFAULT_SERVICE_KEEPER)
    public DefaultServiceKeeperAop defaultServiceKeeperAop() {
        return new DefaultServiceKeeperAop();
    }
}
