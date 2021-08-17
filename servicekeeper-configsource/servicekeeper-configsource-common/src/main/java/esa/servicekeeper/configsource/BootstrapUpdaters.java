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
package esa.servicekeeper.configsource;

import esa.servicekeeper.core.BootstrapContext;
import esa.servicekeeper.core.BootstrapContextListener;
import esa.servicekeeper.core.utils.LogUtils;
import esa.servicekeeper.core.utils.SpiUtils;
import esa.servicekeeper.core.utils.SystemConfigUtils;
import esa.commons.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class BootstrapUpdaters implements BootstrapContextListener {

    public static final String DISABLE_CONFIGURATORS = "servicekeeper.configurators.disable";

    private static final Logger logger = LogUtils.logger();

    @Override
    public void onInitialization(BootstrapContext ctx) {
        if (SystemConfigUtils.getBooleanFromEnvAndProp(DISABLE_CONFIGURATORS, false)) {
            logger.info("All configurators are disabled!");
            return;
        }

        logger.info("Begin to init dynamic updaters...");

        final List<SelfStartUpdaters> updaters = SpiUtils.loadAll(SelfStartUpdaters.class);
        if (updaters.isEmpty()) {
            logger.info("The dynamic updaters loaded by spi are empty");
            return;
        }

        logger.info("The dynamic updater loaded by spi are: " + updaters.stream()
                .map(SelfStartUpdaters::name).collect(Collectors.toList()));

        final InternalsUpdater updater0 = buildInternalsUpdater(ctx);
        updaters.forEach((updater) -> updater.initAndStart(updater0));
    }

    /**
     * Build {@link InternalsUpdater} for further use in {@link SelfStartUpdaters#updater}.
     *
     * @param ctx ctx
     * @return {@link InternalsUpdater}.
     */
    private InternalsUpdater buildInternalsUpdater(final BootstrapContext ctx) {
        return new InternalsUpdaterImpl(ctx.cluster(), ctx.groupConfig(), ctx.factory(),
                ctx.globalConfig(), ctx.listeners());
    }

}
