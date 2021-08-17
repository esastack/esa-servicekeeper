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
package esa.servicekeeper.configsource;

import esa.servicekeeper.configsource.cache.RegexConfigCache;
import esa.servicekeeper.configsource.cache.RegexValue;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

public class RegexConfigsHandler extends ConfigsHandlerImpl {

    private static final Logger logger = LogUtils.logger();

    private final RegexConfigCache cache;

    public RegexConfigsHandler(RegexConfigCache cache, InternalsUpdater updater) {
        super(cache, updater);
        this.cache = cache;
    }

    @Override
    protected void doUpdate(ResourceId resourceId, ExternalConfig config) {
        if (resourceId.isRegex()) {
            doRegexUpdate(resourceId, config);
            return;
        }

        super.doUpdate(resourceId, config);
    }

    @Override
    protected RegexConfigCache getCache() {
        return cache;
    }

    private void doRegexUpdate(final ResourceId resourceId, final ExternalConfig config) {
        final RegexValue<ExternalConfig, ResourceId> value = cache.regexConfigOf(resourceId.getName());
        if (value == null) {
            logger.info("Begin to update regex items: [empty], config: {}", config);
            return;
        }

        logger.info("Begin to update regex items: [{}], config: {}", value.items(), config);
        value.items().forEach((id) -> super.doUpdate(id, config));
    }
}
