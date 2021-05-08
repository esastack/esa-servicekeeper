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
package esa.servicekeeper.core.internal;

import esa.servicekeeper.core.utils.SystemConfigUtils;

import java.util.concurrent.atomic.AtomicReference;

public class GlobalConfig {

    private static final String FALSE = "false";

    public static final String SERVICE_KEEPER_DISABLE_KEY = "servicekeeper.disable";
    public static final String ARG_KEEPER_ENABLE_KEY = "servicekeeper.arg.level.enable";
    public static final String RETRY_KEEPER_ENABLE_KEY = "servicekeeper.retry.enable";

    private final AtomicReference<Boolean> globalDisable = new AtomicReference<>();
    private final AtomicReference<Boolean> argLevelEnable = new AtomicReference<>();
    private final AtomicReference<Boolean> retryEnable = new AtomicReference<>();

    private static final boolean DEFAULT_GLOBAL = getGlobalDisable();
    private static final boolean ARG_LEVEL_ENABLE = getArgLevelEnable();
    private static final boolean RETRY_ENABLE = getRetryEnable();

    public boolean globalDisable() {
        final Boolean value = globalDisable.get();
        return value != null ? value : DEFAULT_GLOBAL;
    }

    public boolean argLevelEnable() {
        final Boolean value = argLevelEnable.get();
        return value != null ? value : ARG_LEVEL_ENABLE;
    }

    public boolean retryEnable() {
        final Boolean value = retryEnable.get();
        return value != null ? value : RETRY_ENABLE;
    }

    public void updateGlobalDisable(Boolean newValue) {
        globalDisable.updateAndGet((pre) -> newValue);
    }

    public void updateArgLevelEnable(Boolean newValue) {
        argLevelEnable.updateAndGet((pre) -> newValue);
    }

    public void updateRetryEnable(Boolean newValue) {
        retryEnable.updateAndGet((pre) -> newValue);
    }

    public static Boolean getGlobalDisable() {
        String value = SystemConfigUtils.getFromEnvAndProp(SERVICE_KEEPER_DISABLE_KEY);
        return value == null ? Boolean.FALSE : Boolean.valueOf(value);
    }

    private static Boolean getArgLevelEnable() {
        String value = SystemConfigUtils.getFromEnvAndProp(ARG_KEEPER_ENABLE_KEY);
        return !FALSE.equalsIgnoreCase(value);
    }

    private static Boolean getRetryEnable() {
        String value = SystemConfigUtils.getFromEnvAndProp(RETRY_KEEPER_ENABLE_KEY);
        return !FALSE.equalsIgnoreCase(value);
    }
}

