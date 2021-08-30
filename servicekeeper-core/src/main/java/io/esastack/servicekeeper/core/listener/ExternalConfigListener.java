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
package io.esastack.servicekeeper.core.listener;

import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;

/**
 * The dynamic config listener will be notified by when the newest dynamic config has been received,
 * then observers will fill itself by the newest config.
 */
public interface ExternalConfigListener extends Listener<ResourceId, ExternalConfig> {

    /**
     * To fill itself using the dynamic configuration.
     *
     * @param config config
     */
    @Override
    void onUpdate(ExternalConfig config);

    /**
     * Get the resourceId which current listener is listening to.
     *
     * @return {@link ResourceId}.
     */
    @Override
    ResourceId listeningKey();
}

