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
package esa.servicekeeper.core.listener;

import esa.servicekeeper.core.configsource.ExternalConfig;

public interface FondConfigListener<T> extends ExternalConfigListener {

    /**
     * Get concerned config of the component.
     *
     * @param config dynamic configuration.
     * @return configuration.
     */
    T getFond(ExternalConfig config);

    /**
     * Update itself by the dynamic configuration.
     *
     * @param config config
     */
    @Override
    default void onUpdate(ExternalConfig config) {
        T newestFondConfig = getFond(config);
        if (null == newestFondConfig) {
            updateWhenNewestConfigIsNull();
        } else {
            if (isConfigEquals(newestFondConfig)) {
                return;
            }
            updateWithNewestConfig(newestFondConfig);
        }
    }

    /**
     * Update with the concerned configuration.
     *
     * @param newestConfig config
     */
    void updateWithNewestConfig(T newestConfig);

    /**
     * Update when the newest dynamic config is null.
     */
    void updateWhenNewestConfigIsNull();

    /**
     * Whether the new config is different to the pre one.
     *
     * @param newestConfig the newest config.
     * @return true if the newConfig is different to the old one, else false.
     */
    boolean isConfigEquals(T newestConfig);

}

