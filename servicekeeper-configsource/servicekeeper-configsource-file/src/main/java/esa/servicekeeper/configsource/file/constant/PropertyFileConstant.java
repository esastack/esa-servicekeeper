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
package esa.servicekeeper.configsource.file.constant;

import esa.commons.StringUtils;
import esa.servicekeeper.core.utils.SystemConfigUtils;

import java.io.File;

public final class PropertyFileConstant {

    public static final String SERVICE_KEEPER_CONFIG_DIR = "servicekeeper.config.dir";

    public static final String SERVICE_KEEPER_CONFIG_NAME = "servicekeeper.config.name";

    private static final String DEFAULT_SERVICE_KEEPER_CONFIG_DIR = "./conf";

    private static final String DEFAULT_SERVICE_KEEPER_CONFIG_NAME = "service-keeper.properties";

    private static final String ABSOLUTE_PATH_PREFIX = "/";
    private static final Object LOCK = new Object();
    private static volatile String CONFIG_DIR;
    private static volatile String CONFIG_NAME;

    private PropertyFileConstant() {
    }

    /**
     * Get service keeper's config name
     *
     * @return config name
     */
    public static String configName() {
        if (CONFIG_NAME != null) {
            return CONFIG_NAME;
        }

        synchronized (LOCK) {
            if (CONFIG_NAME != null) {
                return CONFIG_NAME;
            }
            CONFIG_NAME = detectName();
        }
        return CONFIG_NAME;
    }

    /**
     * Update the config name, which is designed to help junit test.
     *
     * @param configName name
     */
    public static synchronized void configName(String configName) {
        CONFIG_NAME = configName;
    }

    /**
     * Get service-keeper.properties' directory.
     *
     * @return configuration's directory.
     */
    public static String configDir() {
        if (CONFIG_DIR != null) {
            return CONFIG_DIR;
        }
        synchronized (LOCK) {
            if (CONFIG_DIR != null) {
                return CONFIG_DIR;
            }
            CONFIG_DIR = detectDir();
        }
        return CONFIG_DIR;
    }

    /**
     * Update the config name, which is designed to help junit test.
     *
     * @param configDir configDir
     */
    public static synchronized void configDir(String configDir) {
        CONFIG_DIR = configDir;
    }

    private static String detectName() {
        String configName;
        if (StringUtils.isNotEmpty((configName = System.getenv(SERVICE_KEEPER_CONFIG_NAME)))) {
            return configName;
        }

        if (StringUtils.isNotEmpty((configName = System.getProperty(SERVICE_KEEPER_CONFIG_NAME)))) {
            return configName;
        }

        return DEFAULT_SERVICE_KEEPER_CONFIG_NAME;
    }

    private static String detectDir() {
        String defaultConfigDir = defaultDir();

        // Whether service-keeper.properties has existed in default config directory
        if (new File(defaultConfigDir, configName()).exists()) {
            return defaultConfigDir;
        }

        // Try to get config from src/main/resources
        String resourcesDir = System.getProperty("user.dir") + File.separator + "src" +
                File.separator + "main" + File.separator + "resources";
        if (new File(resourcesDir, configName()).exists()) {
            return resourcesDir;
        }

        return defaultConfigDir;
    }

    private static String defaultDir() {
        // Default to ./conf
        String configDir = DEFAULT_SERVICE_KEEPER_CONFIG_DIR;

        final String systemDir = SystemConfigUtils.getFromEnvAndProp(SERVICE_KEEPER_CONFIG_DIR);
        if (systemDir != null) {
            configDir = systemDir;
        }

        // Absolute path
        if (configDir.startsWith(ABSOLUTE_PATH_PREFIX)) {
            return configDir;
        }
        return System.getProperty("user.dir") + File.separator + configDir;
    }
}
