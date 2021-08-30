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
package io.esastack.servicekeeper.core.entry;

import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.config.ConcurrentLimitConfig;
import io.esastack.servicekeeper.core.config.RateLimitConfig;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.utils.ParameterUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.esastack.servicekeeper.core.configsource.MoatLimitConfigSource.getMaxSizeLimit;

public class CompositeServiceKeeperConfig {

    private final ServiceKeeperConfig methodConfig;
    private final ArgsServiceKeeperConfig argConfig;
    private final GroupResourceId group;

    public CompositeServiceKeeperConfig(ServiceKeeperConfig methodConfig,
                                        ArgsServiceKeeperConfig argConfig,
                                        GroupResourceId group) {
        this.methodConfig = methodConfig;
        this.argConfig = argConfig;
        this.group = group;
    }

    public static CompositeServiceKeeperConfigBuilder builder() {
        return new CompositeServiceKeeperConfigBuilder();
    }

    public ServiceKeeperConfig getMethodConfig() {
        return methodConfig;
    }

    public ArgsServiceKeeperConfig getArgConfig() {
        return argConfig;
    }

    public GroupResourceId getGroup() {
        return group;
    }

    public static final class CompositeServiceKeeperConfigBuilder {
        private ServiceKeeperConfig methodConfig;
        private GroupResourceId group;
        private Map<Integer, CompositeArgConfig> argConfigMap = new LinkedHashMap<>(4);

        private CompositeServiceKeeperConfigBuilder() {
        }

        public CompositeServiceKeeperConfigBuilder methodConfig(ServiceKeeperConfig methodConfig) {
            this.methodConfig = methodConfig;
            return this;
        }

        public CompositeServiceKeeperConfigBuilder group(GroupResourceId group) {
            this.group = group;
            return this;
        }

        public CompositeServiceKeeperConfigBuilder argConcurrentLimit(int index, Map<Object, Integer> thresholdMap) {
            return this.argConcurrentLimit(index, ParameterUtils.defaultName(index), thresholdMap);
        }

        public CompositeServiceKeeperConfigBuilder argConcurrentLimit(int index, Map<Object, Integer> thresholdMap,
                                                                      Integer maxValueSize) {
            return this.argConcurrentLimit(index, ParameterUtils.defaultName(index), thresholdMap, maxValueSize);
        }

        public CompositeServiceKeeperConfigBuilder argConcurrentLimit(int index,
                                                                      String argName,
                                                                      Map<Object, Integer> thresholdMap) {
            return argConcurrentLimit(index, argName, thresholdMap, null);
        }

