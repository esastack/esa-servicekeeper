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

import esa.commons.Checks;

import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableSet;

public final class RegexValue<C, K> {

    private final Pattern pattern;
    private final C config;
    private final CopyOnWriteArraySet<K> items;

    RegexValue(Pattern pattern, C config, CopyOnWriteArraySet<K> items) {
        Checks.checkNotNull(pattern, "RegexValue's pattern must not be null");
        Checks.checkNotNull(config, "RegexValue's config must not be null");
        Checks.checkNotNull(items, "RegexValue's items must not be null");
        this.pattern = pattern;
        this.config = config;
        this.items = items;
    }

    public C getConfig() {
        return config;
    }

    public Set<K> getItems() {
        return unmodifiableSet(items);
    }

    Pattern getPattern() {
        return pattern;
    }

    void addItem(K item) {
        items.add(item);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RegexValue.class.getSimpleName() + "[", "]")
                .add("pattern=" + pattern)
                .add("config=" + config)
                .add("items=" + items)
                .toString();
    }
}
