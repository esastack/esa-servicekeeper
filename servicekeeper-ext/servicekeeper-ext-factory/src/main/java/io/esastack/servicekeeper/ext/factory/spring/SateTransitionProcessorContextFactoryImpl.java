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
package io.esastack.servicekeeper.ext.factory.spring;

import io.esastack.servicekeeper.core.factory.SateTransitionProcessorFactoryImpl;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerSateTransitionProcessor;
import io.esastack.servicekeeper.ext.factory.spring.utils.SpringContextUtils;

import java.util.LinkedList;
import java.util.List;

public class SateTransitionProcessorContextFactoryImpl extends SateTransitionProcessorFactoryImpl {

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected List<CircuitBreakerSateTransitionProcessor> doCreate() {
        final List<CircuitBreakerSateTransitionProcessor> processors = new LinkedList<>();
        processors.addAll(SpringContextUtils.getBeans(CircuitBreakerSateTransitionProcessor.class));
        processors.addAll(super.doCreate());
        return processors;
    }

}
