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
package esa.servicekeeper.core.configsource;

import esa.servicekeeper.core.common.ResourceId;

import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

public class ExternalGroupConfig extends ExternalConfig {

    private volatile Set<ResourceId> items;

    public Set<ResourceId> getItems() {
        return items == null ? emptySet() : unmodifiableSet(items);
    }

    public synchronized void setItems(Set<ResourceId> items) {
        this.items = items;
    }

    @Override
    protected boolean isAllEmpty() {
        return super.isAllEmpty() && (items == null || items.isEmpty());
    }

    @Override
    public String toString() {
        return "ExternalGroupConfig{" + "items:" + items + "," + super.toString();
    }
}
