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
package io.esastack.servicekeeper.core.utils;

import io.esastack.servicekeeper.core.factory.FallbackHandlerFactory;
import io.esastack.servicekeeper.core.factory.FallbackHandlerFactoryImpl;
import io.esastack.servicekeeper.core.factory.PredicateStrategyFactory;
import io.esastack.servicekeeper.core.factory.PredicateStrategyFactoryImpl;
import io.esastack.servicekeeper.core.factory.SateTransitionProcessorFactory;
import io.esastack.servicekeeper.core.factory.SateTransitionProcessorFactoryImpl;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class SpiUtilsTest {

    @Test
    void testLoadByPriority() {
        final FallbackHandlerFactory fallbackHandler = SpiUtils.loadByPriority(FallbackHandlerFactory.class);
        then(fallbackHandler).isNotNull();
        BDDAssertions.then(fallbackHandler.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        then(fallbackHandler).isInstanceOf(FallbackHandlerFactoryImpl.class);

        final PredicateStrategyFactory predicateStrategy = SpiUtils.loadByPriority(PredicateStrategyFactory.class);
        then(predicateStrategy).isNotNull();
        BDDAssertions.then(predicateStrategy.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        then(predicateStrategy).isInstanceOf(PredicateStrategyFactoryImpl.class);

        final SateTransitionProcessorFactory transitionProcessor = SpiUtils
                .loadByPriority(SateTransitionProcessorFactory.class);
        then(transitionProcessor).isNotNull();
        BDDAssertions.then(transitionProcessor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
        then(transitionProcessor).isInstanceOf(SateTransitionProcessorFactoryImpl.class);
    }

    @Test
    void testLoadAll() {
        then(SpiUtils.loadAll(FallbackHandlerFactory.class).size()).isEqualTo(1);
        then(SpiUtils.loadAll(PredicateStrategyFactory.class).size()).isEqualTo(1);
        then(SpiUtils.loadAll(SateTransitionProcessorFactory.class).size()).isEqualTo(1);
    }
}
