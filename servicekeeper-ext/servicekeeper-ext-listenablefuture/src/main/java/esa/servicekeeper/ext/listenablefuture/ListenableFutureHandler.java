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
package esa.servicekeeper.ext.listenablefuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import esa.commons.Checks;
import esa.servicekeeper.core.asynchandle.AsyncResultHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;

import static com.google.common.util.concurrent.Futures.getDone;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

public class ListenableFutureHandler<M> implements AsyncResultHandler<ListenableFuture<M>> {

    @Override
    public boolean supports(Class<?> returnType) {
        return ListenableFuture.class.isAssignableFrom(returnType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ListenableFuture<M> handle0(ListenableFuture<M> returnValue, RequestHandle requestHandle) {

        SettableFuture<M> future = SettableFuture.create();
        returnValue.addListener(
                () -> {
                    M v = null;
                    Throwable t = null;
                    try {
                        v = getDone(returnValue);
                    } catch (Throwable th) {
                        t = th;
                    }

                    if (t != null) {
                        try {
                            processFallback(future, (ListenableFuture<M>) (requestHandle.fallback(t)));
                        } catch (Throwable th) {
                            future.setException(th);
                        }
                    } else {
                        requestHandle.endWithResult(v);
                        future.set(v);
                    }
                }
                , directExecutor());

        return future;
    }

    private void processFallback(SettableFuture<M> resultFuture, ListenableFuture<M> fallbackValue) {
        Checks.checkNotNull(fallbackValue, "fallbackValue");
        fallbackValue.addListener(() -> {
                    try {
                        resultFuture.set(getDone(fallbackValue));
                    } catch (Throwable th) {
                        resultFuture.setException(th);
                    }
                }, directExecutor()
        );
    }

    @Override
    public String toString() {
        return "ListenableFutureHandler";
    }
}
