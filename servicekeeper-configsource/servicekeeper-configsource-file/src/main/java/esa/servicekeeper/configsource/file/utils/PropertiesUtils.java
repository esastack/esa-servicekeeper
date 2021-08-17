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
package esa.servicekeeper.configsource.file.utils;

import esa.commons.StringUtils;
import esa.servicekeeper.configsource.file.constant.ExternalConfigName;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;
import esa.servicekeeper.core.utils.LogUtils;
import esa.commons.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static esa.servicekeeper.configsource.constant.Constants.COLON;
import static esa.servicekeeper.configsource.constant.Constants.COMMA;
import static esa.servicekeeper.configsource.constant.Constants.FALSE;
import static esa.servicekeeper.configsource.constant.Constants.GROUP_CONFIG_PREFIX;
import static esa.servicekeeper.configsource.constant.Constants.MAP_FORMAT;
import static esa.servicekeeper.configsource.constant.Constants.PERIOD_EN;
import static esa.servicekeeper.configsource.file.utils.MaxSizeLimitUtils.toKey;
import static esa.servicekeeper.configsource.utils.ResourceIdUtils.parseWithSuffix;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;
import static esa.servicekeeper.core.internal.GlobalConfig.ARG_KEEPER_ENABLE_KEY;
import static esa.servicekeeper.core.internal.GlobalConfig.RETRY_KEEPER_ENABLE_KEY;
import static esa.servicekeeper.core.internal.GlobalConfig.SERVICE_KEEPER_DISABLE_KEY;

public final class PropertiesUtils {

    private static final Logger logger = LogUtils.logger();

    private static final Set<String> INTERNAL_CONFIG_NAMES = new HashSet<>(3);

    static {
        INTERNAL_CONFIG_NAMES.add(MAX_CIRCUIT_BREAKER_VALUE_SIZE);
        INTERNAL_CONFIG_NAMES.add(MAX_RATE_LIMIT_VALUE_SIZE);
        INTERNAL_CONFIG_NAMES.add(MAX_CONCURRENT_LIMIT_VALUE_SIZE);
    }

    private PropertiesUtils() {
    }

    /**
     * Get {resourceId, ExternalConfig} Map from properties.
     *
     * @param properties properties
     * @return config map
     */
    public static Map<ResourceId, ExternalConfig> configs(final Properties properties) {
        final Map<ResourceId, ExternalConfig> configMap = new ConcurrentHashMap<>(128);
        if (properties == null) {
            return configMap;
        }

        for (String name : properties.stringPropertyNames()) {
            final String trimmedName = StringUtils.trim(name);
            if (isGlobalConfig(trimmedName)) {
                continue;
            }

            final String stringConfigName = propName(trimmedName);
            final ExternalConfigName configName = ExternalConfigName.getByName(stringConfigName);
            if (configName == null) {
                if (!INTERNAL_CONFIG_NAMES.contains(stringConfigName)) {
                    logger.error("Unsupported config name: " + name);
                }
                continue;
            }

            final String configValue = StringUtils.trim(properties.getProperty(name));
            final Map<ResourceId, String> valueMap = new HashMap<>(8);
            if (isGroupConfig(trimmedName)) {
                valueMap.putIfAbsent(GroupResourceId.from(groupName(trimmedName)), configValue);
            } else if (isMethodConfig(configValue)) {
                valueMap.putIfAbsent(parseWithSuffix(trimmedName), configValue);
            } else {
                valueMap.putAll(parseToArgConfigs(parseWithSuffix(trimmedName), configValue));
            }

            for (Map.Entry<ResourceId, String> entry : valueMap.entrySet()) {
                ExternalConfig config = configMap.computeIfAbsent(entry.getKey(), (key) -> {
                    if (key instanceof GroupResourceId) {
                        return new ExternalGroupConfig();
                    } else {
                        return new ExternalConfig();
                    }
                });

                try {
                    setConfigValue(config, configName, entry.getValue());
                } catch (Exception ex) {
                    logger.error("Failed to parse {}'s {}, the original value: {}", entry.getKey().getName(),
                            configName, entry.getValue(), ex);
                }
            }
        }

        return filterArgTemplate(configMap);
    }

