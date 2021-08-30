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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimerLogger {

    private static final Logger logger = LogUtils.logger();
    private static final long LOG_PERIOD = TimeUnit.SECONDS.toNanos(30L);

    private final AtomicLong lastLogTime = new AtomicLong(0L);

    public void logPeriodically(String message, Object... objects) {
        if (canLogRateNow()) {
            logger.warn(message, objects);
        }
    }

    private boolean canLogRateNow() {
        long timestamp = System.nanoTime();
        if (timestamp - lastLogTime.get() > LOG_PERIOD) {
            lastLogTime.set(timestamp);
            return true;
        }
        return false;
    }


}
