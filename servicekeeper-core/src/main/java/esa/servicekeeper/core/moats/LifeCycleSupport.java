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
package esa.servicekeeper.core.moats;

public interface LifeCycleSupport {

    /**
     * Get the type of lifeCycle.
     *
     * @return the type of lifeCycle.
     */
    LifeCycleType lifeCycleType();

    /**
     * Should the component to be destroyed now.
     *
     * @return whether the component should be destroyed now.
     */
    default boolean shouldDelete() {
        return false;
    }

    enum LifeCycleType {
        /**
         * A permanent component exists all the time. When the corresponding dynamic config is null,
         * it will use the original config to replace them.
         */
        PERMANENT(1),

        /**
         * A temporary component exists only the corresponding dynamic config is not null.
         */
        TEMPORARY(0);

        private final int priority;

        LifeCycleType(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }
}
