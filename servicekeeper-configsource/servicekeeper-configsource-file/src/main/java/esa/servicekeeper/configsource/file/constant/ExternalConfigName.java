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
import esa.servicekeeper.configsource.file.utils.GroupItemUtils;
import esa.servicekeeper.configsource.utils.ClassConvertUtils;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;
import esa.servicekeeper.core.utils.ClassCastUtils;
import esa.servicekeeper.core.utils.DurationUtils;
import esa.servicekeeper.core.utils.ParamCheckUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ExternalConfigName {

    /**
     * maxConcurrentLimit
     */
    MAX_CONCURRENT_LIMIT("maxConcurrentLimit") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final int maxConcurrentLimit = Integer.parseInt(value);
            ParamCheckUtils.notNegativeInt(maxConcurrentLimit,
                    this.buildErrorMsg("must not be an negative number", maxConcurrentLimit));
            config.setMaxConcurrentLimit(maxConcurrentLimit);
        }
    },

    /**
     * limitForPeriod
     */
    LIMIT_FOR_PERIOD("limitForPeriod") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final int limitForPeriod = Integer.parseInt(value);
            ParamCheckUtils.positiveInt(limitForPeriod,
                    this.buildErrorMsg("must greater than 0", limitForPeriod));
            config.setLimitForPeriod(Integer.valueOf(value));
        }
    },

    /**
     * limitRefreshPeriod
     */
    LIMIT_REFRESH_PERIOD("limitRefreshPeriod") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setLimitRefreshPeriod(DurationUtils.parse(value));
        }
    },

    /**
     * failureRateThreshold
     */
    FAILURE_RATE_THRESHOLD("failureRateThreshold") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final float failureRateThreshold = Float.parseFloat(value);
            ParamCheckUtils.legalFailureThreshold(failureRateThreshold,
                    this.buildErrorMsg("must between [0, 100]", failureRateThreshold));
            config.setFailureRateThreshold(Float.valueOf(value));
        }
    },

    /**
     * ringBufferSizeInClosedState
     */
    RING_BUFFER_SIZE_IN_CLOSED_STATE("ringBufferSizeInClosedState") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final int ringBufferSizeInClosedState = Integer.parseInt(value);
            ParamCheckUtils.positiveInt(ringBufferSizeInClosedState,
                    this.buildErrorMsg("must greater than 0", ringBufferSizeInClosedState));
            config.setRingBufferSizeInClosedState(Integer.valueOf(value));
        }
    },

    /**
     * ringBufferSizeInHalfOpenState
     */
    RING_BUFFER_SIZE_IN_HALF_OPEN_STATE("ringBufferSizeInHalfOpenState") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final int ringBufferSizeInHalfOpenState = Integer.parseInt(value);
            ParamCheckUtils.positiveInt(ringBufferSizeInHalfOpenState,
                    this.buildErrorMsg("must greater than 0", ringBufferSizeInHalfOpenState));
            config.setRingBufferSizeInHalfOpenState(Integer.valueOf(value));
        }
    },

    /**
     * waitDurationInOpenState
     */
    WAIT_DURATION_IN_OPEN_STATE("waitDurationInOpenState") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setWaitDurationInOpenState(DurationUtils.parse(value));
        }
    },

    /**
     * ignoreExceptions
     */
    IGNORE_EXCEPTIONS("ignoreExceptions") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setIgnoreExceptions(ClassCastUtils.cast(ClassConvertUtils.toClasses(value)));
        }
    },

    /**
     * maxSpendTimeMs
     */
    MAX_SPEND_TIME_MS("maxSpendTimeMs") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final long maxSpendTimeMs = Long.parseLong(value);
            ParamCheckUtils.positiveLong(maxSpendTimeMs,
                    this.buildErrorMsg("must greater than 0", maxSpendTimeMs));
            config.setMaxSpendTimeMs(Long.valueOf(value));
        }
    },

    /**
     * forcedOpen
     */
    FORCED_OPEN("forcedOpen") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setForcedDisabled(null);
            config.setForcedOpen(Boolean.valueOf(value));
        }
    },

    /**
     * forcedDisabled
     */
    FORCED_DISABLED("forcedDisabled") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setForcedOpen(null);
            config.setForcedDisabled(Boolean.valueOf(value));
        }
    },

    /**
     * predicateStrategy
     */
    PREDICATE_STRATEGY("predicateStrategy") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setPredicateStrategy(ClassCastUtils.cast(ClassConvertUtils.toClasses(value)[0]));
        }
    },

    /**
     * fallbackMethod
     */
    FALLBACK_METHOD("fallbackMethod") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setFallbackMethodName(value);
        }
    },

    /**
     * fallbackClass
     */
    FALLBACK_CLASS("fallbackClass") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setFallbackClass(ClassConvertUtils.toClasses(value)[0]);
        }
    },

    /**
     * fallbackValue
     */
    FALLBACK_VALUE("fallbackValue") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setFallbackValue(value);
        }
    },

    /**
     * fallbackExceptionClass
     */
    FALLBACK_EXCEPTION_CLASS("fallbackExceptionClass") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setFallbackExceptionClass(ClassCastUtils.cast(ClassConvertUtils.toClasses(value)[0]));
        }
    },

    /**
     * alsoApplyToBizException
     */
    ALSO_APPLY_FALLBACK_TO_BIZ_EXCEPTION("alsoApplyFallbackToBizException") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setAlsoApplyFallbackToBizException(
                    Boolean.valueOf(value));
        }
    },

    /**
     * includeExceptions
     */
    RETRY_INCLUDE_EXCEPTIONS("includeExceptions") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setIncludeExceptions(ClassCastUtils.cast(ClassConvertUtils.toClasses(value)));
        }
    },

    /**
     * excludeExceptions
     */
    RETRY_EXCLUDE_EXCEPTIONS("excludeExceptions") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            config.setExcludeExceptions(ClassCastUtils.cast(ClassConvertUtils.toClasses(value)));
        }
    },

    /**
     * maxAttempts
     */
    RETRY_MAX_ATTEMPTS("maxAttempts") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final int maxAttempts = Integer.parseInt(value);
            ParamCheckUtils.notNegativeInt(maxAttempts,
                    this.buildErrorMsg("must not be an negative number", maxAttempts));
            config.setMaxAttempts(maxAttempts);
        }
    },

    /**
     * delay
     */
    RETRY_DELAY("delay") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final long delay = Long.parseLong(value);
            ParamCheckUtils.notNegativeLong(delay,
                    this.buildErrorMsg("must not be an negative number", delay));
            config.setDelay(delay);
        }
    },

    /**
     * maxDelay
     */
    RETRY_MAX_DELAY("maxDelay") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final long maxDelay = Long.parseLong(value);
            ParamCheckUtils.notNegativeLong(maxDelay,
                    this.buildErrorMsg("must not be an negative number", maxDelay));
            config.setMaxDelay(maxDelay);
        }
    },

    /**
     * multiplier
     */
    RETRY_MULTIPLIER("multiplier") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            final double multiplier = Double.parseDouble(value);
            ParamCheckUtils.notNegativeDouble(multiplier,
                    this.buildErrorMsg("must not be an negative number", multiplier));
            config.setMultiplier(multiplier);
        }
    },

    /**
     * items
     */
    GROUP_ITEMS("items") {
        @Override
        public void applyConfigValue(ExternalConfig config, String value) {
            if (!(config instanceof ExternalGroupConfig)) {
                return;
            }
            if (StringUtils.isEmpty(value)) {
                return;
            }
            ExternalGroupConfig groupConfig = (ExternalGroupConfig) config;
            groupConfig.setItems(GroupItemUtils.parseToItems(value));
        }
    };

    public static final Map<String, ExternalConfigName> CONFIG_NAME_MAP = new ConcurrentHashMap<>(values().length);

    static {
        Arrays.stream(values()).forEach((configName) -> CONFIG_NAME_MAP.putIfAbsent(configName.name, configName));
    }

    private final String name;

    ExternalConfigName(String name) {
        this.name = name;
    }

    public static ExternalConfigName getByName(String name) {
        return CONFIG_NAME_MAP.get(name);
    }

    public String getName() {
        return name;
    }

    public abstract void applyConfigValue(ExternalConfig config, String value);

    /**
     * build error message with a template
     *
     * @param message      tips
     * @param currentValue current value
     * @return error msg
     */
    protected String buildErrorMsg(String message, Object currentValue) {
        return this.getName() + " " + message + " current value : " + currentValue;
    }

    @Override
    public String toString() {
        return name;
    }
}
