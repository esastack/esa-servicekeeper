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
package esa.servicekeeper.core.factory;

import esa.commons.Checks;
import esa.servicekeeper.core.moats.MoatClusterListener;

import java.util.List;

class MoatFactoryContext {

    private final FallbackHandlerFactory fallbackHandler;
    private final List<EventProcessorFactory> processors;
    private final List<MoatClusterListener> listeners;
    private final PredicateStrategyFactory strategy;
    private final SateTransitionProcessorFactory cProcessors;

    MoatFactoryContext(FallbackHandlerFactory fallbackHandler,
                       List<EventProcessorFactory> processors,
                       List<MoatClusterListener> listeners,
                       PredicateStrategyFactory strategy,
                       SateTransitionProcessorFactory cProcessors) {
        Checks.checkNotNull(fallbackHandler, "fallbackHandler");
        Checks.checkNotNull(processors, "processors");
        Checks.checkNotNull(strategy, "strategy");
        Checks.checkNotNull(cProcessors, "cProcessors");
        this.fallbackHandler = fallbackHandler;
        this.processors = processors;
        this.listeners = listeners;
        this.strategy = strategy;
        this.cProcessors = cProcessors;
    }

    FallbackHandlerFactory handler() {
        return fallbackHandler;
    }

    List<EventProcessorFactory> processors() {
        return processors;
    }

    List<MoatClusterListener> listeners() {
        return listeners;
    }

    PredicateStrategyFactory strategy() {
        return strategy;
    }

    SateTransitionProcessorFactory cProcessors() {
        return cProcessors;
    }
}
