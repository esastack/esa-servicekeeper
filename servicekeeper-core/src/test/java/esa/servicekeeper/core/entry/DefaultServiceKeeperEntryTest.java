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

import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.OriginalInvocation;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.config.*;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.PlainConfigSource;
import esa.servicekeeper.core.executionchain.AsyncExecutionChainImpl;
import esa.servicekeeper.core.executionchain.RetryableExecutionChain;
import esa.servicekeeper.core.executionchain.SyncContext;
import esa.servicekeeper.core.executionchain.SyncExecutionChainImpl;
import esa.servicekeeper.core.factory.LimitableMoatFactoryContext;
import esa.servicekeeper.core.factory.MoatClusterFactoryImpl;
import esa.servicekeeper.core.internal.GlobalConfig;
import esa.servicekeeper.core.internal.ImmutableConfigs;
import esa.servicekeeper.core.internal.InternalMoatCluster;
import esa.servicekeeper.core.moats.ArgMoatCluster;
import esa.servicekeeper.core.moats.ArgMoatClusterImpl;
import esa.servicekeeper.core.moats.Moat;
import esa.servicekeeper.core.moats.RetryableMoatCluster;
import esa.servicekeeper.core.moats.concurrentlimit.ConcurrentLimitMoat;
import esa.servicekeeper.core.retry.RetryOperationsImpl;
import esa.servicekeeper.core.retry.RetryableExecutor;
import esa.servicekeeper.core.retry.internal.BackOffPolicy;
import esa.servicekeeper.core.retry.internal.RetryablePredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static esa.servicekeeper.core.configsource.MoatLimitConfigSource.VALUE_MATCH_ALL;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultServiceKeeperEntryTest {

    private final ImmutableConfigs configs = mock(ImmutableConfigs.class);

    private final PlainConfigSource source = mock(PlainConfigSource.class);
    private final GlobalConfig config = new GlobalConfig();

    private final LimitableMoatFactoryContext ctx = mock(LimitableMoatFactoryContext.class);
    private final InternalMoatCluster cluster = mock(InternalMoatCluster.class);

    private DefaultServiceKeeperEntry entry;

    @BeforeEach
    void setUp() {
        entry = new DefaultServiceKeeperEntry(source, configs, new MoatClusterFactoryImpl(ctx, cluster, configs),
                config);
    }

    @Test
    void testInvoke() throws Throwable {
        final Method method = HelloService.class.getDeclaredMethod("sayHello");
        final Object delegate = new HelloService();

        then(entry.invoke("abc", method, delegate)).isEqualTo("Hello");
    }

    @Test
    void testCall() throws Throwable {
        final HelloService delegate = new HelloService();
        then(entry.call("abc", () -> null, null,
                delegate::sayHello, new Object[0])).isEqualTo("Hello");
    }

    @Test
    void testRun() throws Throwable {
        entry.run("abc", () -> {
        });
    }

    @Test
    void testRun1() throws Throwable {
        entry.run("abc", null, () -> {
        });
    }

    @Test
    void testInvoke1() throws Throwable {
        final Method method = HelloService.class.getDeclaredMethod("sayHello");
        final Object delegate = new HelloService();

        then(entry.invoke(method, delegate)).isEqualTo("Hello");
    }

    @Test
    void testCall1() throws Throwable {
        final HelloService delegate = new HelloService();
        then(entry.call("abc", delegate::sayHello)).isEqualTo("Hello");
    }

    @Test
    void testCall2() throws Throwable {
        final HelloService delegate = new HelloService();
        then(entry.call("abc", null, delegate::sayHello)).isEqualTo("Hello");
    }

    @Test
    void testCall3() throws Throwable {
        final HelloService delegate = new HelloService();
        then(entry.call("abc", null, null,
                delegate::sayHello)).isEqualTo("Hello");
    }

    @Test
    void testBuildWithNoArgs() {
        final String name = "testBuildWithNoArgs";
        final List<Moat<?>> moats = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from(name)), ConcurrentLimitConfig.ofDefault(),
                null, null));

        final RetryableExecutor retryable = new RetryableExecutor(new RetryOperationsImpl(ResourceId.from(name),
                null, BackOffPolicy.newInstance(RetryConfig.ofDefault().getBackoffConfig()),
                RetryablePredicate.newInstance(RetryConfig.ofDefault()), RetryConfig.ofDefault(),
                null));

        // Only args:   sync    ----> moats and retryable
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats, null, null, retryable));
        then(entry.buildExecutionChain(name, null, null, false))
                .isInstanceOf(RetryableExecutionChain.class);

        // Only args:   sync    ----> retryable is null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats, null,
                null, null));
        then(entry.buildExecutionChain(name, null, null, false))
                .isInstanceOf(SyncExecutionChainImpl.class);

        // Only args:   sync    ----> retryable and moats are all null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(null, null,
                null, null));
        then(entry.buildExecutionChain(name, null, null, false)).isNull();

        // Only args:   sync    ----> retryable is empty
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(Collections.emptyList(),
                null, null, null));
        then(entry.buildExecutionChain(name, null, null, false)).isNull();


        // Only args:   async    ----> moats and retryable
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats, null, null, retryable));
        then(entry.buildExecutionChain(name, null, null, true))
                .isInstanceOf(AsyncExecutionChainImpl.class);

        // Only args:   async    ----> retryable is null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats, null, null,
                null));
        then(entry.buildExecutionChain(name, null, null, true))
                .isInstanceOf(AsyncExecutionChainImpl.class);

        // Only args:   async    ----> retryable and moats are all null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(null, null, null,
                null));
        then(entry.buildExecutionChain(name, null, null, true)).isNull();

        // Only args:   async    ----> moats is empty
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(Collections.emptyList(),
                null, null, null));
        then(entry.buildExecutionChain(name, null, null, true)).isNull();
    }

    @Test
    void testBuildWithArgs() {
        final String name = "testBuildWithArgs";
        final List<Moat<?>> moats0 = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from(name)), ConcurrentLimitConfig.ofDefault(),
                null, null));

        final RetryableExecutor retryable0 = new RetryableExecutor(new RetryOperationsImpl(ResourceId.from(name),
                null, BackOffPolicy.newInstance(RetryConfig.ofDefault().getBackoffConfig()),
                RetryablePredicate.newInstance(RetryConfig.ofDefault()), RetryConfig.ofDefault(),
                null));

        final ArgResourceId argId = new ArgResourceId(ResourceId.from(name), "arg0", "LiMing");

        final List<Moat<?>> moats1 = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(argId), ConcurrentLimitConfig.ofDefault(),
                null, null));

        final ArgMoatCluster argMoatCluster = new ArgMoatClusterImpl(moats1, null);

        // With args:   sync    ----> moats and retryable
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                null));
        when(cluster.get(argId)).thenReturn(argMoatCluster);

        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(SyncExecutionChainImpl.class);

        // Only args:   sync    ----> retryable is null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                null));
        when(cluster.get(argId)).thenReturn(argMoatCluster);

        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(SyncExecutionChainImpl.class);

        // Only args:   sync    ----> retryable and moats are all null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                null));
        when(cluster.get(argId)).thenReturn(new ArgMoatClusterImpl(null, null));

        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(SyncExecutionChainImpl.class);

        // Only args:   sync    ----> moats is empty
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(null, null, null,
                null));
        when(cluster.get(argId)).thenReturn(new ArgMoatClusterImpl(Collections.emptyList(), null));

        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isNull();

        // Only args:   async    ----> moats and retryable
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                retryable0));
        when(cluster.get(argId)).thenReturn(argMoatCluster);

        then(entry.buildExecutionChain(name, null, null, true, "LiMing"))
                .isInstanceOf(AsyncExecutionChainImpl.class);

        // Only args:   async    ----> retryable is null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                null));
        when(cluster.get(argId)).thenReturn(argMoatCluster);

        then(entry.buildExecutionChain(name, null, null, true, "LiMing"))
                .isInstanceOf(AsyncExecutionChainImpl.class);

        // Only args:   async    ----> retryable and moats are all null
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                null));
        when(cluster.get(argId)).thenReturn(new ArgMoatClusterImpl(null, null));

        then(entry.buildExecutionChain(name, null, null, true, "LiMing"))
                .isInstanceOf(AsyncExecutionChainImpl.class);

        // Only args:   async    ----> moats is empty
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(null, null, null,
                null));
        when(cluster.get(argId)).thenReturn(new ArgMoatClusterImpl(Collections.emptyList(), null));

        then(entry.buildExecutionChain(name, null, null, true, "LiMing"))
                .isNull();
    }

    @Test
    void testGlobalConfig() {
        final String name = "testGlobalConfig";
        final List<Moat<?>> moats0 = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(ResourceId.from(name)), ConcurrentLimitConfig.ofDefault(),
                null, null));

        final RetryableExecutor retryable0 = new RetryableExecutor(new RetryOperationsImpl(ResourceId.from(name),
                null, BackOffPolicy.newInstance(RetryConfig.ofDefault().getBackoffConfig()),
                RetryablePredicate.newInstance(RetryConfig.ofDefault()), RetryConfig.ofDefault(),
                null));

        // when global is disabled
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                retryable0));
        then(entry.buildExecutionChain(name, null, null, false))
                .isInstanceOf(RetryableExecutionChain.class);
        config.updateGlobalDisable(true);
        then(entry.buildExecutionChain(name, null, null, false))
                .isNull();

        // Do recover
        config.updateGlobalDisable(false);


        // when arg level is disabled
        final ArgResourceId argId = new ArgResourceId(ResourceId.from(name), "arg0", "LiMing");

        final List<Moat<?>> moats1 = Collections.singletonList(new ConcurrentLimitMoat(
                new MoatConfig(argId), ConcurrentLimitConfig.ofDefault(),
                null, null));

        final ArgMoatCluster argMoatCluster = new ArgMoatClusterImpl(moats1, null);
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(null, null, null,
                null));
        when(cluster.get(argId)).thenReturn(argMoatCluster);
        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(SyncExecutionChainImpl.class);
        config.updateArgLevelEnable(false);
        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isNull();

        // Do recover
        config.updateArgLevelEnable(true);

        // when retry is disabled
        when(cluster.get(ResourceId.from(name))).thenReturn(new RetryableMoatCluster(moats0, null, null,
                retryable0));
        when(cluster.get(argId)).thenReturn(argMoatCluster);
        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(RetryableExecutionChain.class);
        config.updateRetryEnable(false);
        then(entry.buildExecutionChain(name, null, null, false, "LiMing"))
                .isInstanceOf(SyncExecutionChainImpl.class);

        // Do recover
        config.updateRetryEnable(true);
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
    }

    @Test
    void testBuildContext() {
        then(entry.buildContext("testBuildContext", new Object[0])).isInstanceOf(SyncContext.class);
    }

    @Test
    void testGetOriginalInvocation0() throws NoSuchMethodException {
        final OriginalInvocation info = entry.getOriginalInvocation(this.getClass()
                .getDeclaredMethod("demo", String.class)).get();

        then(info.getReturnType()).isEqualTo(String.class);
        then(info.getParameterTypes().length).isEqualTo(1);
        then(info.getParameterTypes()[0]).isEqualTo(String.class);
    }

    @Test
    void testGetOriginalInvocation1() {
        final Callable<String> callable = () -> "ABC";
        final OriginalInvocation info = entry.getOriginalInvocation(callable).get();

        then(info.getReturnType()).isEqualTo(Object.class);
        then(info.getParameterTypes().length).isEqualTo(0);
    }

    @Test
    void testGetOriginalInvocation2() {
        final OriginalInvocation info = entry.getOriginalInvocation().get();

        then(info.getReturnType()).isEqualTo(void.class);
        then(info.getParameterTypes().length).isEqualTo(0);
    }

    @Test
    void testGetImmutableConfig() {
        final ResourceId resourceId = ResourceId.from("testGetImmutableConfig");
        final String argValue = "testGetImmutableConfig";

        // Case 1: argConfig == null
        then(entry.getImmutableConfig(resourceId, argValue, null)).isNull();

        // Case 2: argConfig template
        final CompositeServiceKeeperConfig compositeConfig2 = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(0, "arg0",
                        CircuitBreakerConfig.builder()
                                .failureRateThreshold(90.0f)
                                .ringBufferSizeInClosedState(10)
                                .ringBufferSizeInHalfOpenState(11).build(), Collections.emptyMap())
                .argRateLimitConfig(0, "arg0",
                        RateLimitConfig.builder()
                                .limitRefreshPeriod(Duration.ofSeconds(10L)).build(), Collections.emptyMap())
                .build();

        final ServiceKeeperConfig config2 = entry.getImmutableConfig(resourceId, argValue,
                compositeConfig2.getArgConfig().getArgConfigMap().get(0));
        then(config2.getConcurrentLimitConfig()).isNull();
        then(config2.getFallbackConfig()).isNull();
        then(config2.getRetryConfig()).isNull();
        then(config2.getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(10L));
        then(config2.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(90.0f);
        then(config2.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(10);
        then(config2.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(11);

        // Case 3: argConfig of *
        final CompositeServiceKeeperConfig compositeConfig3 = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(0, "arg0",
                        CircuitBreakerConfig.builder()
                                .failureRateThreshold(90.0f)
                                .ringBufferSizeInClosedState(10)
                                .ringBufferSizeInHalfOpenState(11).build(),
                        Collections.singletonMap(VALUE_MATCH_ALL, 99.0f))
                .argConcurrentLimit(0, Collections.singletonMap(VALUE_MATCH_ALL, 99))
                .argRateLimitConfig(0, "arg0",
                        RateLimitConfig.builder()
                                .limitRefreshPeriod(Duration.ofSeconds(10L)).build(),
                        Collections.singletonMap(VALUE_MATCH_ALL, 99))
                .build();

        final ServiceKeeperConfig config3 = entry.getImmutableConfig(resourceId, argValue,
                compositeConfig3.getArgConfig().getArgConfigMap().get(0));
        then(config3.getFallbackConfig()).isNull();
        then(config3.getRetryConfig()).isNull();
        then(config3.getConcurrentLimitConfig().getThreshold()).isEqualTo(99);
        then(config3.getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(10L));
        then(config3.getRateLimitConfig().getLimitForPeriod()).isEqualTo(99);
        then(config3.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(99.0f);
        then(config3.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(10);
        then(config3.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(11);

        // Case 4: argConfig
        final Map<Object, Float> failureRateThresholds = new HashMap<>();
        failureRateThresholds.put(VALUE_MATCH_ALL, 99.0f);
        failureRateThresholds.put(argValue, 66.0f);

        final Map<Object, Integer> maxConcurrentLimits = new HashMap<>();
        maxConcurrentLimits.put(VALUE_MATCH_ALL, 99);
        maxConcurrentLimits.put(argValue, 66);

        final Map<Object, Integer> limitFoPeriods = new HashMap<>(maxConcurrentLimits);

        final CompositeServiceKeeperConfig compositeConfig4 = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(0, "arg0",
                        CircuitBreakerConfig.builder()
                                .failureRateThreshold(90.0f)
                                .ringBufferSizeInClosedState(10)
                                .ringBufferSizeInHalfOpenState(11).build(),
                        failureRateThresholds)
                .argConcurrentLimit(0, maxConcurrentLimits)
                .argRateLimitConfig(0, "arg0",
                        RateLimitConfig.builder()
                                .limitRefreshPeriod(Duration.ofSeconds(10L)).build(),
                        limitFoPeriods)
                .build();

        final ServiceKeeperConfig config4 = entry.getImmutableConfig(resourceId, argValue,
                compositeConfig4.getArgConfig().getArgConfigMap().get(0));
        then(config4.getFallbackConfig()).isNull();
        then(config4.getRetryConfig()).isNull();
        then(config4.getConcurrentLimitConfig().getThreshold()).isEqualTo(66);
        then(config4.getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(10L));
        then(config4.getRateLimitConfig().getLimitForPeriod()).isEqualTo(66);
        then(config4.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(66.0f);
        then(config4.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(10);
        then(config4.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
    }

    private String demo(String name) {
        return name;
    }

    private static class HelloService {

        private String sayHello() {
            return "Hello";
        }
    }

}
