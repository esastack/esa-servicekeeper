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
package esa.servicekeeper.core.moats.circuitbreaker;

import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.moats.circuitbreaker.internal.CircuitBreakerStateMachine;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CircuitBreakerRegistry {

    private static final Logger logger = LogUtils.logger();

    private final Map<String, CircuitBreaker> limiterMap = new ConcurrentHashMap<>(64);

    private CircuitBreakerRegistry() {
    }

    public static CircuitBreakerRegistry singleton() {
        return new CircuitBreakerRegistry();
    }

    /**
     * Get or doCreate a component by the name and config.
     *
     * @param name               The name of the target component.
     * @param config             the configuration
     * @param immutableConfig    immutable configuration
     * @return t
     */
    public CircuitBreaker getOrCreate(String name,
                                      final CircuitBreakerConfig config,
                                      final CircuitBreakerConfig immutableConfig,
                                      List<CircuitBreakerSateTransitionProcessor> processors) {
        return limiterMap.computeIfAbsent(name,
                key -> new CircuitBreakerStateMachine(name, config, immutableConfig, processors));
    }

    /**
     * Unregister the component from the registry.
     *
     * @param name name
     */
    public void unRegister(String name) {
        if (limiterMap.remove(name) != null && logger.isDebugEnabled()) {
            logger.info("Removed circuitBreaker: {} from registry successfully", name);
        }
    }

}
