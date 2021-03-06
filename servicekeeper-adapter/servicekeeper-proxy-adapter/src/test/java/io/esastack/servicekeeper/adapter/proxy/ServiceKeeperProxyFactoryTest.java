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
package io.esastack.servicekeeper.adapter.proxy;

import io.esastack.servicekeeper.core.exception.RateLimitOverflowException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServiceKeeperProxyFactoryTest {

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testCreateProxyNoInterface() {
        HelloServiceNoInterface proxyNoInterface =
                ServiceKeeperProxyFactory.createProxyNoInterface(new HelloServiceNoInterface());

        then(proxyNoInterface.hello()).isEqualTo(HelloServiceNoInterface.HELLO);
        assertThrows(RateLimitOverflowException.class, proxyNoInterface::hello);
    }

    //RateLimit is configured through RateLimitConfigSourcesFactory
    @Test
    void testCreateProxyHasInterface() {
        HelloService proxyHasInterface =
                ServiceKeeperProxyFactory.createProxyHasInterface(new HelloServiceHasInterface());

        then(proxyHasInterface.helloV2()).isEqualTo(HelloServiceNoInterface.HELLO);
        assertThrows(RateLimitOverflowException.class, proxyHasInterface::helloV2);
    }

}