        public CompositeServiceKeeperConfigBuilder argConcurrentLimit(int index, String argName,
                                                                      Map<Object, Integer> thresholdMap,
                                                                      Integer maxValueSize) {
            // thresholdMap is empty
            if (index < 0 || thresholdMap == null || thresholdMap.isEmpty()) {
                if (maxValueSize == null && ParameterUtils.defaultName(index).equals(argName)) {
                    return this;
                } else {
                    // Save argName and maxValueSize
                    CompositeArgConfig argConfig = argConfigMap.computeIfAbsent(index,
                            key -> new CompositeArgConfig(index, argName, null, null));
                    argConfig.setMaxConcurrentLimitSizeLimit(maxValueSize);
                    return this;
                }
            }

            // thresholdMap isn't empty
            CompositeArgConfig argConfig = argConfigMap.computeIfAbsent(index,
                    key -> new CompositeArgConfig(index, argName, null,
                            new LinkedHashMap<>(thresholdMap.size())));
            argConfig.setMaxConcurrentLimitSizeLimit(maxValueSize == null ? getMaxSizeLimit() : maxValueSize);

            final Map<Object, ServiceKeeperConfig> valueToConfig = argConfig.getValueToConfig();
            for (Map.Entry<Object, Integer> entry : thresholdMap.entrySet()) {
                ServiceKeeperConfig config;
                if ((config = valueToConfig.get(entry.getKey())) == null) {
                    valueToConfig.put(entry.getKey(), ServiceKeeperConfig.builder()
                            .concurrentLimiterConfig(ConcurrentLimitConfig.builder()
                                    .threshold(entry.getValue()).build())
                            .build());
                } else {
                    config.setConcurrentLimitConfig(ConcurrentLimitConfig.builder()
                            .threshold(entry.getValue()).build());
                }
            }

            return this;
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, Map<Object, Integer> thresholdMap) {
            return this.argRateLimitConfig(index, ParameterUtils.defaultName(index), null, thresholdMap);
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, Map<Object, Integer> thresholdMap,
                                                                      Integer maxValueSize) {
            return this.argRateLimitConfig(index, ParameterUtils.defaultName(index), null, thresholdMap,
                    maxValueSize);
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, String argName,
                                                                      final RateLimitConfig template,
                                                                      Map<Object, Integer> thresholdMap,
                                                                      Integer maxValueSize) {
            if (index < 0) {
                return this;
            }

            boolean isEmptyMap = thresholdMap == null || thresholdMap.isEmpty();
            // All are default and nothing to set.
            if (isEmptyMap
                    && ParameterUtils.defaultName(index).equals(argName)
                    && template == null
                    && maxValueSize == null) {
                return this;
            }

            if (isEmptyMap) {
                CompositeArgConfig argConfig = argConfigMap.get(index);
                if (argConfig == null) {
                    argConfig = argConfigMap.computeIfAbsent(index, (key) ->
                            new CompositeArgConfig(index, argName, template == null
                                    ? null : ServiceKeeperConfig.builder().rateLimiterConfig(template).build(),
                                    null));
                } else {
                    if (template != null) {
                        if (argConfig.getTemplate() == null) {
                            argConfig.setTemplate(ServiceKeeperConfig.builder().rateLimiterConfig(template).build());
                        } else {
                            argConfig.getTemplate().setRateLimitConfig(template);
                        }
                    }
                }
                argConfig.setMaxRateLimitSizeLimit(maxValueSize);
                return this;
            }

            // thresholdMap isn't empty
            CompositeArgConfig argConfig = argConfigMap.computeIfAbsent(index,
                    key -> new CompositeArgConfig(index, argName, template == null
                            ? null : ServiceKeeperConfig.builder().rateLimiterConfig(template).build(),
                            new LinkedHashMap<>(thresholdMap.size())));
            argConfig.setMaxRateLimitSizeLimit(maxValueSize == null ? getMaxSizeLimit() : maxValueSize);

            if (template != null) {
                if (argConfig.template == null) {
                    argConfig.setTemplate(ServiceKeeperConfig.builder().rateLimiterConfig(template).build());
                } else {
                    argConfig.template.setRateLimitConfig(template);
                }
            }

            RateLimitConfig rateLimitConfigTemplate = argConfig.template == null
                    || argConfig.template.getRateLimitConfig() == null
                    ? RateLimitConfig.ofDefault() : argConfig.template.getRateLimitConfig();
            final Map<Object, ServiceKeeperConfig> valueToConfig = this.argConfigMap.get(index).getValueToConfig();
            for (Map.Entry<Object, Integer> entry : thresholdMap.entrySet()) {
                if (valueToConfig.get(entry.getKey()) == null) {
                    valueToConfig.putIfAbsent(entry.getKey(), ServiceKeeperConfig.builder()
                            .rateLimiterConfig(RateLimitConfig.builder().limitForPeriod(entry.getValue())
                                    .limitRefreshPeriod(rateLimitConfigTemplate.getLimitRefreshPeriod())
                                    .build())
                            .build());
                } else {
                    final ServiceKeeperConfig config = valueToConfig.get(entry.getKey());
                    config.setRateLimitConfig(RateLimitConfig.builder().limitForPeriod(entry.getValue())
                            .limitRefreshPeriod(rateLimitConfigTemplate.getLimitRefreshPeriod())
                            .build());
                }
            }

            return this;
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, String argName,
                                                                      RateLimitConfig template,
                                                                      Map<Object, Integer> thresholdMap) {
            return this.argRateLimitConfig(index, argName, template, thresholdMap, null);
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, String argName,
                                                                      Map<Object, Integer> thresholdMap,
                                                                      Integer maxValueSize) {
            return this.argRateLimitConfig(index, argName, null, thresholdMap, maxValueSize);
        }

