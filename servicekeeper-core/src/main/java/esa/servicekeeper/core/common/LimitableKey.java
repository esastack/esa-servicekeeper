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
package esa.servicekeeper.core.common;

import esa.commons.Checks;
import esa.servicekeeper.core.moats.MoatType;

public class LimitableKey {

    private static final String PREFIX = "LimitableKey:";

    private final ResourceId id;
    private final MoatType type;

    public LimitableKey(ResourceId id, MoatType type) {
        Checks.checkNotNull(id, "ResourceId must not be null!");
        this.id = id;
        this.type = type;
    }

    public ResourceId getId() {
        return id;
    }

    public MoatType getType() {
        return type;
    }

    @Override
    public String toString() {
        return PREFIX + (type == null ? id.getName() : id.getName() + "." + type.toString());
    }
}