    public static Map<ArgConfigKey, Integer> maxSizeLimits(final Properties properties) {
        final Map<ArgConfigKey, Integer> maxSizeLimits = new ConcurrentHashMap<>(4);
        for (String name : properties.stringPropertyNames()) {
            final String trimmedName = StringUtils.trim(name);
            if (isGlobalConfig(trimmedName)) {
                continue;
            }

            final String stringConfigName = propName(trimmedName);
            if (MAX_CIRCUIT_BREAKER_VALUE_SIZE.equals(stringConfigName)
                    || MAX_CONCURRENT_LIMIT_VALUE_SIZE.equals(stringConfigName)
                    || MAX_RATE_LIMIT_VALUE_SIZE.equals(stringConfigName)) {
                try {
                    maxSizeLimits.putIfAbsent(toKey(trimmedName),
                            Integer.parseInt(properties.getProperty(name)));
                } catch (Exception ex) {
                    logger.error("Failed to parse {}'s {}, the original value: {}", name,
                            stringConfigName, properties.get(name), ex);
                }
            }
        }

        return maxSizeLimits;
    }

    /**
     * Whether the global disabled is true.
     *
     * @param properties target properties
     * @return true or false
     */
    public static Boolean getGlobalDisable(Properties properties) {
        String value = properties.getProperty(SERVICE_KEEPER_DISABLE_KEY);
        return value == null ? null : Boolean.valueOf(value);
    }

    /**
     * Whether the arg level is enable.
     *
     * @param properties target properties
     * @return true or false
     */
    public static Boolean getArgLevelEnable(Properties properties) {
        String value = properties.getProperty(ARG_KEEPER_ENABLE_KEY);
        return value == null ? null : !FALSE.equalsIgnoreCase(value);
    }

    /**
     * Whether the retry is enable.
     *
     * @param properties target properties
     * @return true or false
     */
    public static Boolean getRetryEnable(Properties properties) {
        String value = properties.getProperty(RETRY_KEEPER_ENABLE_KEY);
        return value == null ? null : !FALSE.equalsIgnoreCase(value);
    }

    /**
     * Set the config value
     *
     * @param config     externalConfig
     * @param configName configName
     * @param value      string value
     */
    private static void setConfigValue(ExternalConfig config, ExternalConfigName configName, String value) {
        if (configName != null) {
            configName.applyConfigValue(config, value);
        }
    }

    /**
     * Whether the propName is belongs to group config.
     *
     * @param propName propName
     * @return true or false
     */
    private static boolean isGroupConfig(String propName) {
        return StringUtils.isNotEmpty(propName) && propName.startsWith(GROUP_CONFIG_PREFIX);
    }

    /**
     * Whether is method level config.
     *
     * @param propValue prop Value
     * @return true or false
     */
    private static boolean isMethodConfig(String propValue) {
        String value = StringUtils.trim(propValue);
        if (value == null || value.isEmpty()) {
            return true;
        }
        return !(value.startsWith(MAP_FORMAT[0]) && value.endsWith(MAP_FORMAT[1]));
    }

    /**
     * Whether the config is global config.
     *
     * @param name propertyName
     * @return true of false
     */
    private static boolean isGlobalConfig(String name) {
        return SERVICE_KEEPER_DISABLE_KEY.equals(name) || ARG_KEEPER_ENABLE_KEY.equals(name)
                || RETRY_KEEPER_ENABLE_KEY.equals(name);
    }

    /**
     * Get the group name from fully propName. eg: group.demo.groupA.maxConcurrentLimit = 10 and the target
     * group name is: demo.groupA
     *
     * @param propName property name
     * @return group name
     */
    private static String groupName(String propName) {
        if (StringUtils.isEmpty(propName)) {
            return StringUtils.EMPTY_STRING;
        }
        return propName.substring(GROUP_CONFIG_PREFIX.length(), propName.lastIndexOf(PERIOD_EN));
    }

    /**
     * Get the property name from the fullString. eg: com.example.service.DemoClass.demoMethod.maxConcurrentLimit=20
     * and the target propName is: com.example.service.DemoClass.demoMethod.maxConcurrentLimit
     *
     * @param fullString full string
     * @return target propName
     */
    private static String propName(String fullString) {
        if (StringUtils.isEmpty(fullString)) {
            return StringUtils.EMPTY_STRING;
        }
        return fullString.substring(fullString.lastIndexOf(PERIOD_EN) + 1);
    }

