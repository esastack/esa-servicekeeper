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
import esa.commons.logging.Logger;

import static esa.servicekeeper.core.fallback.FallbackHandler.FallbackType.FALLBACK_TO_VALUE;

public class FallbackToValue implements FallbackHandler<String> {

    private static final Logger logger = LogUtils.logger();

    private final String value;
    private final boolean alsoApplyToBizException;

    public FallbackToValue(String value, boolean alsoApplyToBizException) {
        Checks.checkNotNull(value, "value");
        this.value = value;
        this.alsoApplyToBizException = alsoApplyToBizException;
    }

    @Override
    public String handle(Context ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug(ctx.getResourceId() + " fallback to value: " + value);
        }
        return value;
    }

    @Override
    public FallbackType getType() {
        return FALLBACK_TO_VALUE;
    }

    @Override
    public boolean alsoApplyToBizException() {
        return alsoApplyToBizException;
    }

    @Override
    public String toString() {
        return "FallbackToValue{" + "value='" + value + '\'' +
                ", alsoApplyToBizException=" + alsoApplyToBizException +
                '}';
    }
}
