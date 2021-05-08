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
package esa.servicekeeper.core.internal;

import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.moats.MoatCluster;

import java.util.Map;
import java.util.function.Function;

public interface InternalMoatCluster {

    /**
     * Get moat cluster by resourceId
     *
     * @param resourceId resourceId
     * @return moat cluster
     */
    MoatCluster get(ResourceId resourceId);

    /**
     * Get all moat clusters which is a copy of current.
     *
     * @return resourceId to moat cluster
     */
    Map<ResourceId, MoatCluster> getAll();

    /**
     * Remove all moats corresponding to resourceId
     *
     * @param resourceId resourceId
     */
    void remove(ResourceId resourceId);

    /**
     * Compute the {@link MoatCluster} and return when the value corresponding to the specified resourceId
     * doesn't exist.
     *
     * @param resourceId resourceId
     * @param function   function
     * @return cluster
     */
    MoatCluster computeIfAbsent(ResourceId resourceId, Function<ResourceId, MoatCluster> function);

}

