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

import esa.servicekeeper.core.config.MoatConfig;
import esa.servicekeeper.core.exception.ServiceKeeperNotPermittedException;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.fallback.FallbackHandler;
import esa.servicekeeper.core.utils.Ordered;

/**
 * A moat is likely to a interceptor or a filter, which is usually used to protect target resource.
 * When you want to access the original resource, you must go through the {@link MoatCluster} corresponding with
 * the target resource one by one.
 */
public interface Moat<T> extends Ordered {

    /**
     * get current used config
     *
     * @return config
     */
    T config();

    /**
     * Try to through the component.
     *
     * @param ctx ctx
     * @return whether through successfully.
     */
    void tryThrough(Context ctx) throws ServiceKeeperNotPermittedException;

    /**
     * Exit current moat
     *
     * @param ctx ctx
     */
    void exit(Context ctx);

    /**
     * Get type
     *
     * @return type
     */
    MoatType type();

    /**
     * Obtains {@link MoatConfig}.
     *
     * @return config
     */
    MoatConfig config0();
}
