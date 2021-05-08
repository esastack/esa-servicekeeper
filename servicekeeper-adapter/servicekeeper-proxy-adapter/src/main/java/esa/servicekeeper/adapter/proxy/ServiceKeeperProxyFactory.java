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
package esa.servicekeeper.adapter.proxy;

import esa.servicekeeper.adapter.proxy.handler.ServiceKeeperInvocationHandler;
import esa.servicekeeper.adapter.proxy.handler.ServiceKeeperMethodHandler;
import esa.servicekeeper.core.utils.ClassCastUtils;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import java.lang.reflect.Proxy;

public final class ServiceKeeperProxyFactory {

    private ServiceKeeperProxyFactory() {
    }

    /**
     * Proxy the target object which not implements from any interface.
     *
     * @param delegate delegate
     * @param <T>      T
     * @return proxied object
     */
    public static <T> T createProxyNoInterface(T delegate) {
        //Instantiate the proxy factory.
        final ProxyFactory factory = new ProxyFactory();

        //Set super class, proxy factory will new a class which extends from target class.
        factory.setSuperclass(delegate.getClass());

        final Class<?> proxyClazz = factory.createClass();

        ProxyObject proxied;
        try {
            proxied = (ProxyObject) proxyClazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to proxy object: " + delegate.getClass().getName(), e);
        }

        proxied.setHandler(new ServiceKeeperMethodHandler(delegate));
        return ClassCastUtils.cast(proxied);
    }

    /**
     * Proxy the target object which implements from interface, this way has higher performance than
     * {@link #createProxyNoInterface(Object)}.
     *
     * @param delegate delegate
     * @param <T> T
     * @return proxied object
     */
    public static <T> T createProxyHasInterface(final T delegate) {
        return ClassCastUtils.cast(Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                new ServiceKeeperInvocationHandler(delegate)));
    }

}
