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
package io.esastack.servicekeeper.core.common;

@FunctionalInterface
public interface ResourceId {
    /**
     * Construct a resourceId using the given name.
     *
     * @param name    name
     * @param isRegex whether the resourceId is regex
     * @return return value.
     */
    static ResourceId from(String name, boolean isRegex) {
        return new ResourceId() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj || ((obj instanceof ResourceId) && ((ResourceId) obj).getName().equals(name))
                        && ((ResourceId) obj).isRegex() == isRegex;
            }

            @Override
            public String toString() {
                return name;
            }

            @Override
            public boolean isRegex() {
                return isRegex;
            }
        };
    }

    /**
     * Get the type of current resourceId
     *
     * @return resourceId
     */
    default Type getType() {
        return Type.COMMON;
    }

    /**
     * Construct a resourceId using the given name.
     *
     * @param name name
     * @return return value.
     */
    static ResourceId from(String name) {
        return from(name, false);
    }

    /**
     * Get the name of the resourceId
     *
     * @return name
     */
    String getName();

    /**
     * Whether the resourceId is regex
     *
     * @return true or false
     */
    default boolean isRegex() {
        return false;
    }

    enum Type {
        /**
         * Common ResourceId
         */
        COMMON,

        /**
         * Arg level ResourceId
         */
        ARG,

        /**
         * Group ResourceId
         */
        GROUP
    }
}
