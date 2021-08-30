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
package io.esastack.servicekeeper.core.executionchain;

import io.esastack.servicekeeper.core.fallback.FallbackHandler;
import io.esastack.servicekeeper.core.moats.Moat;

import java.util.List;

import static java.lang.System.nanoTime;

public class SyncExecutionChainImpl extends AbstractExecutionChain {

    private long startTimeNs;
    private long endTimeNs;
    private int currentIndex;

    public SyncExecutionChainImpl(List<Moat<?>> moats, FallbackHandler<?> fallbackHandler) {
        super(moats, fallbackHandler);
    }

    @Override
    protected void recordStartTime() {
        startTimeNs = nanoTime();
    }

    @Override
    protected void recordEndTime() {
        endTimeNs = nanoTime();
    }

    @Override
    protected long getStartTime() {
        return startTimeNs;
    }

    @Override
    protected long getEndTime() {
        return endTimeNs;
    }

    @Override
    protected long getSpendTimeMs() {
        return (endTimeNs - startTimeNs) / 1_000_000;
    }

    @Override
    protected int getCurrentIndex() {
        return currentIndex;
    }

    @Override
    protected void setCurrentIndex(int index) {
        currentIndex = index;
    }
}
