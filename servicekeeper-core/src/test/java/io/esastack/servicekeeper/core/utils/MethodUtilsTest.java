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
package io.esastack.servicekeeper.core.utils;

import io.esastack.servicekeeper.core.common.GroupResourceId;
import io.esastack.servicekeeper.core.config.ServiceKeeperConfig;
import io.esastack.servicekeeper.core.entry.CompositeServiceKeeperConfig;
import io.esastack.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import io.esastack.servicekeeper.core.mock.MockMethods;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MethodUtilsTest {

    private final Class<?> mockClass = MockMethods.class;

    @Test
    void testGetQualifiedName() throws Exception {
        final Method method = mockClass.getDeclaredMethod("testGetQualifiedName");
        then(MethodUtils.getMethodAlias(method)).isEqualTo("io.esastack.servicekeeper.core.mock" +
                ".MockMethods.testGetQualifiedName");
    }

    @Test
    void testGetMethodAlias() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodWithoutAnnotation");
        then(MethodUtils.getMethodAlias(method))
                .isEqualTo(method.getDeclaringClass().getName() + "." + method.getName());

        method = mockClass.getDeclaredMethod("methodWithAnnotation");
        then(MethodUtils.getMethodAlias(method)).isEqualTo("method-alias-test");
    }

    @Test
    void testGetOnlyFallbackConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyFallback");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);
        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNotNull();
        then(config.getMethodConfig().getFallbackConfig().getSpecifiedValue()).isEqualTo("FallbackValue");
        then(config.getMethodConfig().getFallbackConfig().getMethodName()).isEqualTo("fallbackMethod");
        then(config.getMethodConfig().getFallbackConfig().getSpecifiedException())
                .isEqualTo(RuntimeException.class);
        then(config.getMethodConfig().getFallbackConfig().getTargetClass()).isEqualTo(MockMethods.class);

        then(config.getMethodConfig().getCircuitBreakerConfig()).isNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNull();
        then(config.getMethodConfig().getRateLimitConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testGetOnlyConcurrentLimitConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyConcurrentLimit");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNull();
        then(config.getMethodConfig().getCircuitBreakerConfig()).isNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNotNull();
        then(config.getMethodConfig().getConcurrentLimitConfig().getThreshold()).isEqualTo(500);

        then(config.getMethodConfig().getRateLimitConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testConcurrentLimitAliasSet() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodConcurrentLimitAliasSet");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNull();
        then(config.getMethodConfig().getCircuitBreakerConfig()).isNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNotNull();
        then(config.getMethodConfig().getConcurrentLimitConfig().getThreshold()).isEqualTo(500);

        then(config.getMethodConfig().getRateLimitConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testConcurrentLimitAliasSetError() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodConcurrentLimitAliasSetError");
        assertThrows(IllegalArgumentException.class, () -> MethodUtils.getCompositeConfig(method));
    }

    @Test
    void testGetOnlyRateLimitConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyRateLimit");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);
        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNull();
        then(config.getMethodConfig().getCircuitBreakerConfig()).isNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNull();

        then(config.getMethodConfig().getRateLimitConfig()).isNotNull();
        then(config.getMethodConfig().getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(config.getMethodConfig().getRateLimitConfig().getLimitForPeriod()).isEqualTo(500);

        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testRateLimitAliasSet() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodRateLimitAliasSet");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);
        assert config != null;
        then(config.getMethodConfig().getRateLimitConfig().getLimitForPeriod()).isEqualTo(1);
    }

    @Test
    void testRateLimitAliasSetError() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodRateLimitAliasSetError");
        assertThrows(IllegalArgumentException.class, () -> MethodUtils.getCompositeConfig(method));
    }

    @Test
    void testGetOnlyCircuitBreakerConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyCircuitBreaker");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNull();
        then(config.getMethodConfig().getCircuitBreakerConfig()).isNotNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNull();
        then(config.getMethodConfig().getRateLimitConfig()).isNull();

        then(config.getMethodConfig().getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(99);
        then(config.getMethodConfig().getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(9);
        then(config.getMethodConfig().getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(DurationUtils.parse("59s"));
        then(config.getMethodConfig().getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(config.getMethodConfig().getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(50);
        then(config.getMethodConfig().getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(49.0f);
        then(config.getMethodConfig().getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{IllegalStateException.class, IllegalArgumentException.class});

        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testCircuitBreakerAliasSet() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodCircuitBreakerAliasSet");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig().getFallbackConfig()).isNull();
        then(config.getMethodConfig().getCircuitBreakerConfig()).isNotNull();
        then(config.getMethodConfig().getConcurrentLimitConfig()).isNull();
        then(config.getMethodConfig().getRateLimitConfig()).isNull();

        then(config.getMethodConfig().getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(99);
        then(config.getMethodConfig().getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(9);
        then(config.getMethodConfig().getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(DurationUtils.parse("59s"));
        then(config.getMethodConfig().getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(config.getMethodConfig().getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(50);
        then(config.getMethodConfig().getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(49.0f);
        then(config.getMethodConfig().getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{IllegalStateException.class, IllegalArgumentException.class});

        then(config.getArgConfig().getArgConfigMap()).isEmpty();
    }

    @Test
    void testCircuitBreakerAliasSetError() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodCircuitBreakerAliasSetError");
        assertThrows(IllegalArgumentException.class, () -> MethodUtils.getCompositeConfig(method));
    }

    @Test
    void testRetryAliasSet() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodRetryAliasSet");
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig().getRetryConfig().getMaxAttempts()).isEqualTo(5);
    }

    @Test
    void testRetryAliasSetError() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodRetryAliasSetError");
        assertThrows(IllegalArgumentException.class, () -> MethodUtils.getCompositeConfig(method));
    }

    @Test
    void testGetOnlyArgsRateLimitConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyArgRateLimit", String.class);
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().size()).isEqualTo(1);
        CompositeServiceKeeperConfig.CompositeArgConfig argConfig = config.getArgConfig().getArgConfigMap().get(0);
        then(argConfig.getIndex()).isEqualTo(0);
        then(argConfig.getArgName()).isEqualTo(ParameterUtils.defaultName(0));
        then(argConfig.getTemplate().getFallbackConfig()).isNull();
        then(argConfig.getTemplate().getRateLimitConfig()).isNotNull();
        then(argConfig.getTemplate().getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(argConfig.getTemplate().getConcurrentLimitConfig()).isNull();
        then(argConfig.getTemplate().getCircuitBreakerConfig()).isNull();

        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();
        then(argConfig.getValueToConfig().get("LiMing").getFallbackConfig()).isNull();
        then(argConfig.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();

        then(argConfig.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(argConfig.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod()).isEqualTo(20);

        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(argConfig.getValueToConfig().get("ZhangSan").getFallbackConfig()).isNull();
        then(argConfig.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(argConfig.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(argConfig.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod()).isEqualTo(60);
    }

    @Test
    void testGetOnlyArgsConcurrentLimitConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyArgConcurrentLimit", String.class);
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().size()).isEqualTo(1);

        then(config.getArgConfig().getArgConfigMap().get(0).getArgName())
                .isEqualTo(ParameterUtils.defaultName(0));
        then(config.getArgConfig().getArgConfigMap().get(0).getIndex()).isEqualTo(0);
        then(config.getArgConfig().getArgConfigMap().get(0).getTemplate()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().size()).isEqualTo(2);

        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("ZhangSan")
                .getConcurrentLimitConfig().getThreshold()).isEqualTo(56);
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("ZhangSan")
                .getCircuitBreakerConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("ZhangSan")
                .getRateLimitConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("ZhangSan")
                .getFallbackConfig()).isNull();

        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("LiSi")
                .getConcurrentLimitConfig().getThreshold()).isEqualTo(23);
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("LiSi")
                .getCircuitBreakerConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("LiSi")
                .getRateLimitConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getValueToConfig().get("LiSi")
                .getFallbackConfig()).isNull();
    }

    @Test
    void testGetOnlyArgsCircuitBreakerConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodOnlyArgCircuitBreaker", String.class);
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);

        assert config != null;
        then(config.getMethodConfig()).isNull();
        then(config.getArgConfig().getArgConfigMap().size()).isEqualTo(1);
        CompositeServiceKeeperConfig.CompositeArgConfig argConfig = config.getArgConfig().getArgConfigMap().get(0);
        then(argConfig.getIndex()).isEqualTo(0);
        then(argConfig.getArgName()).isEqualTo(ParameterUtils.defaultName(0));
        then(argConfig.getTemplate().getFallbackConfig()).isNull();
        then(argConfig.getTemplate().getRateLimitConfig()).isNull();
        then(argConfig.getTemplate().getConcurrentLimitConfig()).isNull();
        then(argConfig.getTemplate().getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(101);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{RuntimeException.class});
        then(argConfig.getTemplate().getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(10);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(61));

        then(argConfig.getValueToConfig().get("LiMing").getFallbackConfig()).isNull();
        then(argConfig.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getRingBufferSizeInClosedState()).isEqualTo(101);
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{RuntimeException.class});
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(10);
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(61));
        then(argConfig.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(20.0f);

        then(argConfig.getValueToConfig().get("ZhangSan").getFallbackConfig()).isNull();
        then(argConfig.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getRingBufferSizeInClosedState()).isEqualTo(101);
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getRingBufferSizeInHalfOpenState()).isEqualTo(11);
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{RuntimeException.class});
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(10);
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(61));
        then(argConfig.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(60.0f);
    }

    @Test
    void testGetGroup() throws NoSuchMethodException {
        final Method method = mockClass.getDeclaredMethod("methodWithGroup");
        BDDAssertions.then(MethodUtils.getGroup(method)).isEqualTo(GroupResourceId.from("abc"));
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);
        assert config != null;
        then(config.getArgConfig().getArgConfigMap()).isEmpty();
        then(config.getMethodConfig()).isNull();
        then(config.getGroup()).isEqualTo(GroupResourceId.from("abc"));
    }

    @Test
    void testGetCompositeConfig() throws NoSuchMethodException {
        Method method = mockClass.getDeclaredMethod("methodWithAll", String.class, String.class);
        CompositeServiceKeeperConfig config = MethodUtils.getCompositeConfig(method);
        assert config != null;
        ServiceKeeperConfig methodConfig = config.getMethodConfig();
        then(MethodUtils.getMethodAlias(method)).isEqualTo("method-withAll");
        then(methodConfig.getFallbackConfig().getMethodName()).isEqualTo("fallbackMethod");
        then(methodConfig.getFallbackConfig().getTargetClass()).isEqualTo(MockMethods.class);
        then(methodConfig.getFallbackConfig().getSpecifiedException()).isEqualTo(RuntimeException.class);
        then(methodConfig.getFallbackConfig().getSpecifiedValue()).isEqualTo("FallbackValue");
        then(methodConfig.getRateLimitConfig().getLimitForPeriod()).isEqualTo(500);
        then(methodConfig.getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(DurationUtils.parse("2s"));
        then(methodConfig.getConcurrentLimitConfig().getThreshold()).isEqualTo(500);
        then(methodConfig.getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(9);
        then(methodConfig.getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(99);
        then(methodConfig.getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(DurationUtils.parse("59s"));
        then(methodConfig.getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(methodConfig.getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(50);
        then(methodConfig.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(49.0f);
        then(methodConfig.getCircuitBreakerConfig().getIgnoreExceptions())
                .isEqualTo(new Class[]{IllegalStateException.class, IllegalArgumentException.class});

        then(methodConfig.getRetryConfig().getMaxAttempts()).isEqualTo(10L);
        then(Arrays.equals(methodConfig.getRetryConfig().getIncludeExceptions(),
                new Class[]{RuntimeException.class})).isTrue();
        then(Arrays.equals(methodConfig.getRetryConfig().getExcludeExceptions(),
                new Class[]{IllegalStateException.class})).isTrue();
        then(methodConfig.getRetryConfig().getBackoffConfig().getDelay()).isEqualTo(10L);
        then(methodConfig.getRetryConfig().getBackoffConfig().getMaxDelay()).isEqualTo(100L);
        then(methodConfig.getRetryConfig().getBackoffConfig().getMultiplier()).isEqualTo(2.0d);

        CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = config.getArgConfig();
        then(argsConfig.getArgConfigMap().size()).isEqualTo(2);
        final CompositeServiceKeeperConfig.CompositeArgConfig arg0Config = argsConfig.getArgConfigMap().get(0);
        then(arg0Config.getArgName()).isEqualTo("name");
        then(arg0Config.getTemplate()).isNull();
        then(arg0Config.getIndex()).isEqualTo(0);
        then(arg0Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(arg0Config.getValueToConfig().get("ZhangSan").getRateLimitConfig()).isNull();
        then(arg0Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig().getThreshold()).isEqualTo(56);
        then(arg0Config.getValueToConfig().get("ZhangSan").getFallbackConfig()).isNull();

        then(arg0Config.getValueToConfig().get("LiSi").getCircuitBreakerConfig()).isNull();
        then(arg0Config.getValueToConfig().get("LiSi").getRateLimitConfig()).isNull();
        then(arg0Config.getValueToConfig().get("LiSi").getConcurrentLimitConfig().getThreshold()).isEqualTo(23);
        then(arg0Config.getValueToConfig().get("LiSi").getFallbackConfig()).isNull();

        final CompositeServiceKeeperConfig.CompositeArgConfig arg1Config = argsConfig.getArgConfigMap().get(1);
        then(arg1Config.getArgName()).isEqualTo("address");
        then(arg1Config.getTemplate().getFallbackConfig()).isNull();
        then(arg1Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg1Config.getTemplate().getCircuitBreakerConfig()).isNull();
        then(arg1Config.getTemplate().getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));

        then(arg1Config.getIndex()).isEqualTo(1);
        then(arg1Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(arg1Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod())
                .isEqualTo(60);
        then(arg1Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(arg1Config.getValueToConfig().get("ZhangSan").getFallbackConfig()).isNull();

        then(arg1Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();
        then(arg1Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitRefreshPeriod())
                .isEqualTo(DurationUtils.parse("2s"));
        then(arg1Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod())
                .isEqualTo(20);
        then(arg1Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(arg1Config.getValueToConfig().get("LiMing").getFallbackConfig()).isNull();
    }
}
