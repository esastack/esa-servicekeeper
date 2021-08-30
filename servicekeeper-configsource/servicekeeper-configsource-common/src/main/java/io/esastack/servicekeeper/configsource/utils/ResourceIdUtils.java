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
package io.esastack.servicekeeper.configsource.utils;

import esa.commons.StringUtils;
import io.esastack.servicekeeper.configsource.constant.Constants;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static esa.commons.StringUtils.isEmpty;
import static esa.commons.StringUtils.trim;
import static io.esastack.servicekeeper.core.common.ResourceId.from;

public final class ResourceIdUtils {

    private ResourceIdUtils() {
    }

    /**
     * Converts {@link String} to {@link ResourceId}. eg: io.esastack.servicekeeper.demo.DemoClass.demoMethod
     * .limitForPeriod and the target method name is: io.esastack.servicekeeper.demo.DemoClass.demoMethod
     *
     * @param name original name
     * @return resourceId
     */
    public static ResourceId parseWithSuffix(String name) {
        if (isEmpty(name)) {
            return from(StringUtils.EMPTY_STRING);
        }

        name = trim(name);
        if (name.startsWith(Constants.REGEX_PREFIX)) {
            return from(trim(name.substring(Constants.REGEX_PREFIX.length(),
                    computeIndex(name))), true);
        } else {
            return from(name.substring(0, computeIndex(name)));
        }
    }

    /**
     * Converts {@link String} to {@link ResourceId}s directly. eg: esa@a,b,c,d,xyz.* and the target resourceId names
     * are:
     * {@link ArgResourceId} esa.a
     * {@link ArgResourceId} esa.b
     * {@link ArgResourceId} esa.c
     * {@link ArgResourceId} esa.d
     * {@link ArgResourceId} esa.xyz.*
     * <p>
     * eg: regex:esa.* and the target resourceId is:
     * {@link ResourceId} which is regex
     *
     * @param name name
     * @return resourceIds
     */
    public static List<ResourceId> complexParse(String name) {
        if (isEmpty(name)) {
            return Collections.emptyList();
        }

        name = trim(name);
        if (name.startsWith(Constants.REGEX_PREFIX)) {
            return Collections.singletonList(from(trim(name.substring(Constants.REGEX_PREFIX.length())), true));
        }

        final int index = name.lastIndexOf(Constants.AT);
        if (index < 0) {
            return Collections.singletonList(from(name));
        }

        final String prefix = trim(name.substring(0, index));
        final String postfix = trim(name.substring(index + 1));
        if (isEmpty(postfix)) {
            return Collections.singletonList(from(prefix));
        }

        final String[] args = postfix.split(Constants.COMMA);
        final List<ResourceId> ids = new ArrayList<>(args.length);
        for (String arg : args) {
            ids.add(new ArgResourceId(prefix, trim(arg)));
        }

        return ids;
    }

    private static int computeIndex(final String name) {
        return name.contains(Constants.PERIOD_EN) ? name.lastIndexOf(Constants.PERIOD_EN) : name.length();
    }
}
