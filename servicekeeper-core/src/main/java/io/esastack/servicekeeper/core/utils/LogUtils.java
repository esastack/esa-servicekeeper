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
package io.esastack.servicekeeper.core.utils;

import esa.commons.logging.Logger;
import esa.commons.logging.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.lang.System.lineSeparator;

public final class LogUtils {

    private static final Logger logger = LoggerFactory.getLogger("io.esastack.servicekeeper");

    private LogUtils() {
    }

    public static Logger logger() {
        return logger;
    }

    public static String concatValue(List<?> values) {
        final StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (values != null && !values.isEmpty()) {
            builder.append(lineSeparator());
            for (int i = 0; i < values.size() - 1; i++) {
                builder.append(values.get(i)).append(lineSeparator());
            }
            builder.append(values.get(values.size() - 1));
        }

        builder.append("]");
        return builder.toString();
    }

    public static String concatValue(Map<?, ?> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        if (values != null && !values.isEmpty()) {
            builder.append(lineSeparator());

            final int size = values.size();
            int index = 0;
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                builder.append(entry.getKey()).append(" : ").append(entry.getValue());
                if (++index < size) {
                    builder.append(lineSeparator());
                }
            }
        }

        builder.append("]");
        return builder.toString();
    }
}