    /**
     * Split args' config map. eg:  {LiSi:20, ZhangSan : 50, wangwu: 30}
     *
     * @param methodId  methodId
     * @param propValue propValue
     * @return config map
     */
    private static Map<ResourceId, String> parseToArgConfigs(final ResourceId methodId, final String propValue) {
        if (StringUtils.isEmpty(propValue)) {
            return Collections.emptyMap();
        }
        final String[] valueAndConfigs = propValue.substring(1, propValue.length() - 1).split(COMMA);
        final Map<ResourceId, String> configValues = new ConcurrentHashMap<>(valueAndConfigs.length);
        ArgResourceId argId;
        for (String valueAndConfig : valueAndConfigs) {
            String itemName = StringUtils.trim(valueAndConfig.substring(0, valueAndConfig.indexOf(COLON)));
            String itemValue = StringUtils.trim(valueAndConfig.substring(valueAndConfig.indexOf(COLON) + 1));

            if (methodId.isRegex()) {
                argId = new ArgResourceId(ResourceId.from(methodId.getName()), itemName, true);
            } else {
                argId = new ArgResourceId(methodId.getName(), itemName);
            }
            configValues.putIfAbsent(argId, itemValue);
        }
        return configValues;
    }

    /**
     * Filter config map.
     *
     * @param configMap original configMap
     * @return config map without template
     */
    private static Map<ResourceId, ExternalConfig> filterArgTemplate(final Map<ResourceId, ExternalConfig> configMap) {
        final Set<ResourceId> argTemplateIdsToRemove = new HashSet<>(36);
        final Map<ResourceId, ExternalConfig> filteredConfigMaps = new ConcurrentHashMap<>(configMap.size());
        for (Map.Entry<ResourceId, ExternalConfig> entry : configMap.entrySet()) {
            if (entry.getKey() instanceof ArgResourceId) {
                final ArgResourceId argResourceId = (ArgResourceId) entry.getKey();
                argTemplateIdsToRemove.add(argResourceId.getMethodAndArgId());
                final ExternalConfig argTemplate = configMap.get(argResourceId.getMethodAndArgId());
                if (argTemplate == null) {
                    filteredConfigMaps.putIfAbsent(argResourceId, entry.getValue());
                } else {
                    //If arg config template exists, try to fill argConfig with the template.
                    filteredConfigMaps.putIfAbsent(argResourceId, tryToFillArgConfigWithTemplate(argTemplate,
                            entry.getValue()));
                }
            } else {
                filteredConfigMaps.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        for (ResourceId resourceId : argTemplateIdsToRemove) {
            filteredConfigMaps.remove(resourceId);
        }
        return filteredConfigMaps;
    }

    /**
     * Combine argConfig with template
     *
     * @param template  template, which must not be null.
     * @param argConfig argConfig, which must not be null.
     * @return config after combined
     */
    static ExternalConfig tryToFillArgConfigWithTemplate(final ExternalConfig template,
                                                         final ExternalConfig argConfig) {
        // Fill argConfig's (RateLimitConfig) with template
        if (argConfig.getLimitRefreshPeriod() == null && template.getLimitRefreshPeriod() != null) {
            argConfig.setLimitRefreshPeriod(template.getLimitRefreshPeriod());
        }

        // Fill argConfig's (CircuitBreakerConfig) with template
        if (argConfig.getRingBufferSizeInClosedState() == null &&
                template.getRingBufferSizeInClosedState() != null) {
            argConfig.setRingBufferSizeInClosedState(template.getRingBufferSizeInClosedState());
        }
        if (argConfig.getRingBufferSizeInHalfOpenState() == null &&
                template.getRingBufferSizeInHalfOpenState() != null) {
            argConfig.setRingBufferSizeInHalfOpenState(template.getRingBufferSizeInHalfOpenState());
        }
        if (argConfig.getWaitDurationInOpenState() == null && template.getWaitDurationInOpenState() != null) {
            argConfig.setWaitDurationInOpenState(template.getWaitDurationInOpenState());
        }
        if (argConfig.getIgnoreExceptions() == null && template.getIgnoreExceptions() != null) {
            argConfig.setIgnoreExceptions(template.getIgnoreExceptions());
        }
        if (argConfig.getPredicateStrategy() == null && template.getPredicateStrategy() != null) {
            argConfig.setPredicateStrategy(template.getPredicateStrategy());
        }
        if (argConfig.getMaxSpendTimeMs() == null && template.getMaxSpendTimeMs() != null) {
            argConfig.setMaxSpendTimeMs(template.getMaxSpendTimeMs());
        }

        return argConfig;
    }

}
