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
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.configsource.constant.Constants;
import io.esastack.servicekeeper.configsource.file.constant.ExternalConfigName;
import io.esastack.servicekeeper.core.common.ArgConfigKey;
import io.esastack.servicekeeper.core.common.ArgResourceId;
import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.configsource.ExternalConfig;
import io.esastack.servicekeeper.core.configsource.ExternalGroupConfig;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.esastack.servicekeeper.configsource.file.utils.MaxSizeLimitUtils.toKey;
import static io.esastack.servicekeeper.configsource.utils.ResourceIdUtils.parseWithSuffix;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CIRCUIT_BREAKER_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_CONCURRENT_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.MAX_RATE_LIMIT_VALUE_SIZE;
import static io.esastack.servicekeeper.core.internal.GlobalConfig.ARG_KEEPER_ENABLE_KEY;
import static io.esastack.servicekeeper.core.internal.GlobalConfig.RETRY_KEEPER_ENABLE_KEY;
import static io.esastack.servicekeeper.core.internal.GlobalConfig.SERVICE_KEEPER_DISABLE_KEY;

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
            final ExternalConfigName configName = extractConfigName(trimmedName);
            if (configName == null) {
                continue;
            }
            combineToConfigMap(configName, extractValueMap(name, trimmedName, configName, properties), configMap);
        }

        //the standard usage is that args can only be configured in the form of map.
        //the following method is to be compatible with the previous usage of configuration.
        fillArgConfigsWithTemplate(configMap);
        return configMap;
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
        return value == null ? null : !Constants.FALSE.equalsIgnoreCase(value);
    }

    /**
     * Whether the retry is enable.
     *
     * @param properties target properties
     * @return true or false
     */
    public static Boolean getRetryEnable(Properties properties) {
        String value = properties.getProperty(RETRY_KEEPER_ENABLE_KEY);
        return value == null ? null : !Constants.FALSE.equalsIgnoreCase(value);
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
        return StringUtils.isNotEmpty(propName) && propName.startsWith(Constants.GROUP_CONFIG_PREFIX);
    }

    /**
     * Whether is method level config.
     *
     * @param propValue prop Value
     * @return true or false
     */
    private static boolean isNotMapConfig(String propValue) {
        String value = StringUtils.trim(propValue);
        if (value == null || value.isEmpty()) {
            return true;
        }
        return !(value.startsWith(Constants.MAP_FORMAT[0]) && value.endsWith(Constants.MAP_FORMAT[1]));
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
        return propName.substring(Constants.GROUP_CONFIG_PREFIX.length(), propName.lastIndexOf(Constants.PERIOD_EN));
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
        return fullString.substring(fullString.lastIndexOf(Constants.PERIOD_EN) + 1);
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
        final String[] valueAndConfigs = propValue.substring(1, propValue.length() - 1).split(Constants.COMMA);
        final Map<ResourceId, String> configValues = new ConcurrentHashMap<>(valueAndConfigs.length);
        ArgResourceId argId;
        for (String valueAndConfig : valueAndConfigs) {
            String itemName = StringUtils.trim(valueAndConfig.substring(0, valueAndConfig.indexOf(Constants.COLON)));
            String itemValue = StringUtils.trim(valueAndConfig.substring(valueAndConfig.indexOf(Constants.COLON) + 1));

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
     */
    private static void fillArgConfigsWithTemplate(final Map<ResourceId, ExternalConfig> configMap) {
        for (Map.Entry<ResourceId, ExternalConfig> entry : configMap.entrySet()) {
            if (entry.getKey() instanceof ArgResourceId) {
                final ArgResourceId argResourceId = (ArgResourceId) entry.getKey();
                final ExternalConfig argTemplate = configMap.get(argResourceId.getMethodAndArgId());
                if (argTemplate != null) {
                    tryToFillArgConfigWithTemplate(argTemplate, entry.getValue());
                }
            }
        }
    }

    private static void combineToConfigMap(ExternalConfigName configName, Map<ResourceId, String> valueMap,
                                           Map<ResourceId, ExternalConfig> configMap) {
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

    private static ExternalConfigName extractConfigName(final String trimmedName) {
        if (isGlobalConfig(trimmedName)) {
            return null;
        }

        final String stringConfigName = propName(trimmedName);
        final ExternalConfigName configName = ExternalConfigName.getByName(stringConfigName);
        if (configName == null) {
            if (!INTERNAL_CONFIG_NAMES.contains(stringConfigName)) {
                logger.error("Unsupported config name: " + trimmedName);
            }
        }
        return configName;
    }

    private static Map<ResourceId, String> extractValueMap(final String name, final String trimmedName,
                                                           final ExternalConfigName configName,
                                                           final Properties properties) {
        final String configValue = StringUtils.trim(properties.getProperty(name));
        final Map<ResourceId, String> valueMap = new HashMap<>(8);
        if (isGroupConfig(trimmedName)) {
            valueMap.putIfAbsent(GroupResourceId.from(groupName(trimmedName)), configValue);
        } else if (isNotMapConfig(configValue)) {
            valueMap.putIfAbsent(parseWithSuffix(trimmedName), configValue);
        } else {
            valueMap.putAll(parseToArgConfigs(parseWithSuffix(trimmedName), configValue));
        }
        return valueMap;
    }

    /**
     * Combine argConfig with template
     *
     * @param template  template, which must not be null.
     * @param argConfig argConfig, which must not be null.
     */
    static void tryToFillArgConfigWithTemplate(final ExternalConfig template,
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
        if (argConfig.getForcedDisabled() == null && template.getForcedDisabled() != null) {
            argConfig.setForcedDisabled(template.getForcedDisabled());
        }
        if (argConfig.getForcedOpen() == null && template.getForcedOpen() != null) {
            argConfig.setForcedOpen(template.getForcedOpen());
        }
    }

}
