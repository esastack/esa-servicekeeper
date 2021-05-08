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
package esa.servicekeeper.ext.nettyfuture;

import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import io.netty.util.concurrent.Future;

public class NettyFutureHandler<M> implements AsyncResultHandler<Future<M>> {

    @Override
    public boolean supports(Class<?> returnType) {
        return Future.class.isAssignableFrom(returnType);
    }

    @Override
    public Future<M> handle0(Future<M> returnValue, RequestHandle requestHandle) {
        returnValue.addListener(f -> {
            if (f.isCancellable()) {
                requestHandle.endWithSuccess();
                return;
            }

            if (f.isSuccess()) {
                requestHandle.endWithResult(f.getNow());
            } else {
                requestHandle.endWithError(f.cause());
            }
        });

        return returnValue;
    }

    @Override
    public String toString() {
        return "NettyFutureHandler";
    }
}
