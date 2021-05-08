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

import esa.commons.Checks;
import esa.commons.StringUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([a-zA-Z]{0,2})$");

    private static final int SIXTY = 60;
    private static final int TWENTY_FOUR = 24;
    private static final int THOUSAND = 1000;
    private static final int MILLION = 1000000;

    private DurationUtils() {
    }

    public static Duration parse(String value) {
        Checks.checkNotEmptyArg(value, "Duration's string value must not be empty");
        try {
            Matcher matcher = DURATION_PATTERN.matcher(value);
            ParamCheckUtils.isTrue(matcher.matches(), "Parse the string duration fails, original value: " +
                    value);

            String suffix = matcher.group(2);
            return (StringUtils.isNotEmpty(suffix) ? fromSuffix(suffix) : Unit.SECONDS).parse(matcher.group(1));
        } catch (Exception ex) {
            throw new IllegalArgumentException("[" + value + "] is not a valid string duration", ex);
        }
    }

    public static String toString(Duration duration) {
        if (duration == null) {
            return null;
        }
        long seconds = duration.getSeconds();
        if (seconds <= 0) {
            int nano = duration.getNano();
            if (nano >= MILLION) {
                return nano / MILLION + "ms";
            } else if (nano >= THOUSAND) {
                return nano / THOUSAND + "us";
            } else if (nano >= 0) {
                return nano + "ns";
            }
            throw new IllegalArgumentException("Illegal duration:" + duration);
        }
        if (seconds >= SIXTY * SIXTY * TWENTY_FOUR && seconds % SIXTY * SIXTY * TWENTY_FOUR == 0) {
            return (seconds / (SIXTY * SIXTY * TWENTY_FOUR)) + "d";
        } else if (seconds >= SIXTY * SIXTY && seconds % SIXTY * SIXTY == 0) {
            return (seconds / (SIXTY * SIXTY)) + "h";
        } else if (seconds >= SIXTY && seconds % SIXTY == 0) {
            return (seconds / SIXTY) + "m";
        } else {
            return seconds + "s";
        }
    }

    private static Unit fromSuffix(String suffix) {
        for (Unit candidate : Unit.values()) {
            if (candidate.suffix.equalsIgnoreCase(suffix)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown unit '" + suffix + "'");
    }

    /**
     * Units that we support.
     */
    private enum Unit {

        /**
         * Nanoseconds.
         */
        NANOS(ChronoUnit.NANOS, "ns"),

        /**
         * Microseconds.
         */
        MICROS(ChronoUnit.MICROS, "us"),

        /**
         * Milliseconds.
         */
        MILLIS(ChronoUnit.MILLIS, "ms"),

        /**
         * Seconds.
         */
        SECONDS(ChronoUnit.SECONDS, "s"),

        /**
         * Minutes.
         */
        MINUTES(ChronoUnit.MINUTES, "m"),

        /**
         * Hours.
         */
        HOURS(ChronoUnit.HOURS, "h"),

        /**
         * Days.
         */
        DAYS(ChronoUnit.DAYS, "d");

        private final ChronoUnit chronoUnit;
        private final String suffix;

        Unit(ChronoUnit chronoUnit, String suffix) {
            this.chronoUnit = chronoUnit;
            this.suffix = suffix;
        }

        private Duration parse(String value) {
            return Duration.of(Long.valueOf(value), this.chronoUnit);
        }
    }
}
