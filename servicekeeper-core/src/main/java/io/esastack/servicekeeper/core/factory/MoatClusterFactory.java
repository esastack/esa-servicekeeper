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

import io.esastack.servicekeeper.core.common.OriginalInvocation;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.moats.MoatCluster;

import java.util.function.Supplier;

public interface MoatClusterFactory {

    /**
     * Try to get a moat cluster by resourceId, if null try to doCreate one and return.
     *
     * @param resourceId         resourceId
     * @param originalInvocation the supplier to get original invocation
     * @param immutableConfig    the supplier to get immutable config
     * @param externalConfig     the supplier to get external config
     * @param isAsync            isAsync
     * @return moat cluster, null if not configured.
     */
    MoatCluster getOrCreate(ResourceId resourceId, Supplier<OriginalInvocation> originalInvocation,
                            Supplier<ServiceKeeperConfig> immutableConfig,
                            Supplier<ExternalConfig> externalConfig,
                            boolean isAsync);

    /**
     * Try to update moat cluster with newest config.
     *
     * @param resourceId resourceId
     * @param cluster0   cluster
     * @param config     config
     */
    void update(ResourceId resourceId, MoatCluster cluster0, ExternalConfig config);

}

