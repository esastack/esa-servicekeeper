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
package io.esastack.servicekeeper.core;

import esa.commons.Checks;
import io.esastack.servicekeeper.core.asynchandle.CompletableStageHandler;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperEntry;
import io.esastack.servicekeeper.core.entry.ServiceKeeperAsyncEntry;
import io.esastack.servicekeeper.core.entry.ServiceKeeperEntry;

import java.util.Collections;

public final class Bootstrap {

    public static final String OVERRIDING_KEY = "servicekeeper.bootstrap.overriding.enable";

    private static final boolean OVERRIDING = overriding();

    private static volatile IntimateItem INSTANCE;

    /**
     * Init current {@link Bootstrap} with {@link BootstrapContext}.
     *
     * @param ctx ctx
     */
    public static synchronized void init(BootstrapContext ctx) {
        if (OVERRIDING || INSTANCE == null) {
            INSTANCE = new IntimateItem(new CompositeServiceKeeperEntry(ctx.config(),
                    ctx.immutableConfigs(),
                    ctx.factory(), ctx.globalConfig(),
                    ctx.groupConfig(), ctx.handlers()), ctx);
        }
    }

    /**
     * Obtains the {@link ServiceKeeperEntry}.
     *
     * @return {@link ServiceKeeperEntry}
     */
    public static ServiceKeeperEntry entry() {
        if (INSTANCE == null) {
            init(BootstrapContext.singleton(Collections.singletonList(new CompletableStageHandler<>())));
        }
        return INSTANCE.entry;
    }

    /**
     * Obtains the {@link ServiceKeeperAsyncEntry}.
     *
     * @return {@link ServiceKeeperAsyncEntry}
     */
    public static ServiceKeeperAsyncEntry asyncEntry() {
        if (INSTANCE == null) {
            init(BootstrapContext.singleton(Collections.emptyList()));
        }
        return INSTANCE.entry;
    }

    /**
     * Obtains the {@link BootstrapContext} related to current {@link #entry()} and {@link #asyncEntry()}.
     *
     * @return ctx
     */
    public static BootstrapContext ctx() {
        return INSTANCE.ctx;
    }

    private static boolean overriding() {
        if (Boolean.TRUE.toString().equals(System.getenv(OVERRIDING_KEY))) {
            return true;
        }
        return Boolean.TRUE.toString().equals(System.getProperty(OVERRIDING_KEY));
    }

    private static class IntimateItem {

        private final CompositeServiceKeeperEntry entry;
        private final BootstrapContext ctx;

        private IntimateItem(CompositeServiceKeeperEntry entry, BootstrapContext ctx) {
            this.entry = Checks.checkNotNull(entry, "entry");
            this.ctx = Checks.checkNotNull(ctx, "ctx");
        }
    }
}
