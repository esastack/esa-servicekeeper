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
package io.esastack.servicekeeper.core.moats.circuitbreaker.internal;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.config.CircuitBreakerConfig;
import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerSateTransitionEvent;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreakerSateTransitionProcessor;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class CircuitBreakerStateMachine implements CircuitBreaker {

    private final String name;
    private final AtomicReference<CircuitBreakerState> stateReference;
    private final CircuitBreakerConfig immutableConfig;
    private final CircuitBreakerConfig circuitBreakerConfig;
    private final List<CircuitBreakerSateTransitionProcessor> processors;

    /**
     * Creates a circuitBreaker.
     *
     * @param name                 the name of the CircuitBreaker
     * @param circuitBreakerConfig The CircuitBreaker configuration.
     * @param immutableConfig      The immutable CircuitBreaker configuration.
     */
    public CircuitBreakerStateMachine(String name, CircuitBreakerConfig circuitBreakerConfig,
                                      CircuitBreakerConfig immutableConfig,
                                      List<CircuitBreakerSateTransitionProcessor> processors) {
        Checks.checkNotEmptyArg(name, "name");
        Checks.checkNotNull(circuitBreakerConfig, "circuitBreakerConfig");

        this.name = name;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.immutableConfig = immutableConfig;

        switch (circuitBreakerConfig.getState()) {
            case FORCED_OPEN:
                this.stateReference = new AtomicReference<>(new ForcedOpenState(this));
                break;
            case FORCED_DISABLED:
                this.stateReference = new AtomicReference<>(new DisabledState(this));
                break;
            default:
                this.stateReference = new AtomicReference<>(new ClosedState(this));
                break;
        }

        this.processors = (processors == null ? Collections.emptyList() : Collections.unmodifiableList(processors));
    }

    /**
     * Requests permission to call this backend.
     *
     * @return true, if the call is allowed.
     */
    @Override
    public boolean isCallPermitted() {
        return stateReference.get().isCallPermitted();
    }

    @Override
    public void onSuccess() {
        stateReference.get().onSuccess();
    }

    @Override
    public void onFailure() {
        stateReference.get().onFailure();
    }

    /**
     * Get the name of this CircuitBreaker.
     *
     * @return the the name of this CircuitBreaker
     */
    @Override
    public String name() {
        return this.name;
    }

    /**
     * Get the config of this CircuitBreaker.
     *
     * @return the config of this CircuitBreaker
     */
    @Override
    public CircuitBreakerConfig config() {
        return circuitBreakerConfig;
    }

    @Override
    public CircuitBreakerConfig immutableConfig() {
        return immutableConfig;
    }

    @Override
    public CircuitBreakerMetrics metrics() {
        return this.stateReference.get().getMetrics();
    }

    @Override
    public void reset() {
        stateReference.getAndUpdate(currentState -> new ClosedState(this));
    }

    @Override
    public void transitionToOpenState() {
        final State preState = getState();
        stateTransition(State.OPEN, currentState -> new OpenState(this,
                currentState.getMetrics()));
        processSateTransition(preState, State.OPEN);
    }

    @Override
    public void transitionToHalfOpenState() {
        final State preState = getState();
        stateTransition(State.HALF_OPEN, currentState -> new HalfOpenState(this));
        processSateTransition(preState, State.HALF_OPEN);
    }

    @Override
    public void transitionToClosedState() {
        final State preState = getState();
        stateTransition(State.CLOSED, currentState ->
                new ClosedState(this, currentState.getMetrics()));
        processSateTransition(preState, State.CLOSED);
    }

    @Override
    public void forceToDisabledState() {
        final State preState = getState();
        stateTransition(State.FORCED_DISABLED, currentState ->
                new DisabledState(this));
        processSateTransition(preState, State.FORCED_DISABLED);
    }

    @Override
    public void forceToForcedOpenState() {
        final State preState = getState();
        stateTransition(State.FORCED_OPEN, currentState ->
                new ForcedOpenState(this));
        processSateTransition(preState, State.FORCED_OPEN);
    }

    @Override
    public State getState() {
        return stateReference.get().getState();
    }

    private void stateTransition(CircuitBreaker.State newState,
                                 Function<CircuitBreakerState, CircuitBreakerState> newStateGenerator) {
        stateReference.getAndUpdate(currentState -> {
            if (currentState.getState() == newState) {
                return currentState;
            }
            LogUtils.logger().warn("The circuitBreaker transition from {} to {}, which name is {}",
                    stateReference.get().getState(), newState, name());
            return newStateGenerator.apply(currentState);
        });
    }

    private void processSateTransition(CircuitBreaker.State preState, CircuitBreaker.State newState) {
        if (processors.isEmpty()) {
            return;
        }

        for (CircuitBreakerSateTransitionProcessor processor : processors) {
            processor.process(name, new CircuitBreakerSateTransitionEvent(preState, newState));
        }
    }
}

