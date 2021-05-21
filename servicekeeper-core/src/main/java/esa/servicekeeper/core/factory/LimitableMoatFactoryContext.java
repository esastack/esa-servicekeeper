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
import esa.servicekeeper.core.internal.MoatCreationLimit;
import esa.servicekeeper.core.moats.MoatClusterListener;

import java.util.List;

public class LimitableMoatFactoryContext extends MoatFactoryContext {

    private final MoatCreationLimit limit;

    private LimitableMoatFactoryContext(FallbackHandlerFactory fallbackHandler,
                                        List<EventProcessorFactory> processors,
                                        List<MoatClusterListener> listeners,
                                        PredicateStrategyFactory strategy,
                                        SateTransitionProcessorFactory cProcessors,
                                        MoatCreationLimit limit) {
        super(fallbackHandler, processors, listeners, strategy, cProcessors);
        Checks.checkNotNull(limit, "limit");
        this.limit = limit;
    }

    public static LimitableMoatFactoryContextBuilder builder() {
        return new LimitableMoatFactoryContextBuilder();
    }

    MoatCreationLimit limit() {
        return limit;
    }

    public static class LimitableMoatFactoryContextBuilder {

        private FallbackHandlerFactory fallbackHandler = new FallbackHandlerFactoryImpl();
        private List<EventProcessorFactory> processors;
        private List<MoatClusterListener> listeners;
        private PredicateStrategyFactory strategy = new PredicateStrategyFactoryImpl();
        private MoatCreationLimit limit;
        private SateTransitionProcessorFactory cProcessors;

        public LimitableMoatFactoryContextBuilder fallbackHandler(FallbackHandlerFactory fallbackHandler) {
            this.fallbackHandler = fallbackHandler;
            return this;
        }

        public LimitableMoatFactoryContextBuilder processors(List<EventProcessorFactory> processors) {
            this.processors = processors;
            return this;
        }

        public LimitableMoatFactoryContextBuilder strategy(PredicateStrategyFactory strategy) {
            this.strategy = strategy;
            return this;
        }

        public LimitableMoatFactoryContextBuilder limite(MoatCreationLimit limit) {
            this.limit = limit;
            return this;
        }

        public LimitableMoatFactoryContextBuilder listeners(List<MoatClusterListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public LimitableMoatFactoryContextBuilder cProcessors(SateTransitionProcessorFactory processors) {
            this.cProcessors = processors;
            return this;
        }

        public LimitableMoatFactoryContext build() {
            return new LimitableMoatFactoryContext(fallbackHandler, processors,
                    listeners, strategy, cProcessors, limit);
        }
    }
}
