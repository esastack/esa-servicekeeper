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
package esa.servicekeeper.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.lineSeparator;

public final class LogUtils {

    private static final Logger logger = LoggerFactory.getLogger("esa.servicekeeper");

    private static final long LOG_PERIOD = TimeUnit.SECONDS.toNanos(30L);
    private static final LogUtils INSTANCE = new LogUtils();
    private final AtomicLong lastRateLogTime = new AtomicLong(0L);
    private final AtomicLong lastConcurrentLogTime = new AtomicLong(0L);
    private final AtomicLong lastCircuitBreakerLogTime = new AtomicLong(0L);

    private LogUtils() {
    }

    public static Logger logger() {
        return logger;
    }

    public static void logRatePeriodically(String message, Object... objects) {
        if (canLogRateNow()) {
            logger.warn(message, objects);
        }
    }

    public static void logConcurrentPeriodically(String message, Object... objects) {
        if (canLogConcurrentNow()) {
            logger.warn(message, objects);
        }
    }

    public static void logCircuitBreakerPeriodically(String message, Object... objects) {
        if (canLogCircuitBreakerNow()) {
            logger.warn(message, objects);
        }
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

    private static boolean canLogRateNow() {
        long timestamp = System.nanoTime();
        if (timestamp - INSTANCE.lastRateLogTime.get() > LOG_PERIOD) {
            INSTANCE.lastRateLogTime.lazySet(timestamp);
            return true;
        }
        return false;
    }

    private static boolean canLogConcurrentNow() {
        long timestamp = System.nanoTime();
        if (timestamp - INSTANCE.lastConcurrentLogTime.get() > LOG_PERIOD) {
            INSTANCE.lastConcurrentLogTime.lazySet(timestamp);
            return true;
        }
        return false;
    }

    private static boolean canLogCircuitBreakerNow() {
        long timestamp = System.nanoTime();
        if (timestamp - INSTANCE.lastCircuitBreakerLogTime.get() > LOG_PERIOD) {
            INSTANCE.lastCircuitBreakerLogTime.lazySet(timestamp);
            return true;
        }
        return false;
    }
}

