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

import esa.servicekeeper.core.moats.MoatType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface MoatFactory<KEY, CNF1, CNF2, RTU> {

    /**
     * Get moat factories through context.s
     *
     * @param context ctx
     * @return map
     */
    static Map<MoatType, AbstractMoatFactory<?, ?>> factories(LimitableMoatFactoryContext context) {
        final Map<MoatType, AbstractMoatFactory<?, ?>> factories = new HashMap<>(4);
        factories.putIfAbsent(MoatType.CIRCUIT_BREAKER,
                new LimitableMoatFactory.LimitableCircuitBreakerMoatFactory(context));

        factories.putIfAbsent(MoatType.CONCURRENT_LIMIT,
                new LimitableMoatFactory.LimitableConcurrentMoatFactory(context));

        factories.putIfAbsent(MoatType.RATE_LIMIT,
                new LimitableMoatFactory.LimitableRateMoatFactory(context));

        factories.putIfAbsent(MoatType.RETRY,
                new AbstractMoatFactory.RetryOperationFactory(context));

        return Collections.unmodifiableMap(factories);
    }

    /**
     * Get or create target instance by supplied args.
     *
     * @param key             supplied key
     * @param config1         supplied config1
     * @param config2         supplied config2
     * @param immutableConfig immutable config
     * @return instance
     */
    RTU doCreate(KEY key,
                 CNF1 config1,
                 CNF2 config2,
                 CNF2 immutableConfig);

}
