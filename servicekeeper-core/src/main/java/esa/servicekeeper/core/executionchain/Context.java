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
package esa.servicekeeper.core.executionchain;

import esa.servicekeeper.core.exception.ServiceKeeperException;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;

import java.io.Serializable;

public abstract class Context implements Serializable {

    private static final long serialVersionUID = 69139775609353556L;

    private final String resourceId;
    private final transient Object[] args;

    private ServiceKeeperNotPermittedException throughFailsCause;

    public Context(String resourceId) {
        this(resourceId, null);
    }

    public Context(String resourceId, Object[] args) {
        this.resourceId = resourceId;
        this.args = args;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Object[] getArgs() {
        return args;
    }

    public ServiceKeeperException getThroughFailsCause() {
        return throughFailsCause;
    }

    /**
     * Get bizException
     *
     * @return bizException
     */
    public abstract Throwable getBizException();

    /**
     * Get result
     *
     * @return result
     */
    public abstract Object getResult();

    /**
     * Get spendTimeMs of original method's execution.
     *
     * @return maxSpendTimeMs
     */
    public abstract long getSpendTimeMs();

    /**
     * Get spendTimeMs
     *
     * @param spendTimeMs spendTimeMs
     */
    abstract void setSpendTimeMs(long spendTimeMs);

    /**
     * Set result
     *
     * @param result result
     */
    abstract void setResult(Object result);

    /**
     * Set bizException
     *
     * @param bizException bizException
     */
    abstract void setBizException(Throwable bizException);
}
