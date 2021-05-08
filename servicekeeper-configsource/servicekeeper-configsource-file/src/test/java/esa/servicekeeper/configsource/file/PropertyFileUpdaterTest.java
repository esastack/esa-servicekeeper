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

import esa.servicekeeper.configsource.file.constant.PropertyFileConstant;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.BDDAssertions.then;

class PropertyFileUpdaterTest {

    @Test
    void testCreateFile() {
        // Case 1: target file existed before
        final String configDir0 = PropertyFileConstant.getConfigDir();
        final String configName0 = PropertyFileConstant.getConfigName();

        final File file0 = new File(configDir0, configName0);
        then(file0.exists()).isTrue();
        then(new PropertyFileUpdater().createFile(configDir0, configName0)).isTrue();

        // Case 2: target file doesn't exist
        final String configDir1 = PropertyFileConstant.getConfigDir();
        final String configName1 = "abc.properties";
        final File file1 = new File(configDir1, configName1);
        then(file1.exists()).isFalse();
        then(new PropertyFileUpdater().createFile(configDir1, configName1)).isTrue();
        file1.deleteOnExit();
    }

}
