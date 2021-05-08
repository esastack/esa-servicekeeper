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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class SpiUtils {

    private SpiUtils() {
    }

    /**
     * loader the highest Priority instance, lower order means higher Priority
     *
     * @param clazz class to load
     * @param <T>   type
     * @return instance
     */
    public static <T extends Ordered> T loadByPriority(Class<T> clazz) {
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        T result = null;
        for (T instance : loader) {
            if (instance == null) {
                continue;
            }
            if (result == null || instance.getOrder() < result.getOrder()) {
                result = instance;
            }
        }

        return result;
    }

    /**
     * simple load instance by SPI
     *
     * @param clazz class to load
     * @param <T>   type
     * @return instance list
     */
    public static <T> List<T> loadAll(Class<T> clazz) {
        List<T> list = new ArrayList<>();
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        for (T instance : loader) {
            if (instance != null) {
                list.add(instance);
            }
        }
        return list;
    }
}
