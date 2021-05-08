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
package esa.servicekeeper.configsource.file;

import esa.servicekeeper.configsource.ConfigsHandler;
import esa.servicekeeper.configsource.ConfigsHandlerImpl;
import esa.servicekeeper.configsource.InternalsUpdater;
import esa.servicekeeper.configsource.RegexConfigsHandler;
import esa.servicekeeper.configsource.cache.ConfigCache;
import esa.servicekeeper.configsource.cache.ConfigCacheImp;
import esa.servicekeeper.configsource.cache.RegexConfigCache;
import esa.servicekeeper.core.utils.SystemConfigUtils;

import static esa.servicekeeper.configsource.constant.Constants.ENABLE_REGEX;
import static java.lang.Boolean.TRUE;

class SingletonFactory {

    private static final ConfigCache INTERNAL_CACHE;

    private static final PropertyFileConfigCache CACHE;
    private static volatile InternalConfigsHandler HANDLER;

    private static final boolean REGEX;

    static {
        final String value = SystemConfigUtils.getFromEnvAndProp(ENABLE_REGEX);

        if (TRUE.toString().equals(value)) {
            INTERNAL_CACHE = new RegexConfigCache();
            CACHE = new PropertyFileConfigCache(INTERNAL_CACHE);
            REGEX = true;
        } else {
            INTERNAL_CACHE = new ConfigCacheImp();
            CACHE = new PropertyFileConfigCache(INTERNAL_CACHE);
            REGEX = false;
        }
    }

    /**
     * Obtains singleton {@link ConfigCache} instance.
     *
     * @return cache
     */
    static PropertyFileConfigCache cache() {
        return CACHE;
    }

    static InternalConfigsHandler handler(InternalsUpdater updater) {
        if (HANDLER != null) {
            return HANDLER;
        }

        synchronized (SingletonFactory.class) {
            if (HANDLER != null) {
                return HANDLER;
            }

            ConfigsHandler handler;
            if (REGEX) {
                handler = new RegexConfigsHandler((RegexConfigCache) INTERNAL_CACHE, updater);
            } else {
                handler = new ConfigsHandlerImpl(INTERNAL_CACHE, updater);
            }
            HANDLER = new InternalConfigsHandler(handler, updater, CACHE);
            return HANDLER;
        }
    }

}
