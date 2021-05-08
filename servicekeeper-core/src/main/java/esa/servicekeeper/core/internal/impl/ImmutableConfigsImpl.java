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
package esa.servicekeeper.core.internal.impl;

import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.moats.MoatType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static esa.commons.StringUtils.isEmpty;
import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

public class ImmutableConfigsImpl implements ImmutableConfigs {

    private final Map<ResourceId, CompositeServiceKeeperConfig> configs = new ConcurrentHashMap<>(64);

    @Override
    public Object getConfig(ResourceId resourceId, ConfigType type) {
        if (resourceId == null) {
            return null;
        }

        if (ArgResourceId.Type.ARG.equals(resourceId.getType())) {
            return getArgConfig((ArgResourceId) resourceId, type);
        } else {
            return getMethodConfig(resourceId, type);
        }
    }

    @Override
    public GroupResourceId getGroupId(ResourceId resourceId) {
        if (resourceId == null) {
            return null;
        }

        CompositeServiceKeeperConfig config = configs.get(resourceId);
        return config == null ? null : config.getGroup();
    }

    @Override
    public Set<ResourceId> getGroupItems(GroupResourceId groupId) {
        if (groupId == null) {
            return emptySet();
        }

        Set<ResourceId> groupItems = new HashSet<>(1);
        configs.forEach((key, value) -> {
            if (groupId.equals(value.getGroup())) {
                groupItems.add(key);
            }
        });

        return unmodifiableSet(groupItems);
    }

    @Override
    public Integer getMaxSizeLimit(final ResourceId methodId, String argName, MoatType type) {
        if (methodId == null || isEmpty(argName)) {
            return null;
        }
        CompositeServiceKeeperConfig config = configs.get(methodId);
        if (config == null) {
            return null;
        }
        CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argConfigs = config.getArgConfig();

        Map<Integer, CompositeServiceKeeperConfig.CompositeArgConfig> argConfigMap;
        if (argConfigs == null || (argConfigMap = argConfigs.getArgConfigMap()).isEmpty()) {
            return null;
        }

        for (Map.Entry<Integer, CompositeServiceKeeperConfig.CompositeArgConfig> argConfig : argConfigMap.entrySet()) {
            final CompositeServiceKeeperConfig.CompositeArgConfig configValue = argConfig.getValue();
            if (argName.equals(configValue.getArgName())) {
                switch (type) {
                    case CONCURRENT_LIMIT:
                        return configValue.getMaxConcurrentLimitSizeLimit();
                    case RATE_LIMIT:
                        return configValue.getMaxRateLimitSizeLimit();
                    case CIRCUIT_BREAKER:
                        return configValue.getMaxCircuitBreakerSizeLimit();
                    default:
                        return null;
                }
            }
        }

        return null;
    }

    @Override
    public CompositeServiceKeeperConfig getOrCompute(ResourceId resourceId,
                                                     Supplier<CompositeServiceKeeperConfig> immutableConfig) {

        return configs.computeIfAbsent(resourceId, (key) -> immutableConfig == null ? null : immutableConfig.get());
    }

    private Object getArgConfig(ArgResourceId argId, ConfigType type) {
        CompositeServiceKeeperConfig compositeConfig = configs.get(argId.getMethodId());
        CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig;
        if (compositeConfig == null || (argsConfig = compositeConfig.getArgConfig()) == null) {
            return null;
        }

        Map<Integer, CompositeServiceKeeperConfig.CompositeArgConfig> argConfigsMap = argsConfig.getArgConfigMap();
        if (argConfigsMap == null) {
            return null;
        }
        for (Map.Entry<Integer, CompositeServiceKeeperConfig.CompositeArgConfig> entry : argConfigsMap.entrySet()) {
            CompositeServiceKeeperConfig.CompositeArgConfig argConfig = entry.getValue();
            if (argConfig == null || !argId.getArgName().equals(argConfig.getArgName())) {
                continue;
            }

            Map<Object, ServiceKeeperConfig> valueMaps = argConfig.getValueToConfig();

            ServiceKeeperConfig config;
            return getConfigByType(type, valueMaps == null
                    ? argConfig.getTemplate() :
                    (config = valueMaps.get(argId.getArgValue())) == null ? valueMaps.get(VALUE_MATCH_ALL) : config);
        }

        return null;
    }

    private Object getMethodConfig(ResourceId resourceId, ConfigType type) {
        CompositeServiceKeeperConfig compositeConfig = configs.get(resourceId);
        ServiceKeeperConfig config;
        if (compositeConfig == null || (config = compositeConfig.getMethodConfig()) == null) {
            return null;
        }

        return getConfigByType(type, config);
    }

    private Object getConfigByType(ConfigType type, ServiceKeeperConfig config) {
        if (config == null) {
            return null;
        }

        switch (type) {
            case RATELIMIT_CONFIG:
                return config.getRateLimitConfig();
            case CIRCUITBREAKER_CONFIG:
                return config.getCircuitBreakerConfig();
            case CONCURRENTLIMIT_CONFIG:
                return config.getConcurrentLimitConfig();
            case FALLBACK_CONFIG:
                return config.getFallbackConfig();
            case RETRY_CONFIG:
                return config.getRetryConfig();
            default:
                return null;
        }
    }
}

