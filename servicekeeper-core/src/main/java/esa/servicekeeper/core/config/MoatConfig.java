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
package esa.servicekeeper.core.config;

import esa.commons.Checks;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.fallback.FallbackHandler;

import java.io.Serializable;

public class MoatConfig implements Serializable {

    private static final long serialVersionUID = 682178548352105156L;

    private final ResourceId resourceId;
    private final FallbackHandler fallbackHandler;

    public MoatConfig(ResourceId resourceId, FallbackHandler fallbackHandler) {
        Checks.checkNotNull(resourceId, "Moat's resourceId must not be null");
        this.resourceId = resourceId;
        this.fallbackHandler = fallbackHandler;
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

    public FallbackHandler getFallbackHandler() {
        return fallbackHandler;
    }

}
