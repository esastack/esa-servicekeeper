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

import esa.commons.Checks;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.executionchain.Context;
import esa.servicekeeper.core.listener.FondConfigListener;

public class PredicateByExceptionAndSpendTime implements PredicateStrategy, PredicateConfigFilling,
        FondConfigListener<Long> {

    private final PredicateByException predicateByException;
    private final PredicateBySpendTime predicateBySpendTime;

    public PredicateByExceptionAndSpendTime(PredicateByException predicateByException,
                                            PredicateBySpendTime predicateBySpendTime) {
        Checks.checkNotNull(predicateByException, "PredicateByException must not be null");
        Checks.checkNotNull(predicateBySpendTime, "PredicateBySpendTime must not be null");
        this.predicateByException = predicateByException;
        this.predicateBySpendTime = predicateBySpendTime;
    }

    @Override
    public boolean isSuccess(Context ctx) {
        return predicateByException.isSuccess(ctx) && predicateBySpendTime.isSuccess(ctx);
    }

    @Override
    public Long getFond(ExternalConfig config) {
        return predicateBySpendTime.getFond(config);
    }

    @Override
    public void updateWithNewestConfig(Long newestConfig) {
        predicateBySpendTime.updateWithNewestConfig(newestConfig);
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        predicateBySpendTime.updateWhenNewestConfigIsNull();
    }

    @Override
    public boolean isConfigEquals(Long newestConfig) {
        return predicateBySpendTime.isConfigEquals(newestConfig);
    }

    @Override
    public ResourceId listeningKey() {
        return predicateBySpendTime.listeningKey();
    }

    @Override
    public String toString() {
        return predicateBySpendTime.toString();
    }

    @Override
    public void fill(CircuitBreakerConfig config) {
        predicateBySpendTime.fill(config);
        predicateByException.fill(config);
    }
}
