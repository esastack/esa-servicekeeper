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
package esa.servicekeeper.core.entry;

import esa.servicekeeper.core.asynchandle.CompletableStageHandler;
import esa.servicekeeper.core.asynchandle.RequestHandle;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.GroupConfigSource;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.factory.LimitableMoatFactoryContext;
import esa.servicekeeper.core.factory.MoatClusterFactory;
import esa.servicekeeper.core.factory.MoatClusterFactoryImpl;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeServiceKeeperEntryTest {

    private final ImmutableConfigs configs = mock(ImmutableConfigs.class);

    private final PlainConfigSource source = mock(PlainConfigSource.class);
    private final GroupConfigSource group = mock(GroupConfigSource.class);
    private final GlobalConfig config = new GlobalConfig();

    private final LimitableMoatFactoryContext ctx = mock(LimitableMoatFactoryContext.class);
    private final InternalMoatCluster cluster = mock(InternalMoatCluster.class);

    private CompositeServiceKeeperEntry entry;

    @BeforeEach
    void setUp() {
        entry = new CompositeServiceKeeperEntry(source, configs, new MoatClusterFactoryImpl(ctx, cluster, configs),
                config, group, null);
    }

    @Test
    void testConstructor() {
        final ImmutableConfigs configs = mock(ImmutableConfigs.class);
        final MoatClusterFactory factory = mock(MoatClusterFactory.class);
        final GlobalConfig config = mock(GlobalConfig.class);

        // ImmutableConfigs isn't allowed null
        assertThrows(NullPointerException.class, () ->
                new CompositeServiceKeeperEntry(null, null, factory,
                        config, null, null));

        // MoatClusterFactory isn't allowed null
        assertThrows(NullPointerException.class, () ->
                new CompositeServiceKeeperEntry(null, configs, null,
                        config, null, null));

        // GlobalConfig isn't allowed null
        assertThrows(NullPointerException.class, () ->
                new CompositeServiceKeeperEntry(null, configs, factory,
                        null, null, null));

        new CompositeServiceKeeperEntry(null, configs, factory, config, null, null);
    }

    @Test
    void testGetExternalConfig() {
        // Get method config directly
        final ResourceId id = ResourceId.from("testGetExternalConfig");
        then(entry.getExternalConfig(id)).isNull();
        when(source.config(id)).thenReturn(new ExternalConfig());
        then(entry.getExternalConfig(id)).isEqualTo(new ExternalConfig());

        // Get arg config directly
        final ArgResourceId argId0 = new ArgResourceId(id, "arg0", "LiMing");
        then(entry.getExternalConfig(argId0)).isNull();
        when(source.config(argId0)).thenReturn(new ExternalConfig());
        then(entry.getExternalConfig(argId0)).isEqualTo(new ExternalConfig());

        // Get arg config of match all
        final ArgResourceId argId1 = new ArgResourceId(id, "arg0", "LiMing0");
        then(entry.getExternalConfig(argId1)).isNull();
        when(source.config(new ArgResourceId(id, "arg0", "*"))).thenReturn(new ExternalConfig());
        then(entry.getExternalConfig(argId1)).isEqualTo(new ExternalConfig());

        // Get group config
        final GroupResourceId groupId = GroupResourceId.from("testGetExternalConfig.group");
        when(source.config(id)).thenReturn(null);
        then(entry.getExternalConfig(id)).isNull();
        when(group.config(groupId)).thenReturn(null);
        when(group.mappingGroupId(id)).thenReturn(groupId);
        then(entry.getExternalConfig(id)).isNull();
        when(group.config(groupId)).thenReturn(new ExternalConfig());

        // Overriding group config
        final ExternalConfig config0 = new ExternalConfig();
        config0.setLimitForPeriod(10);
        config0.setMaxConcurrentLimit(10);
        config0.setFailureRateThreshold(60.0f);
        config0.setFallbackValue("ABC");
        config0.setFallbackMethodName("fallback");
        when(group.config(groupId)).thenReturn(config0);

        final ExternalConfig config1 = new ExternalConfig();
        config0.setLimitForPeriod(100);
        config0.setMaxConcurrentLimit(100);
        config0.setFailureRateThreshold(70.0f);
        when(source.config(id)).thenReturn(config1);

        final ExternalConfig config2 = entry.getExternalConfig(id);
        then(config2.getLimitForPeriod()).isEqualTo(100);
        then(config2.getMaxConcurrentLimit()).isEqualTo(100);
        then(config2.getFailureRateThreshold()).isEqualTo(70.0f);
        then(config2.getFallbackValue()).isEqualTo("ABC");
        then(config2.getFallbackMethodName()).isEqualTo("fallback");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInvoke() throws Throwable {
        final Method method = HelloService.class.getDeclaredMethod("sayHello");
        final Method asyncMethod = HelloService.class.getDeclaredMethod("sayHello0");
        final Object delegate = new HelloService();

        then(entry.invoke("abc", method, delegate, new Object[0])).isEqualTo("Hello");

        final CompositeServiceKeeperEntry entry0 = new CompositeServiceKeeperEntry(source, configs,
                new MoatClusterFactoryImpl(ctx, cluster, configs), config, group,
                Collections.singletonList(new CompletableStageHandler<>()));
        final CompletableFuture<String> asyncResult =
                (CompletableFuture<String>) entry0.invoke("abc", asyncMethod, delegate, new Object[0]);
        then(asyncResult.isDone()).isTrue();
        then(asyncResult.get()).isEqualTo("Hello");
    }

    @Test
    void testCall() throws Throwable {
        final HelloService delegate = new HelloService();
        then(entry.call("abc", () -> null, null,
                delegate::sayHello, new Object[0])).isEqualTo("Hello");

        final CompositeServiceKeeperEntry entry0 = new CompositeServiceKeeperEntry(source, configs,
                new MoatClusterFactoryImpl(ctx, cluster, configs), config, group,
                Collections.singletonList(new CompletableStageHandler<>()));
        final CompletableFuture<String> asyncResult =
                entry0.call("abc", () -> null, () -> new OriginalInvocation(CompletableFuture.class,
                                new Class[0]),
                        delegate::sayHello0, new Object[0]);
        then(asyncResult.isDone()).isTrue();
        then(asyncResult.get()).isEqualTo("Hello");
    }

    @Test
    void testTryAsyncExecute() {
        final RequestHandle handle0 = entry.tryAsyncExecute("abc", null,
                new OriginalInvocation(CompletableFuture.class, new Class[0]), new Object[0]);
        assertNull(handle0.getNotAllowedCause());
    }

    private static class HelloService {

        private String sayHello() {
            return "Hello";
        }

        private CompletableFuture<String> sayHello0() {
            return CompletableFuture.completedFuture("Hello");
        }
    }

}
