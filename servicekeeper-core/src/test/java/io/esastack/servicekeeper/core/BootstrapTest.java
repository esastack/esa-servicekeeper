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

import io.esastack.servicekeeper.core.asynchandle.AsyncResultHandler;
import io.esastack.servicekeeper.core.asynchandle.CompletableStageHandler;
import io.esastack.servicekeeper.core.asynchandle.RequestHandle;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

class BootstrapTest {

    @Test
    void testBasic() {
        then(Bootstrap.entry()).isNotNull();

        final BootstrapContext ctx0 = Bootstrap.ctx();
        then(ctx0.handlers().size()).isEqualTo(1);
        then(ctx0.factory()).isNotNull();
        then(ctx0.globalConfig()).isNotNull();
        then(ctx0.groupConfig()).isNotNull();
        then(ctx0.config()).isNull();
        then(ctx0.cluster()).isNotNull();
        then(ctx0.immutableConfigs()).isNotNull();
        then(ctx0.limitConfig()).isNotNull();
        then(ctx0.listeners()).isNotNull();
        then(ctx0.listeners().size()).isEqualTo(1);


        final List<AsyncResultHandler<?>> handlers = new LinkedList<>();
        handlers.add(new CompletableStageHandler<>());
        handlers.add(new AsyncResultHandler<Object>() {
            @Override
            public boolean supports(Class<?> returnType) {
                return false;
            }

            @Override
            public Object handle0(Object returnValue, RequestHandle requestHandle) {
                return null;
            }
        });

        Bootstrap.init(BootstrapContext.singleton(handlers));
        final BootstrapContext ctx1 = Bootstrap.ctx();
        then(ctx0).isSameAs(ctx1);
    }

}
