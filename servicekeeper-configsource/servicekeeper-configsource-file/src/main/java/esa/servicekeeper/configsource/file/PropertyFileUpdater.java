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
package esa.servicekeeper.configsource.file;

import esa.commons.Checks;
import esa.servicekeeper.configsource.SelfStartUpdaters;
import esa.servicekeeper.configsource.file.constant.PropertyFileConstant;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.moats.circuitbreaker.CircuitBreakerMoat;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.moats.ratelimit.RateLimitMoat;
import esa.servicekeeper.core.utils.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static esa.servicekeeper.configsource.file.SingletonFactory.handler;
import static esa.servicekeeper.configsource.file.utils.PropertiesUtils.*;

/**
 * Use the local configuration file which named as service-keeper.properties in the conf directory of current workspace
 * as the dynamic config source. When the object is instantiated, a listener is set up too. The listener
 * is designed to watch the chang of specified directory which config file is in. When the config file has updated
 * during running time dynamically, the config will be reloaded and the newest {@link ExternalConfig} will be used
 * to onUpdate #{@link ConcurrentLimitMoat} #{@link CircuitBreakerMoat} and #{@link RateLimitMoat}.
 */
public class PropertyFileUpdater extends SelfStartUpdaters {

    private static final Logger logger = LogUtils.logger();

    private static final long AUTO_REFRESH_TIME_MS = 500L;

    private final Properties properties = new Properties();
    private volatile long lastModified = 0L;
    private File file;

    private ScheduledThreadPoolExecutor service;

    @Override
    public void start() {
        final String configDir = PropertyFileConstant.getConfigDir();
        final String configName = PropertyFileConstant.getConfigName();

        if (createFile(configDir, configName)) {
            addWatcher(configDir, configName);
        }
        if (service != null) {
            Runtime.getRuntime().addShutdownHook(new Thread(service::shutdownNow));
        }
    }

    @Override
    public String name() {
        return "PropertyFileUpdater";
    }

    boolean createFile(String configDir, String configName) {
        // Step one: get or create file
        file = new File(configDir, configName);
        try {
            // Make sure the common file exists in the classpath, if not, just doCreate it.
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    logger.error("Failed to create file: {}", file.getName());
                    return false;
                } else {
                    logger.info("The file: {}  doesn't exist, a new one has been created", file.getName());
                    return true;
                }
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to get initial properties and add listener to file: {}",
                    PropertyFileConstant.getConfigDir() + File.separator + configName, e);
            return false;
        }
    }

    private void addWatcher(String configDir, String configName) {
        final TimingCheckTask listeningTask = new TimingCheckTask(handler(updater));
        listeningTask.run();

        // Step two: start timing task
        startTimingTask(configName, configDir, listeningTask);
    }

    private void startTimingTask(String configName, String configDir, TimingCheckTask checkTask) {
        service = new ScheduledThreadPoolExecutor(1, (r) -> {
            Thread thread = new Thread(r);
            thread.setName("Timing-check-" + configName + "-task");
            thread.setDaemon(true);
            return thread;
        }, (r, executor) -> logger.error("A Timing-check-" + configName + "-task was rejected"));
        service.setMaximumPoolSize(1);

        logger.info("A listener has been added to file: {} successfully",
                configDir + File.separator + configName);

        service.scheduleAtFixedRate(checkTask, AUTO_REFRESH_TIME_MS, AUTO_REFRESH_TIME_MS,
                TimeUnit.MILLISECONDS);
    }

    class TimingCheckTask implements Runnable {

        private final InternalConfigsHandler handler;

        TimingCheckTask(InternalConfigsHandler handler) {
            Checks.checkNotNull(handler, "InternalConfigsHandler must not be null");
            this.handler = handler;
        }

        @Override
        public void run() {
            final long currentModified = file.lastModified();
            try {
                if (currentModified != lastModified) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("The " + file.getName() + " has updated");
                    }

                    // Update local config
                    try {
                        properties.load(new FileInputStream(file));
                    } catch (IOException ex) {
                        logger.error("Failed to load file {} while timing check updates", file.getName());
                    }
                    handler.update(getConfigs(properties));
                    handler.updateGlobalConfigs(getGlobalDisable(properties),
                            getArgLevelEnable(properties), getRetryEnable(properties));
                    handler.updateMaxSizeLimits(getMaxSizeLimits(properties));
                }
            } catch (Throwable throwable) {
                logger.error("Failed to update cached external config map", throwable);
            } finally {
                properties.clear();
                lastModified = currentModified;
            }
        }
    }
}
