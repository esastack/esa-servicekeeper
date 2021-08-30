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
package io.esastack.servicekeeper.configsource.file.utils;

import esa.commons.StringUtils;
import io.esastack.servicekeeper.configsource.constant.Constants;
import io.esastack.servicekeeper.core.common.ResourceId;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GroupItemUtils {

    private GroupItemUtils() {
    }

    public static Set<ResourceId> parseToItems(final String origin) {
        String value = StringUtils.trim(origin);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptySet();
        }

        // Single
        if (!(value.startsWith(Constants.ARRAY_FORMAT[0]) && value.endsWith(Constants.ARRAY_FORMAT[1]))) {
            return Collections.emptySet();
        }

        // Set
        Set<ResourceId> resourceIds = new LinkedHashSet<>(8);
        for (String resourceId : value.substring(1, value.length() - 1).split(Constants.COMMA)) {
            if (StringUtils.isEmpty(resourceId)) {
                continue;
            }
            resourceIds.add(ResourceId.from(StringUtils.trim(resourceId)));
        }

        return resourceIds;
    }
}
