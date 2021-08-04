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
package esa.servicekeeper.core.moats;

import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;

import java.util.List;

//TODO 将fallbackHandler()抽到其他地方去，因为Arg的MoatCluster没有fallbackHandler()

/**
 * Many moats makes up one cluster. Usually, a moat cluster is corresponding with one resource. eg,
 * a {@link RateLimitMoat}, {@link ConcurrentLimitMoat} and a {@link CircuitBreakerMoat} which are all used to
 * protect resource A can combine a moat cluster.
 */
public interface MoatCluster {

    /**
     * Whether current moats contains the specified type.
     *
     * @param type type
     * @return true or false.
     */
    boolean contains(MoatType type);

    /**
     * Add a moat to the cluster
     *
     * @param moat the moat to added
     */
    void add(Moat<?> moat);

    /**
     * Remove the moat from the cluster
     *
     * @param moat the moat to removed
     */
    void remove(Moat<?> moat);

    /**
     * Get all moats in the cluster
     *
     * @return the list of moat
     */
    List<Moat<?>> getAll();

    /**
     * Remove moat by type
     *
     * @param type type
     */
    void remove(MoatType type);
}
