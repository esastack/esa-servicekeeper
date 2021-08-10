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

import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.moats.ArgMoatCluster;
import esa.servicekeeper.core.moats.MoatCluster;
import esa.servicekeeper.core.moats.RetryableMoatCluster;

import java.util.function.Supplier;

public interface MoatClusterFactory {

    /**
     * Try to get a moat cluster by methodResourceId, if null try to doCreate one and return.
     *
     * @param resourceId   methodResourceId
     * @param originalInvocation the supplier to get original invocation
     * @param immutableConfig    the supplier to get immutable config
     * @param externalConfig     the supplier to get external config
     * @return moat cluster of method, null if not configured.
     */
    RetryableMoatCluster getOrCreateOfMethod(ResourceId resourceId, Supplier<OriginalInvocation> originalInvocation,
                                             Supplier<ServiceKeeperConfig> immutableConfig,
                                             Supplier<ExternalConfig> externalConfig);

    /**
     * Try to get a moat cluster by argResourceId, if null try to doCreate one and return.
     *
     * @param resourceId      argResourceId
     * @param originalInvocation the supplier to get original invocation
     * @param immutableConfig    the supplier to get immutable config
     * @param externalConfig     the supplier to get external config
     * @return moat cluster of arg, null if not configured.
     */
    ArgMoatCluster getOrCreateOfArg(ArgResourceId resourceId, Supplier<OriginalInvocation> originalInvocation,
                                    Supplier<ServiceKeeperConfig> immutableConfig,
                                    Supplier<ExternalConfig> externalConfig);

    /**
     * Try to update moat cluster with newest config.
     *
     * @param resourceId resourceId
     * @param cluster0   cluster
     * @param config     config
     */
    void update(ResourceId resourceId, MoatCluster cluster0, ExternalConfig config);

}

