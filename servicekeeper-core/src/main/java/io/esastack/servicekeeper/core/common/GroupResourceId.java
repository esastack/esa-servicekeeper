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

import java.util.Objects;

public class GroupResourceId implements ResourceId {

    private final String name;

    public GroupResourceId(String name) {
        this.name = name;
    }

    public static GroupResourceId from(String name) {
        return new GroupResourceId(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return Type.GROUP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GroupResourceId that = (GroupResourceId) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "GroupResourceId{" + "name='" + name + '\'' +
                '}';
    }
}
