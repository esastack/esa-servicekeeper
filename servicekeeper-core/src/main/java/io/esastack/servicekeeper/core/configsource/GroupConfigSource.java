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
package io.esastack.servicekeeper.core.configsource;

import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;

import java.util.Map;
import java.util.Set;

public interface GroupConfigSource extends ConfigSource {

    /**
     * Get all group config
     *
     * @return group config map
     */
    Map<GroupResourceId, ExternalConfig> allGroups();

    /**
     * Get group config by key
     *
     * @param groupId target config's key
     * @return external config
     */
    ExternalConfig config(GroupResourceId groupId);

    /**
     * Get the group id of specified method
     *
     * @param methodId methodId
     * @return groupId
     */
    GroupResourceId mappingGroupId(ResourceId methodId);

    /**
     * Get resourceIds belongs to target groupId
     *
     * @param groupId groupId
     * @return resourceIds
     */
    Set<ResourceId> mappingResourceIds(GroupResourceId groupId);

}
