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

import java.util.Random;

public class RandomUtils {

    public static long randomLong() {
        while (true) {
            long temp = new Random().nextLong();
            if (temp >= 0L) {
                return temp;
            }
        }
    }

    public static float randomFloat(int bound) {
        while (true) {
            float temp = new Random().nextFloat();
            if (temp > 0f && temp < bound) {
                return temp;
            }
        }
    }

    public static int randomInt(int bound) {
        Random random = new Random();
        while (true) {
            int temp = random.nextInt(bound);
            if (temp > 0) {
                return temp;
            }
        }
    }
}
