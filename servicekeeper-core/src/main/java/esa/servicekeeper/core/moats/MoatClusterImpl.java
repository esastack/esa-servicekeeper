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

import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.OrderedComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.unmodifiableList;

public class MoatClusterImpl implements MoatCluster {

    private final CopyOnWriteArrayList<Moat<?>> moats;
    private final List<MoatClusterListener> listeners;

    public MoatClusterImpl(List<Moat<?>> moats,
                           List<MoatClusterListener> listeners) {
        if (moats == null) {
            moats = new CopyOnWriteArrayList<>();
        } else {
            OrderedComparator.sort(moats);
        }
        this.moats = new CopyOnWriteArrayList<>(moats);
        this.listeners = listeners == null ? Collections.emptyList() : unmodifiableList(listeners);
        this.listeners.forEach(item -> this.moats.forEach(item::onAdd));
    }

    @Override
    public void add(Moat<?> moat) {
        moats.add(moat);
        listeners.forEach(item -> item.onAdd(moat));
        OrderedComparator.sort(moats);
    }

    @Override
    public void remove(Moat<?> moat) {
        moats.remove(moat);
        listeners.forEach(item -> item.onRemove(moat));
    }

    @Override
    public List<Moat<?>> getAll() {
        return unmodifiableList(moats);
    }

    @Override
    public boolean contains(MoatType type) {
        switch (type) {
            case RATE_LIMIT:
                for (Moat moat : getAll()) {
                    if (moat instanceof RateLimitMoat) {
                        return true;
                    }
                }
                return false;
            case CIRCUIT_BREAKER:
                for (Moat moat : getAll()) {
                    if (moat instanceof CircuitBreakerMoat) {
                        return true;
                    }
                }
                return false;
            case CONCURRENT_LIMIT:
                for (Moat moat : getAll()) {
                    if (moat instanceof ConcurrentLimitMoat) {
                        return true;
                    }
                }
                return false;
            default:
                return true;
        }
    }

    @Override
    public void remove(MoatType type) {
        final List<Moat<?>> moatsToRemove = new ArrayList<>(1);
        for (Moat moat : moats) {
            if (moat.type().equals(type)) {
                moatsToRemove.add(moat);
            }
        }

        moatsToRemove.forEach(moat -> {
            moats.remove(moat);
            listeners.forEach((listener) -> listener.onRemove(moat));
        });
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MoatClusterImpl.class.getSimpleName() + "[", "]")
                .add("moats=" + moats)
                .toString();
    }
}

