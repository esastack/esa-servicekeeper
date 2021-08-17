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
package esa.servicekeeper.core.retry;

import esa.commons.Checks;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.RetryConfig;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.exception.BackOffInterruptedException;
import esa.servicekeeper.core.exception.ServiceRetryException;
import esa.servicekeeper.core.executionchain.Executable;
import esa.servicekeeper.core.listener.FondConfigListener;
import esa.servicekeeper.core.metrics.RetryMetrics;
import esa.servicekeeper.core.moats.LifeCycleSupport;
import esa.servicekeeper.core.retry.internal.BackOffPolicy;
import esa.servicekeeper.core.retry.internal.RetryablePredicate;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import static esa.servicekeeper.core.metrics.Metrics.Type.RETRY;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.PERMANENT;
import static esa.servicekeeper.core.moats.LifeCycleSupport.LifeCycleType.TEMPORARY;
import static esa.servicekeeper.core.retry.RetryEvent.EventType.COMPLETE;
import static esa.servicekeeper.core.retry.RetryEvent.EventType.START;
import static esa.servicekeeper.core.utils.ConfigUtils.combine;

/**
 * The core implementation of {@link RetryOperations}, and can update itself with {@link RetryConfig}.
 */
public class RetryOperationsImpl implements RetryOperations, FondConfigListener<RetryConfig>, LifeCycleSupport {

    private static final Logger logger = LogUtils.logger();

    private final ResourceId resourceId;
    private final RetryConfig immutableConfig;

    private final LifeCycleSupport.LifeCycleType lifeCycleType;
    private final AtomicBoolean shouldDelete = new AtomicBoolean();
    private final List<RetryEventProcessor> processors;
    private final RetryEventProcessImpl statistics = new RetryEventProcessImpl();

    private volatile RetryConfig config;
    private volatile BackOffPolicy backOffPolicy;
    private volatile RetryablePredicate predicate;

    public RetryOperationsImpl(ResourceId resourceId, List<RetryEventProcessor> processors,
                               BackOffPolicy backOffPolicy, RetryablePredicate predicate,
                               RetryConfig config, RetryConfig immutableConfig) {
        Checks.checkNotNull(resourceId, "resourceId");
        Checks.checkNotNull(predicate, "predicate");
        Checks.checkNotNull(config, "config");
        Checks.checkNotNull(backOffPolicy, "backOffPolicy");
        this.resourceId = resourceId;
        this.immutableConfig = immutableConfig;
        this.config = config;
        this.backOffPolicy = backOffPolicy;
        this.predicate = predicate;
        this.lifeCycleType = immutableConfig == null ? TEMPORARY : PERMANENT;
        if (processors == null) {
            this.processors = Collections.singletonList(statistics);
        } else {
            processors.add(statistics);
            this.processors = Collections.unmodifiableList(processors);
        }
    }

    @Override
    public <T> T execute(RetryContext context, Executable<T> executable) throws Throwable {
        if (!needRetry()) {
            return executable.execute();
        }

        // First time execution, which mustn't back off.
        try {
            return executable.execute();
        } catch (Throwable t) {
            context.registerThrowable(t);
        }

        if (!predicate.canRetry(context)) {
            throw context.getLastThrowable();
        }

        // Begins to real retry.
        final RetryEvent startEvent = buildStartEvt(context);
        processors.forEach((processor) -> processor.process(resourceId.getName(), startEvent));

        while (predicate.canRetry(context)) {
            try {
                backOffPolicy.backOff(context);
                final T result = executable.execute();
                context.registerThrowable(null);

                final RetryEvent endEvent = buildEndEvt(context);
                processors.forEach((processor) -> processor.process(resourceId.getName(), endEvent));
                return result;
            } catch (BackOffInterruptedException ex) {
                throw ex;
            } catch (Throwable th) {
                context.registerThrowable(th);
            }
        }

        final RetryEvent endEvent = buildEndEvt(context);
        processors.forEach((processor) -> processor.process(resourceId.getName(), endEvent));
        throw createFailsCause(context);
    }

