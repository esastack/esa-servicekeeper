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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationUtilsTest {

    @Test
    void testParse() {
        then(DurationUtils.parse("10ms")).isEqualTo(Duration.ofMillis(10));
        // Default suffix is ms
        then(DurationUtils.parse("10")).isEqualTo(Duration.ofSeconds(10));
        then(DurationUtils.parse("10ns")).isEqualTo(Duration.ofNanos(10));
        then(DurationUtils.parse("10us")).isEqualTo(Duration.of(10, ChronoUnit.MICROS));
        then(DurationUtils.parse("10m")).isEqualTo(Duration.ofMinutes(10));
        then(DurationUtils.parse("10s")).isEqualTo(Duration.ofSeconds(10));
        then(DurationUtils.parse("10h")).isEqualTo(Duration.ofHours(10));
        then(DurationUtils.parse("10d")).isEqualTo(Duration.ofDays(10));
        then(DurationUtils.parse("123s")).isEqualTo(Duration.ofSeconds(123));
        assertThrows(IllegalArgumentException.class, () -> DurationUtils.parse("10mm"));
    }

    @Test
    void testToString() {
        then(DurationUtils.toString(Duration.ofMillis(10))).isEqualTo("10ms");
        then(DurationUtils.toString(Duration.ofNanos(10))).isEqualTo("10ns");
        then(DurationUtils.toString(Duration.ofNanos(10000))).isEqualTo("10us");
        then(DurationUtils.toString(Duration.ofSeconds(10))).isEqualTo("10s");
        then(DurationUtils.toString(Duration.ofMinutes(10))).isEqualTo("10m");
        then(DurationUtils.toString(Duration.ofHours(10))).isEqualTo("10h");
        then(DurationUtils.parse("10d")).isEqualTo(Duration.ofDays(10));
        then(DurationUtils.parse("123s")).isEqualTo(Duration.ofSeconds(123));
        then(DurationUtils.toString(Duration.ofDays(10))).isEqualTo("10d");
    }
}