        public CompositeServiceKeeperConfigBuilder argRateLimitConfig(int index, String argName,
                                                                      Map<Object, Integer> thresholdMap) {
            return this.argRateLimitConfig(index, argName, null, thresholdMap);
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, Map<Object,
                Float> failureRateThresholdMap) {
            return this.argCircuitBreakerConfig(index, ParameterUtils.defaultName(index), null,
                    failureRateThresholdMap);
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, Map<Object,
                Float> failureRateThresholdMap, Integer maxValueSize) {
            return this.argCircuitBreakerConfig(index, ParameterUtils.defaultName(index), null,
                    failureRateThresholdMap, maxValueSize);
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, String argName,
                                                                           CircuitBreakerConfig template,
                                                                           Map<Object, Float> failureRateThresholdMap) {
            return argCircuitBreakerConfig(index, argName, template, failureRateThresholdMap, null);
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, String argName,
                                                                           final CircuitBreakerConfig template,
                                                                           Map<Object, Float> failureRateThresholdMap,
                                                                           Integer maxValueSize) {
            if (index < 0) {
                return this;
            }

            boolean isEmptyMap = failureRateThresholdMap == null || failureRateThresholdMap.isEmpty();
            // All are default and nothing to set.
            if (isEmptyMap
                    && template == null
                    && ParameterUtils.defaultName(index).equals(argName)
                    && maxValueSize == null) {
                return this;
            }

            if (isEmptyMap) {
                CompositeArgConfig argConfig = argConfigMap.get(index);
                if (argConfig == null) {
                    argConfig = argConfigMap.computeIfAbsent(index, (key) ->
                            new CompositeArgConfig(index, argName, template == null
                                    ? null : ServiceKeeperConfig.builder().circuitBreakerConfig(template).build(),
                                    null));
                } else {
                    if (template != null) {
                        if (argConfig.getTemplate() == null) {
                            argConfig.setTemplate(ServiceKeeperConfig.builder().circuitBreakerConfig(template).build());
                        } else {
                            argConfig.getTemplate().setCircuitBreakerConfig(template);
                        }
                    }
                }
                argConfig.setMaxCircuitBreakerSizeLimit(maxValueSize);
                return this;
            }

            // thresholdMap isn't empty
            CompositeArgConfig argConfig = argConfigMap.computeIfAbsent(index,
                    (key) -> new CompositeArgConfig(index, argName, template == null
                            ? null : ServiceKeeperConfig.builder().circuitBreakerConfig(template).build(),
                            new LinkedHashMap<>(failureRateThresholdMap.size())));
            argConfig.setMaxCircuitBreakerSizeLimit(maxValueSize == null ? getMaxSizeLimit() : maxValueSize);

            if (template != null) {
                if (argConfig.template == null) {
                    argConfig.setTemplate(ServiceKeeperConfig.builder().circuitBreakerConfig(template).build());
                } else {
                    argConfig.template.setCircuitBreakerConfig(template);
                }
            }

            CircuitBreakerConfig circuitBreakerConfigTemplate = argConfig.template == null
                    || argConfig.template.getCircuitBreakerConfig() == null
                    ? CircuitBreakerConfig.ofDefault() : argConfig.template.getCircuitBreakerConfig();

            final Map<Object, ServiceKeeperConfig> valueToConfig = this.argConfigMap.get(index).getValueToConfig();
            for (Map.Entry<Object, Float> entry : failureRateThresholdMap.entrySet()) {
                if (valueToConfig.get(entry.getKey()) == null) {
                    valueToConfig.putIfAbsent(entry.getKey(), ServiceKeeperConfig.builder()
                            .circuitBreakerConfig(build(entry.getValue(), circuitBreakerConfigTemplate))
                            .build());
                } else {
                    final ServiceKeeperConfig config = valueToConfig.get(entry.getKey());
                    config.setCircuitBreakerConfig(build(entry.getValue(), circuitBreakerConfigTemplate));
                }
            }

            return this;
        }

