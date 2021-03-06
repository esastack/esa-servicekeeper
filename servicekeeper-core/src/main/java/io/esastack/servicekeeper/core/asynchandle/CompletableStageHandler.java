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
package io.esastack.servicekeeper.core.asynchandle;

import esa.commons.Checks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Handler which can handle {@link CompletionStage} result.
 */
public class CompletableStageHandler<M> implements AsyncResultHandler<CompletionStage<M>> {

    @Override
    public boolean supports(Class<?> returnType) {
        return CompletionStage.class.isAssignableFrom(returnType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<M> handle0(CompletionStage<M> returnValue, RequestHandle requestHandle) {
        final CompletableFuture<M> future = new CompletableFuture<>();
        returnValue.whenComplete((r, t) -> {
            if (t != null) {
                try {
                    processFallback(future, (CompletionStage<M>) (requestHandle.fallback(t)));
                } catch (Throwable th) {
                    future.completeExceptionally(th);
                }
            } else {
                requestHandle.endWithResult(r);
                future.complete(r);
            }
        });
        return future;
    }

    @Override
    public String toString() {
        return "CompletableStageHandler";
    }

    private void processFallback(CompletableFuture<M> resultFuture, CompletionStage<M> fallbackValue) {
        Checks.checkNotNull(fallbackValue, "fallbackValue");
        fallbackValue.whenComplete((fr, th) -> {
            if (th == null) {
                resultFuture.complete(fr);
            } else {
                resultFuture.completeExceptionally(th);
            }
        });
    }
}
