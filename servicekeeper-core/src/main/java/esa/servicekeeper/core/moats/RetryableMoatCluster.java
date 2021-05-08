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

import esa.servicekeeper.core.retry.RetryableExecutor;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;

public class RetryableMoatCluster extends MoatClusterImpl {

    private final AtomicReference<RetryableExecutor> retryable;

    public RetryableMoatCluster(List<Moat<?>> moats, List<MoatClusterListener> listeners,
                                RetryableExecutor retryable) {
        super(moats, listeners);
        this.retryable = new AtomicReference<>(retryable);
    }

    public RetryableExecutor retryExecutor() {
        return retryable.get();
    }

    public void updateRetryExecutor(RetryableExecutor executor) {
        retryable.updateAndGet(item -> executor);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetryableMoatCluster.class.getSimpleName() + "[", "]")
                .add("retryable=" + retryable).add(",")
                .add("moats=" + getAll())
                .toString();
    }
}