        private CircuitBreakerConfig build(final float failureRateThreshold, CircuitBreakerConfig template) {
            return CircuitBreakerConfig.builder()
                    .failureRateThreshold(failureRateThreshold)
                    .ringBufferSizeInClosedState(template.getRingBufferSizeInClosedState())
                    .ringBufferSizeInHalfOpenState(template.getRingBufferSizeInHalfOpenState())
                    .ignoreExceptions(template.getIgnoreExceptions())
                    .predicateStrategy(template.getPredicateStrategy())
                    .maxSpendTimeMs(template.getMaxSpendTimeMs())
                    .waitDurationInOpenState(template.getWaitDurationInOpenState()).build();
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, String argName,
                                                                           Map<Object, Float> failureRateThresholdMap) {
            return this.argCircuitBreakerConfig(index, argName, null, failureRateThresholdMap);
        }

        public CompositeServiceKeeperConfigBuilder argCircuitBreakerConfig(int index, String argName,
                                                                           Map<Object, Float> failureRateThresholdMap,
                                                                           Integer maxValueSize) {
            return this.argCircuitBreakerConfig(index, argName, null, failureRateThresholdMap, maxValueSize);
        }

        public CompositeServiceKeeperConfig build() {
            final List<CompositeArgConfig> argConfigs = new ArrayList<>(this.argConfigMap.size());
            this.argConfigMap.forEach((key, value) -> argConfigs.add(value));
            return new CompositeServiceKeeperConfig(methodConfig, new ArgsServiceKeeperConfig(argConfigs), group);
        }
    }

    public static class ArgsServiceKeeperConfig {
        /**
         * args' index ====> args' config
         */
        private final Map<Integer, CompositeArgConfig> argConfigMap = new ConcurrentHashMap<>(4);

        ArgsServiceKeeperConfig(List<CompositeArgConfig> argConfigs) {
            if (argConfigs == null || argConfigs.isEmpty()) {
                return;
            }
            for (CompositeArgConfig argConfig : argConfigs) {
                argConfigMap.putIfAbsent(argConfig.getIndex(), argConfig);
            }
        }

        public Map<Integer, CompositeArgConfig> getArgConfigMap() {
            return argConfigMap;
        }
    }

    public static class CompositeArgConfig {

        private final int index;
        private final String argName;
        private final Map<Object, ServiceKeeperConfig> valueToConfig = new LinkedHashMap<>(4);
        private ServiceKeeperConfig template;
        private Integer maxCircuitBreakerSizeLimit;
        private Integer maxConcurrentLimitSizeLimit;
        private Integer maxRateLimitSizeLimit;

        CompositeArgConfig(int index, String argName, ServiceKeeperConfig template,
                           Map<Object, ServiceKeeperConfig> valueToConfig) {
            this.index = index;
            this.argName = argName;
            this.template = template;
            if (valueToConfig != null && !valueToConfig.isEmpty()) {
                this.valueToConfig.putAll(valueToConfig);
            }
        }

        public int getIndex() {
            return index;
        }

        public String getArgName() {
            return argName;
        }

        public Map<Object, ServiceKeeperConfig> getValueToConfig() {
            return valueToConfig;
        }

        public ServiceKeeperConfig getTemplate() {
            return template;
        }

        void setTemplate(ServiceKeeperConfig template) {
            this.template = template;
        }

        public Integer getMaxCircuitBreakerSizeLimit() {
            return maxCircuitBreakerSizeLimit;
        }

        public void setMaxCircuitBreakerSizeLimit(Integer maxCircuitBreakerSizeLimit) {
            this.maxCircuitBreakerSizeLimit = maxCircuitBreakerSizeLimit;
        }

        public Integer getMaxConcurrentLimitSizeLimit() {
            return maxConcurrentLimitSizeLimit;
        }

        public void setMaxConcurrentLimitSizeLimit(Integer maxConcurrentLimitSizeLimit) {
            this.maxConcurrentLimitSizeLimit = maxConcurrentLimitSizeLimit;
        }

        public Integer getMaxRateLimitSizeLimit() {
            return maxRateLimitSizeLimit;
        }

        public void setMaxRateLimitSizeLimit(Integer maxRateLimitSizeLimit) {
            this.maxRateLimitSizeLimit = maxRateLimitSizeLimit;
        }
    }
}
