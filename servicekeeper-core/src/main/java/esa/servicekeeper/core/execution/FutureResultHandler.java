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
package esa.servicekeeper.core.execution;

import esa.servicekeeper.core.asynchandle.RequestHandle;

import java.util.concurrent.CompletionStage;

/**
 * @see esa.servicekeeper.core.asynchandle.CompletableStageHandler
 * @deprecated since 1.4.0
 */
@Deprecated
public class FutureResultHandler<M> implements AsyncResultHandler<CompletionStage<M>> {

    @Override
    public boolean supports(Class<?> returnType) {
        return CompletionStage.class.isAssignableFrom(returnType);
    }

    @Override
    public CompletionStage<M> handle(CompletionStage<M> returnValue, RequestHandle requestHandle) {
        returnValue.whenComplete((v, th) -> {
            if (th == null) {
                requestHandle.endWithResult(v);
            } else {
                requestHandle.endWithError(th);
            }
        });

        return returnValue;
    }
}
