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

import io.esastack.servicekeeper.core.metrics.CircuitBreakerMetrics;
import io.esastack.servicekeeper.core.moats.circuitbreaker.CircuitBreaker;

import java.util.concurrent.atomic.LongAdder;

/**
 * The class is directly copied from Resilience4j(https://github.com/resilience4j/resilience4j) and we make
 * some simplifications to remove those methods which are never used by us.
 */
abstract class CircuitBreakerState {

    CircuitBreakerStateMachine stateMachine;

    CircuitBreakerState(CircuitBreakerStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Whether current call is permitted to pass.
     *
     * @return true or false.
     */
    abstract boolean isCallPermitted();

    /**
     * Record a success
     */
    abstract void onSuccess();

    /**
     * Record a failure
     */
    abstract void onFailure();

    /**
     * Get collector
     *
     * @return circuitBreaker collector
     */
    abstract Metrics getMetrics();

    /**
     * Get State
     *
     * @return state
     */
    abstract CircuitBreaker.State getState();

    class Metrics implements CircuitBreakerMetrics {

        private final int ringBufferSize;
        private final RingBitSet ringBitSet;
        private final LongAdder numberOfNotPermittedCalls;

        Metrics(int ringBufferSize) {
            this(ringBufferSize, null);
        }

        private Metrics(int ringBufferSize, RingBitSet sourceSet) {
            this.ringBufferSize = ringBufferSize;
            if (sourceSet != null) {
                this.ringBitSet = new RingBitSet(this.ringBufferSize, sourceSet);
            } else {
                this.ringBitSet = new RingBitSet(this.ringBufferSize);
            }
            this.numberOfNotPermittedCalls = new LongAdder();
        }

        /**
         * Creates a new CircuitBreakerMetrics instance and copies the content of the current RingBitSet
         * into the new RingBitSet.
         *
         * @param targetRingBufferSize the ringBufferSize of the new CircuitBreakerMetrics instances
         * @return a CircuitBreakerMetrics
         */
        Metrics copy(int targetRingBufferSize) {
            return new Metrics(targetRingBufferSize, this.ringBitSet);
        }

        /**
         * Records a failed call and returns the current failure rate in percentage.
         *
         * @return the current failure rate  in percentage.
         */
        float onError() {
            int currentNumberOfFailedCalls = ringBitSet.setNextBit(true);
            return getFailureRate(currentNumberOfFailedCalls);
        }

        /**
         * Records a successful call and returns the current failure rate in percentage.
         *
         * @return the current failure rate in percentage.
         */
        float onSuccess() {
            int currentNumberOfFailedCalls = ringBitSet.setNextBit(false);
            return getFailureRate(currentNumberOfFailedCalls);
        }

        /**
         * Records a call which was not permitted, because the CircuitBreaker internal is OPEN.
         */
        void onCallNotPermitted() {
            numberOfNotPermittedCalls.increment();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float failureRateThreshold() {
            return getFailureRate(numberOfFailedCalls());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int maxNumberOfBufferedCalls() {
            return ringBufferSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int numberOfSuccessfulCalls() {
            return numberOfBufferedCalls() - numberOfFailedCalls();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int numberOfBufferedCalls() {
            return this.ringBitSet.length();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long numberOfNotPermittedCalls() {
            return this.numberOfNotPermittedCalls.sum();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int numberOfFailedCalls() {
            return this.ringBitSet.cardinality();
        }

        @Override
        public CircuitBreaker.State state() {
            return stateMachine.getState();
        }

        private float getFailureRate(int numberOfFailedCalls) {
            if (numberOfBufferedCalls() < ringBufferSize) {
                return -1.0f;
            }
            return numberOfFailedCalls * 100.0f / ringBufferSize;
        }

    }
}
