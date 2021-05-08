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

import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.ResourceId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MoatStatisticsImpl implements MoatClusterListener, MoatStatistics {

    private final AtomicInteger totalCount = new AtomicInteger();

    private final Map<MoatType, AtomicInteger> countOfTypeMap = new ConcurrentHashMap<>(3);

    private final Map<ArgConfigKey, AtomicInteger> countOfArgMap = new ConcurrentHashMap<>(1);

    @Override
    public void onAdd(Moat moat) {
        totalCount.incrementAndGet();

        countOfTypeMap.computeIfAbsent(moat.type(), (type) -> new AtomicInteger()).incrementAndGet();

        final ResourceId resourceId = moat.config0().getResourceId();
        if (resourceId instanceof ArgResourceId) {
            final ArgConfigKey key = new ArgConfigKey((ArgResourceId) resourceId, moat.type());
            countOfArgMap.computeIfAbsent(key, (k) -> new AtomicInteger()).incrementAndGet();
        }
    }

    @Override
    public void onRemove(Moat moat) {
        // Decrement total count
        totalCount.decrementAndGet();

        // Decrement count of type
        final AtomicInteger countOfType = countOfTypeMap.get(moat.type());
        if (countOfType == null) {
            return;
        }
        countOfType.decrementAndGet();

        // Decrement count of args'
        final ResourceId resourceId = moat.config0().getResourceId();
        if (!(resourceId instanceof ArgResourceId)) {
            return;
        }
        final ArgConfigKey key = new ArgConfigKey((ArgResourceId) resourceId, moat.type());
        final AtomicInteger countOfArg = countOfArgMap.get(key);
        if (countOfArg == null) {
            return;
        }
        countOfArg.decrementAndGet();
    }

    @Override
    public int countOf(ArgConfigKey key) {
        final AtomicInteger count = countOfArgMap.get(key);
        return count == null ? 0 : count.get();
    }

    @Override
    public int countOf(MoatType type) {
        final AtomicInteger count = countOfTypeMap.get(type);
        return count == null ? 0 : count.get();
    }

    @Override
    public int totalCount() {
        return totalCount.get();
    }

}
