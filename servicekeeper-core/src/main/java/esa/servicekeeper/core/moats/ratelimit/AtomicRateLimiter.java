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
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package esa.servicekeeper.core.moats.ratelimit;

import esa.commons.Checks;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.metrics.RateLimitMetrics;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * The class is directly copied from Resilience4j(https://github.com/resilience4j/resilience4j).
 */
public class AtomicRateLimiter implements RateLimiter {

    private static final long NANO_TIME_START = nanoTime();

    private final String name;
    private final AtomicInteger waitingThreads;
    private final AtomicReference<State> state;
    private final RateLimitConfig immutableConfig;

    public AtomicRateLimiter(String name, RateLimitConfig rateLimitConfig, RateLimitConfig immutableConfig) {
        Checks.checkNotNull(rateLimitConfig, "rateLimitConfig");
        Checks.checkNotEmptyArg(name, "Rname");

        this.name = name;
        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new State(
                rateLimitConfig, 0, rateLimitConfig.getLimitForPeriod(), 0
        ));
        this.immutableConfig = immutableConfig;
    }

    public AtomicRateLimiter(RateLimitConfig rateLimitConfig, RateLimitConfig immutableConfig) {
        Checks.checkNotNull(rateLimitConfig, "rateLimitConfig");

        this.name = null;
        waitingThreads = new AtomicInteger(0);
        state = new AtomicReference<>(new State(
                rateLimitConfig, 0, rateLimitConfig.getLimitForPeriod(), 0
        ));
        this.immutableConfig = immutableConfig;
    }

    @Override
    public void changeLimitForPeriod(final int limitForPeriod) {
        RateLimitConfig newConfig = RateLimitConfig.from(state.get().config)
                .limitForPeriod(limitForPeriod)
                .build();
        state.updateAndGet(currentState -> new State(
                newConfig, currentState.activeCycle, currentState.activePermissions, currentState.nanosToWait
        ));
    }

    @Override
    public void changeConfig(RateLimitConfig rateLimitConfig) {
        RateLimitConfig newConfig = RateLimitConfig.from(rateLimitConfig).build();
        state.updateAndGet(currentState -> new State(
                newConfig, currentState.activeCycle, currentState.activePermissions, currentState.nanosToWait
        ));
    }

    @Override
    public RateLimitConfig config() {
        return state.get().config;
    }

    @Override
    public boolean acquirePermission(final Duration timeoutDuration) {
        long timeoutInNanos = timeoutDuration.toNanos();
        State modifiedState = updateStateWithBackOff(timeoutInNanos);
        return waitForPermissionIfNecessary(timeoutInNanos, modifiedState.nanosToWait);
    }

    @Override
    public RateLimitConfig immutableConfig() {
        return immutableConfig;
    }

    @Override
    public RateLimitMetrics metrics() {
        return new Metrics();
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Calculates time elapsed from the class loading.
     */
    private long currentNanoTime() {
        return nanoTime() - NANO_TIME_START;
    }

    /**
     * Atomically updates the current {@link State} with the results of
     * applying the {@link AtomicRateLimiter#calculateNextState}, returning the updated {@link State}.
     * It differs from {@link AtomicReference#updateAndGet(UnaryOperator)} by constant back off.
     * It means that after one try to {@link AtomicReference#compareAndSet(Object, Object)}
     * this method will wait for a while before try one more time.
     * This technique was originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param timeoutInNanos a side-effect-free function
     * @return the updated value
     */
    private State updateStateWithBackOff(final long timeoutInNanos) {
        AtomicRateLimiter.State prev;
        AtomicRateLimiter.State next;
        do {
            prev = state.get();
            next = calculateNextState(timeoutInNanos, prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     * It differs from  by constant back off.
     * It means that after one try to {@link AtomicReference#compareAndSet(Object, Object)}
     * this method will wait for a while before try one more time.
     * This technique was originally described in this
     * <a href="https://arxiv.org/abs/1305.5800"> paper</a>
     * and showed great results with {@link AtomicRateLimiter} in benchmark tests.
     *
     * @param current the expected value
     * @param next    the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    private boolean compareAndSet(final State current, final State next) {
        if (state.compareAndSet(current, next)) {
            return true;
        }
        // back-off
        parkNanos(1);
        return false;
    }

    /**
     * A side-effect-free function that can calculate next {@link State} from current.
     * It determines time duration that you should wait for permission and reserves it for you,
     * if you'll be able to wait long enough.
     *
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param activeState    current internal of {@link AtomicRateLimiter}
     * @return next {@link State}
     */
    private State calculateNextState(final long timeoutInNanos, final State activeState) {
        long cyclePeriodInNanos = activeState.config.getLimitRefreshPeriodInNanos();
        int permissionsPerCycle = activeState.config.getLimitForPeriod();

        long currentNanos = currentNanoTime();
        long currentCycle = currentNanos / cyclePeriodInNanos;

        long nextCycle = activeState.activeCycle;
        int nextPermissions = activeState.activePermissions;
        if (nextCycle != currentCycle) {
            long elapsedCycles = currentCycle - nextCycle;
            long accumulatedPermissions = elapsedCycles * permissionsPerCycle;
            nextCycle = currentCycle;
            nextPermissions = (int) min(nextPermissions + accumulatedPermissions, permissionsPerCycle);
        }
        long nextNanosToWait = nanosToWaitForPermission(
                cyclePeriodInNanos, permissionsPerCycle, nextPermissions, currentNanos, currentCycle
        );
        return reservePermissions(activeState.config, timeoutInNanos, nextCycle, nextPermissions, nextNanosToWait);
    }

    /**
     * Calculates time to wait for next permission as
     * [time to the next cycle] + [duration of full cycles until reserved permissions expire]
     *
     * @param cyclePeriodInNanos   current configuration values
     * @param permissionsPerCycle  current configuration values
     * @param availablePermissions currently available permissions, can be negative if some permissions have
     *                             been reserved
     * @param currentNanos         current time in nanoseconds
     * @param currentCycle         current {@link AtomicRateLimiter} cycle
     * @return nanoseconds to wait for the next permission
     */
    private long nanosToWaitForPermission(final long cyclePeriodInNanos,
                                          final int permissionsPerCycle,
                                          final int availablePermissions,
                                          final long currentNanos,
                                          final long currentCycle) {
        if (availablePermissions > 0) {
            return 0L;
        }
        long nextCycleTimeInNanos = (currentCycle + 1) * cyclePeriodInNanos;
        long nanosToNextCycle = nextCycleTimeInNanos - currentNanos;
        int fullCyclesToWait = (-availablePermissions) / permissionsPerCycle;
        return (fullCyclesToWait * cyclePeriodInNanos) + nanosToNextCycle;
    }

    /**
     * Determines whether caller can acquire permission before timeout or not and then creates
     * corresponding {@link State}. Reserves permissions only if caller can successfully wait for
     * permission.
     *
     * @param config         rateLimitConfig
     * @param timeoutInNanos max time that caller can wait for permission in nanoseconds
     * @param cycle          cycle for new {@link State}
     * @param permissions    permissions for new {@link State}
     * @param nanosToWait    nanoseconds to wait for the next permission
     * @return new {@link State} with possibly reserved permissions and time to wait
     */
    private State reservePermissions(final RateLimitConfig config,
                                     final long timeoutInNanos,
                                     final long cycle,
                                     final int permissions,
                                     final long nanosToWait) {
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;
        int permissionsWithReservation = permissions;
        if (canAcquireInTime) {
            permissionsWithReservation--;
        }
        return new State(config, cycle, permissionsWithReservation, nanosToWait);
    }

    /**
     * If nanosToWait is bigger than 0 it tries to park {@link Thread} for nanosToWait but not longer then
     * timeoutInNanos.
     *
     * @param timeoutInNanos max time that caller can wait
     * @param nanosToWait    nanoseconds caller need to wait
     * @return true if caller was able to wait for nanosToWait without {@link Thread#interrupt} and not exceed timeout
     */
    private boolean waitForPermissionIfNecessary(final long timeoutInNanos, final long nanosToWait) {
        boolean canAcquireImmediately = nanosToWait <= 0;
        boolean canAcquireInTime = timeoutInNanos >= nanosToWait;

        if (canAcquireImmediately) {
            return true;
        }
        if (canAcquireInTime) {
            return waitForPermission(nanosToWait);
        }
        waitForPermission(timeoutInNanos);
        return false;
    }

    /**
     * Parks {@link Thread} for nanosToWait.
     * <p>If the current thread is {@linkplain Thread#interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException},
     * but its interrupt status will be set.
     *
     * @param nanosToWait nanoseconds caller need to wait
     * @return true if caller was not {@link Thread#interrupted} while waiting
     */
    private boolean waitForPermission(final long nanosToWait) {
        waitingThreads.incrementAndGet();
        long deadline = currentNanoTime() + nanosToWait;
        boolean wasInterrupted = false;
        while (currentNanoTime() < deadline && !wasInterrupted) {
            long sleepBlockDuration = deadline - currentNanoTime();
            parkNanos(sleepBlockDuration);
            wasInterrupted = Thread.interrupted();
        }
        waitingThreads.decrementAndGet();
        if (wasInterrupted) {
            currentThread().interrupt();
        }
        return !wasInterrupted;
    }

    /**
     * <p>{@link AtomicRateLimiter.State} represents immutable internal of {@link AtomicRateLimiter} where:
     * <ul>
     * <li>activeCycle - {@link AtomicRateLimiter} cycle number that was used
     * by the last {@link AtomicRateLimiter#acquirePermission(Duration)} call.</li>
     * <p>
     * <li>activePermissions - count of available permissions after
     * the last {@link AtomicRateLimiter#acquirePermission(Duration)} call.
     * Can be negative if some permissions where reserved.</li>
     * <p>
     * <li>nanosToWait - count of nanoseconds to wait for permission for
     * the last {@link AtomicRateLimiter#acquirePermission(Duration)} call.</li>
     * </ul>
     */
    private static class State {
        private final RateLimitConfig config;

        private final long activeCycle;
        private final int activePermissions;
        private final long nanosToWait;

        private State(RateLimitConfig config,
                      final long activeCycle, final int activePermissions, final long nanosToWait) {
            this.config = config;
            this.activeCycle = activeCycle;
            this.activePermissions = activePermissions;
            this.nanosToWait = nanosToWait;
        }

    }

    private class Metrics implements RateLimitMetrics {

        private Metrics() {
        }

        @Override
        public int numberOfWaitingThreads() {
            return waitingThreads.get();
        }

        @Override
        public int availablePermissions() {
            return calculateNextState(-1, state.get()).activePermissions;
        }

        /**
         * @return estimated time duration in nanos to wait for the next permission
         */
        public long getNanosToWait() {
            State currentState = state.get();
            State estimatedState = calculateNextState(-1, currentState);
            return estimatedState.nanosToWait;
        }

        /**
         * @return estimated current cycle
         */
        public long getCycle() {
            State currentState = state.get();
            State estimatedState = calculateNextState(-1, currentState);
            return estimatedState.activeCycle;
        }
    }

}
