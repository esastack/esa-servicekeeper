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
package io.esastack.servicekeeper.core.config;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.common.ResourceId;

import java.io.Serializable;

public class MoatConfig implements Serializable {

    private static final long serialVersionUID = 682178548352105156L;

    private final ResourceId resourceId;

    public MoatConfig(ResourceId resourceId) {
        Checks.checkNotNull(resourceId, "resourceId");
        this.resourceId = resourceId;
    }

    public ResourceId getResourceId() {
        return resourceId;
    }

}
