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
package esa.servicekeeper.configsource.cache;

import esa.commons.annotation.Beta;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import static esa.servicekeeper.core.utils.LogUtils.concatValue;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableMap;

@Beta
public final class RegexConfigCenter<C, K> {

    private static final Logger logger = LogUtils.logger();

    private static final Object DEFAULT_NULL_VALUE = null;

    /**
     * Caches the resourceId which not matches any of the patterns. In our design, the resourceId which matches any
     * pattern successfully can only matches one time, but the one which doesn't match any of patterns will try to
     * match frequently, which will cause performance degradation. So the {@link #cachedNullValueIds} is designed to
     * alleviate the problem.
     */
    private final Map<K, Object> cachedNullValueIds = new WeakHashMap<>(64);
    private final Object lock = new Object();
    private volatile Map<String, RegexValue<C, K>> regexValues = new ConcurrentHashMap<>(8);

    void addRegexConfig(String regex, C config) {
        final Pattern pattern = Pattern.compile(regex);
        final RegexValue<C, K> value = new RegexValue<>(pattern, config, new CopyOnWriteArraySet<>());
        regexValues.put(regex, value);
    }

    public C configOf(K key) {
        if (cachedNullValueIds.keySet().contains(key)) {
            return null;
        }

        RegexValue<C, K> value;
        for (Map.Entry<String, RegexValue<C, K>> entry : regexValues.entrySet()) {
            value = entry.getValue();
            if (value.pattern().matcher(key.toString()).matches()) {
                value.addItem(key);

                if (logger.isDebugEnabled()) {
                    logger.debug("Obtained {}'s config from regex center, config:{}, regex:{}",
                            key, value.config(), value.pattern().toString());
                }
                return value.config();
            }
        }

        synchronized (lock) {
            cachedNullValueIds.put(key, DEFAULT_NULL_VALUE);
            return null;
        }
    }

    Set<K> items(String regex) {
        for (Map.Entry<String, RegexValue<C, K>> entry : regexValues.entrySet()) {
            if (entry.getKey().equals(regex)) {
                return entry.getValue().items();
            }
        }

        return emptySet();
    }

    public Map<String, RegexValue<C, K>> getAll() {
        if (regexValues == null || regexValues.isEmpty()) {
            return emptyMap();
        }

        return unmodifiableMap(regexValues);
    }

    RegexValue<C, K> removeRegex(String regex) {
        RegexValue<C, K> valueToRemove = null;

        for (Map.Entry<String, RegexValue<C, K>> entry : regexValues.entrySet()) {
            if (entry.getKey().equals(regex)) {
                valueToRemove = entry.getValue();
                break;
            }
        }

        if (valueToRemove == null) {
            return null;
        }
        cachedNullValueIds.clear();
        return regexValues.remove(regex);
    }

    void updateRegexConfigs(Map<String, C> newConfigs) {
        Map<String, RegexValue<C, K>> newConfigsAfterComputing;
        if (newConfigs == null || newConfigs.isEmpty()) {
            newConfigsAfterComputing = emptyMap();
        } else {
            newConfigsAfterComputing = new HashMap<>(newConfigs.size());
            RegexValue<C, K> oldValue;
            for (Map.Entry<String, C> entry : newConfigs.entrySet()) {
                oldValue = regexValues.get(entry.getKey());
                if (null == oldValue) {
                    newConfigsAfterComputing.put(entry.getKey(), new RegexValue<>(Pattern.compile(entry.getKey()),
                            entry.getValue(), new CopyOnWriteArraySet<>()));
                } else {
                    newConfigsAfterComputing.put(entry.getKey(), new RegexValue<>(oldValue.pattern(),
                            entry.getValue(), new CopyOnWriteArraySet<>(oldValue.items())));
                }
            }
        }

        cachedNullValueIds.clear();
        this.regexValues = new ConcurrentHashMap<>(newConfigsAfterComputing);
        logger.info("Updated regex center successfully, newest configs: {}", concatValue(regexValues));
    }

    void updateRegexConfig(String regex, C newConfig) {
        if (newConfig == null) {
            removeRegex(regex);
            return;
        }

        RegexValue<C, K> oldValue = regexValues.get(regex);
        if (null == oldValue) {
            regexValues.put(regex, new RegexValue<>(Pattern.compile(regex),
                    newConfig, new CopyOnWriteArraySet<>()));

            final RegexValue<C, K> newValue = regexValues.get(regex);
            logger.info("Updated {}'s regex config successfully, config: {}, items: {}",
                    regex, newValue.config(), newValue.items());
        } else {
            regexValues.put(regex, new RegexValue<>(oldValue.pattern(),
                    newConfig, new CopyOnWriteArraySet<>(oldValue.items())));

            final RegexValue<C, K> newValue = regexValues.get(regex);
            logger.info("Updated {}'s regex config successfully, config: {}, items: {}",
                    regex, newValue.config(), newValue.items());
        }

        cachedNullValueIds.clear();
    }
}
