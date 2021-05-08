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

import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.exception.FallbackFailsException;
import esa.servicekeeper.core.exception.FallbackToExceptionWrapper;

public final class RequestHandleUtils {

    private RequestHandleUtils() {
    }

    /**
     * Handle the result of try fails.
     *
     * @param resourceId    resourceId.
     * @param requestHandle RequestHandle.
     * @return result
     */
    public static Object handle(String resourceId, RequestHandle requestHandle) throws Throwable {
        if (requestHandle.isFallbackSucceed()) {
            if (requestHandle.getFallbackResult() instanceof FallbackToExceptionWrapper) {
                throw ((FallbackToExceptionWrapper) requestHandle.getFallbackResult()).getCause();
            } else {
                return requestHandle.getFallbackResult();
            }
        }
        if (!(requestHandle.getFallbackFailsCause() instanceof RequestHandle.FallbackNotConfiguredException)) {
            final String msg = resourceId == null ? "Fallback fails" : "Fallback fails, resourceId: " + resourceId;
            throw new FallbackFailsException(msg, requestHandle.getFallbackFailsCause());
        } else {
            throw requestHandle.getNotAllowedCause();
        }
    }

    public static Object handle(RequestHandle requestHandle) throws Throwable {
        return handle(null, requestHandle);
    }

}
