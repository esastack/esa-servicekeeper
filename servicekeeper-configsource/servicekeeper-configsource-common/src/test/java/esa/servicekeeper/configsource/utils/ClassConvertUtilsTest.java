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
package esa.servicekeeper.configsource.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

class ClassConvertUtilsTest {

    @Test
    void testGetClasses() {
        String origin = "java.lang.RuntimeException";
        then(ClassConvertUtils.getClasses(origin).length).isEqualTo(1);
        then(ClassConvertUtils.getClasses(origin)).isEqualTo(new Class[]{RuntimeException.class});

        String origin1 = "java.lan.RuntimeException";
        then(ClassConvertUtils.getClasses(origin1).length).isEqualTo(0);

        String origin2 = "[java.lang.RuntimeException]";
        then(ClassConvertUtils.getClasses(origin2).length).isEqualTo(1);
        then(ClassConvertUtils.getClasses(origin2)).isEqualTo(new Class[]{RuntimeException.class});

        String origin3 = "[java.lang.RuntimeException, java.lang0.Exception, java.lang.IllegalArgumentException]";
        then(ClassConvertUtils.getClasses(origin3).length).isEqualTo(2);
        then(ClassConvertUtils.getClasses(origin3)).isEqualTo(new Class[]
                {RuntimeException.class, IllegalArgumentException.class});

        //Original source is null
        then(ClassConvertUtils.getClasses(null).length).isEqualTo(0);

        //Original source is empty
        then(ClassConvertUtils.getClasses("").length).isEqualTo(0);

        //Original source is empty
        then(ClassConvertUtils.getClasses("[]").length).isEqualTo(0);
    }

    @Test
    void testGetClass() {
        String origin = "java.lang.RuntimeException";
        then(ClassConvertUtils.getClass(origin)).isEqualTo(RuntimeException.class);

        String origin1 = "java.lan.RuntimeException";
        then(ClassConvertUtils.getClass(origin1)).isNull();

        String origin2 = "[java.lang.RuntimeException]";
        then(ClassConvertUtils.getClass(origin2)).isEqualTo(RuntimeException.class);

        String origin3 = "[java.lang.RuntimeException, java.lang0.Exception, java.lang.IllegalArgumentException]";
        then(ClassConvertUtils.getClass(origin3)).isEqualTo(RuntimeException.class);

        //Original source is null
        then(ClassConvertUtils.getClass(null)).isNull();

        //Original source is empty
        then(ClassConvertUtils.getClass("")).isNull();
    }

}
