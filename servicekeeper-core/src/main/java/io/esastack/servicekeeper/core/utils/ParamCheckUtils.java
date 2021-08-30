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

public final class ParamCheckUtils {

    private static final float MAX_FAILURE_THRESHOLD = 100f;
    private static final float MIN_FAILURE_THRESHOLD = 0f;

    private ParamCheckUtils() {
    }

    public static void legalFailureThreshold(float failureThreshold, String message) {
        if (failureThreshold > MAX_FAILURE_THRESHOLD || failureThreshold < MIN_FAILURE_THRESHOLD) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void positiveInt(int number, String message) {
        if (number <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void positiveLong(long number, String message) {
        if (number <= 0L) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNegativeInt(int number, String message) {
        if (number < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNegativeLong(long number, String message) {
        if (number < 0L) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNegativeDouble(double number, String message) {
        if (number < 0.0d) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void isTrue(boolean value, String message) {
        if (!value) {
            throw new IllegalArgumentException(message);
        }
    }
}