    @Override
    public RetryConfig getConfig() {
        return this.config;
    }

    @Override
    public RetryMetrics getMetrics() {
        return new Metrics();
    }

    @Override
    public RetryConfig getFond(ExternalConfig config) {
        return combine(immutableConfig, config);
    }

    @Override
    public synchronized void updateWithNewestConfig(RetryConfig newestConfig) {
        if (!this.config.equals(newestConfig)) {
            logger.info("Begin to update retry operations: {} with the newest config: {}", resourceId, newestConfig);
            this.predicate = RetryablePredicate.newInstance(newestConfig);
            this.backOffPolicy = BackOffPolicy.newInstance(newestConfig.getBackoffConfig());
            this.config = newestConfig;
        }
    }

    @Override
    public void updateWhenNewestConfigIsNull() {
        if (this.lifeCycleType == PERMANENT) {
            updateWithNewestConfig(immutableConfig);
        } else {
            if (processors != null) {
                processors.forEach(processor -> processor.onDestroy(this));
            }
            shouldDelete.compareAndSet(false, true);
        }
    }

    @Override
    public boolean isConfigEquals(RetryConfig newestConfig) {
        return config.equals(newestConfig);
    }

    @Override
    public ResourceId listeningKey() {
        return resourceId;
    }

    @Override
    public LifeCycleType lifeCycleType() {
        return lifeCycleType;
    }

    @Override
    public boolean shouldDelete() {
        return shouldDelete.get();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetryOperationsImpl.class.getSimpleName() + "[", "]")
                .add("resourceId=" + resourceId)
                .add("immutableConfig=" + immutableConfig)
                .add("lifeCycleType=" + lifeCycleType)
                .add("config=" + config)
                .toString();
    }

    private boolean needRetry() {
        return config.getMaxAttempts() != null && config.getMaxAttempts() > 1;
    }

    private ServiceRetryException createFailsCause(final RetryContext context) {
        if (config.getMaxAttempts() == context.getRetriedCount()) {
            return new ServiceRetryException(resourceId.getName() +
                    " has retried " + config.getMaxAttempts() + " times", context.getLastThrowable());
        } else {
            return new ServiceRetryException(resourceId.getName() + " caught none-retryable exception" +
                    " during retry", context.getLastThrowable());
        }
    }

    private RetryEvent buildStartEvt(final RetryContext ctx) {
        return new RetryEvent() {
            @Override
            public RetryContext getContext() {
                return ctx;
            }

            @Override
            public EventType getType() {
                return START;
            }
        };
    }

    private RetryEvent buildEndEvt(final RetryContext ctx) {
        return new RetryEvent() {
            @Override
            public RetryContext getContext() {
                return ctx;
            }

            @Override
            public EventType getType() {
                return COMPLETE;
            }
        };
    }

    private class Metrics implements RetryMetrics {

        private final int maxAttempts;
        private final long retriedTimes;
        private final long totalRetriedCount;

        private Metrics() {
            this.maxAttempts = config.getMaxAttempts();
            this.retriedTimes = statistics.retriedTimes.sum();
            this.totalRetriedCount = statistics.totalRetriedCount.sum();
        }

        @Override
        public int maxAttempts() {
            return maxAttempts;
        }

        @Override
        public long retriedTimes() {
            return retriedTimes;
        }

        @Override
        public long totalRetriedCount() {
            return totalRetriedCount;
        }

        @Override
        public Type type() {
            return RETRY;
        }
    }

    private static class RetryEventProcessImpl implements RetryEventProcessor {

        private final LongAdder retriedTimes = new LongAdder();
        private final LongAdder totalRetriedCount = new LongAdder();

        @Override
        public void process(String name, RetryEvent event) {
            if (event == null) {
                return;
            }
            switch (event.getType()) {
                case START:
                    onStart();
                    break;
                case COMPLETE:
                    onEnd(event.getContext());
                    break;
                default:
            }
        }

        private void onStart() {
            retriedTimes.increment();
        }

        private void onEnd(RetryContext ctx) {
            totalRetriedCount.add(ctx.getRetriedCount() - 1);
        }
    }
}
