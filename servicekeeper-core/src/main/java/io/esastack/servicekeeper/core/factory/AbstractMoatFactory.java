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
package io.esastack.servicekeeper.core.factory;

import esa.commons.Checks;
import esa.commons.logging.Logger;
import io.esastack.servicekeeper.core.common.OriginalInvocation;
import io.esastack.servicekeeper.core.common.ResourceId;
import io.esastack.servicekeeper.core.config.RetryConfig;
import io.esastack.servicekeeper.core.retry.RetryEventProcessor;
import io.esastack.servicekeeper.core.retry.RetryOperations;
import io.esastack.servicekeeper.core.retry.RetryOperationsImpl;
import io.esastack.servicekeeper.core.retry.internal.BackOffPolicy;
import io.esastack.servicekeeper.core.retry.internal.RetryablePredicate;
import io.esastack.servicekeeper.core.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractMoatFactory<CNF2, RTU> implements
        MoatFactory<ResourceId, OriginalInvocation, CNF2, RTU> {

    private final MoatFactoryContext context;

    AbstractMoatFactory(MoatFactoryContext context) {
        Checks.checkNotNull(context, "context");
        this.context = context;
    }

    /**
     * Obtains {@link MoatFactoryContext} of current factory.
     *
     * @return ctx
     */
    protected MoatFactoryContext context() {
        return context;
    }

    static class RetryOperationFactory extends AbstractMoatFactory<RetryConfig, RetryOperations> {

        private static final Logger logger = LogUtils.logger();

        RetryOperationFactory(MoatFactoryContext context) {
            super(context);
        }

        @Override
        public RetryOperations doCreate(ResourceId resourceId,
                                        OriginalInvocation config1,
                                        RetryConfig config2,
                                        RetryConfig immutableConfig) {
            List<RetryEventProcessor> processors = new ArrayList<>(1);

            for (EventProcessorFactory factory : context().processors()) {
                RetryEventProcessor processor = factory.retry(resourceId);
                if (processor != null) {
                    processors.add(processor);
                }
            }

            final RetryOperations operations = new RetryOperationsImpl(resourceId, processors,
                    BackOffPolicy.newInstance(config2.getBackoffConfig()),
                    RetryablePredicate.newInstance(config2), config2,
                    immutableConfig);

            logger.info("Created retry operations successfully, resourceId: {}," +
                    " config: {}, immutable config: {}", resourceId, config2, immutableConfig);

            processors.forEach(processor -> processor.onInitialization(operations));
            return operations;
        }

    }
}
