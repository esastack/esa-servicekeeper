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
package io.esastack.servicekeeper.core.moats;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.config.MoatConfig;

import java.util.Collections;
import java.util.List;

public abstract class AbstractMoat<T> implements Moat<T> {

    protected final boolean hasProcessors;
    private final List<MoatEventProcessor> processors;
    private final MoatConfig config;

    public AbstractMoat(List<MoatEventProcessor> processors, MoatConfig config) {
        Checks.checkNotNull(config, "config");
        this.processors = (processors == null ? null : Collections.unmodifiableList(processors));
        this.config = config;
        this.hasProcessors = (processors != null && !processors.isEmpty());
    }

    @Override
    public MoatConfig config0() {
        return this.config;
    }

    protected final void preDestroy0() {
        if (processors != null) {
            processors.forEach(process -> process.onDestroy(this));
        }
    }

    protected void process(MoatEvent event) {
        for (MoatEventProcessor processor : processors) {
            processor.process(name(), event);
        }
    }

    /**
     * Obtains name of current {@link Moat}.
     *
     * @return name
     */
    protected abstract String name();

}
