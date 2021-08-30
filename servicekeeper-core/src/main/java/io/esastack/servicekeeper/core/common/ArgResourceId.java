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

import esa.commons.Checks;

public class ArgResourceId implements ResourceId {

    private final ResourceId methodId;
    private final String argName;
    private final Object argValue;
    private final String name;
    private final boolean isRegex;

    public ArgResourceId(ResourceId methodId, String argName, Object argValue) {
        this(methodId, argName, argValue, false);
    }

    public ArgResourceId(ResourceId methodId, String argName, Object argValue, boolean isRegex) {
        this.methodId = methodId;
        this.argName = argName;
        this.argValue = argValue;
        this.name = methodId.getName() + "." + argName + "." + argValue.toString();
        this.isRegex = isRegex;
    }

    public ArgResourceId(String methodAndArgName, Object argValue) {
        this(methodAndArgName, argValue, false);
    }

    public ArgResourceId(String methodAndArgName, Object argValue, boolean isRegex) {
        Checks.checkNotEmptyArg(methodAndArgName, "Composite string of method and arg name must not be empty!");
        this.methodId = ResourceId.from(methodAndArgName.substring(0,
                methodAndArgName.lastIndexOf(".")));
        this.argName = methodAndArgName.substring(methodAndArgName.lastIndexOf(".") + 1);
        this.argValue = argValue;
        this.name = methodAndArgName + "." + argValue;
        this.isRegex = isRegex;
    }

    public ResourceId getMethodId() {
        return methodId;
    }

    public ResourceId getMethodAndArgId() {
        return ResourceId.from(methodId.getName() + "." + argName);
    }

    public String getArgName() {
        return argName;
    }

    public Object getArgValue() {
        return argValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isRegex() {
        return isRegex;
    }

    @Override
    public Type getType() {
        return Type.ARG;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResourceId)) {
            return false;
        }

        ResourceId that = (ResourceId) o;
        return name.equals(that.getName()) && isRegex == that.isRegex();
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
