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
package esa.servicekeeper.core.moats.circuitbreaker.predicate;

import esa.servicekeeper.core.executionchain.Context;

/**
 * The predicateStrategy to predicate whether a original invocation is success or failure. The result is false that
 * means a failure result will be recorded in the ring buffer, else a success result will be recorded.
 */
@FunctionalInterface
public interface PredicateStrategy {

    /**
     * To predicate whether the original invocation is successful.
     *
     * @param ctx ctx
     * @return true if the invocation is predicated as successful, else in contrast.
     */
    boolean isSuccess(Context ctx);

}

