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
package esa.servicekeeper.core.fallback;

import esa.commons.Checks;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import static esa.servicekeeper.core.fallback.FallbackHandler.FallbackType.FALLBACK_TO_EXCEPTION;

public class FallbackToException implements FallbackHandler {

    private static final Logger logger = LogUtils.logger();

    private final Exception ex;

    public FallbackToException(Exception ex) {
        Checks.checkNotNull(ex, "Fallback Exception must not be null");
        this.ex = ex;
    }

    @Override
    public Object handle(Context ctx) throws Throwable {
        if (logger.isDebugEnabled()) {
            logger.debug(ctx.getResourceId() + " fallback to exception: ", ex);
        }
        throw ex;
    }

    @Override
    public FallbackType getType() {
        return FALLBACK_TO_EXCEPTION;
    }

    @Override
    public String toString() {
        return "FallbackToException{" + "ex=" + ex +
                '}';
    }
}
