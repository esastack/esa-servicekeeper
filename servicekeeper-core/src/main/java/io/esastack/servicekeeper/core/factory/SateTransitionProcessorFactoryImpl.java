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
package io.esastack.servicekeeper.core.factory;

import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerSateTransitionProcessor;
import io.esastack.servicekeeper.core.utils.Ordered;
import io.esastack.servicekeeper.core.utils.SpiUtils;

import java.util.List;

public class SateTransitionProcessorFactoryImpl implements SateTransitionProcessorFactory {

    private volatile List<CircuitBreakerSateTransitionProcessor> processors;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public List<CircuitBreakerSateTransitionProcessor> all() {
        if (processors != null) {
            return processors;
        }
        synchronized (this) {
            if (processors != null) {
                return processors;
            }

            processors = doCreate();
            return processors;
        }
    }

    protected List<CircuitBreakerSateTransitionProcessor> doCreate() {
        return SpiUtils.loadAll(CircuitBreakerSateTransitionProcessor.class);
    }

}
