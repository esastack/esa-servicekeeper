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
package esa.servicekeeper.configsource.file.utils;

import esa.servicekeeper.configsource.file.constant.PropertyFileConstant;
import esa.servicekeeper.configsource.file.mock.CustomFallback;
import esa.servicekeeper.core.common.ArgConfigKey;
import esa.servicekeeper.core.common.ArgResourceId;
import esa.servicekeeper.core.common.GroupResourceId;
import esa.servicekeeper.core.common.ResourceId;
import esa.servicekeeper.core.configsource.ExternalConfig;
import esa.servicekeeper.core.configsource.ExternalGroupConfig;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime;
import esa.servicekeeper.core.utils.ClassCastUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static esa.servicekeeper.configsource.file.utils.MaxSizeLimitUtils.toKey;
import static org.assertj.core.api.BDDAssertions.then;

//com.example.service.DemoClass.demoMethod.maxConcurrentLimit=20

//com.example.service.DemoClass.demoMethod.includeExceptions=[java.lang.IllegalArgumentException,
// java.lang.IllegalStateException]
//com.example.service.DemoClass.demoMethod.excludeExceptions=[java.lang.IllegalArgumentException,
// java.lang.IllegalStateException]
//com.example.service.DemoClass.demoMethod.maxAttempts=5
//com.example.service.DemoClass.demoMethod.delay=50
//com.example.service.DemoClass.demoMethod.maxDelay=500
//com.example.service.DemoClass.demoMethod.multiplier=3.0
//
//com.example.service.DemoClass.demoMethod.limitForPeriod=600
//com.example.service.DemoClass.demoMethod.limitRefreshPeriod=2s
//com.example.service.DemoClass.demoMethod.maxTimeoutDuration=10ms
//com.example.service.DemoClass.demoMethod.failureRateThreshold=55.5
//com.example.service.DemoClass.demoMethod.maxSpendTimeMs=20
//com.example.service.DemoClass.demoMethod.ringBufferSizeInClosedState=100
//com.example.service.DemoClass.demoMethod.ringBufferSizeInHalfOpenState=10
//com.example.service.DemoClass.demoMethod.waitDurationInOpenState=10ms
//com.example.service.DemoClass.demoMethod.ignoreExceptions=[java.lang.IllegalArgumentException,
// java.lang.IllegalStateException]
//com.example.service.DemoClass.demoMethod.predicateStrategy=esa.servicekeeper.core.moats.circuitbreaker.predicate
// .PredicateByExceptionAndSpendTime
//com.example.service.DemoClass.demoMethod.applyScope=all
//com.example.service.DemoClass.demoMethod.fallbackValue=Custom fallbackHandler value
//com.example.service.DemoClass.demoMethod.fallbackExceptionClass=java.lang.RuntimeException
//com.example.service.DemoClass.demoMethod.fallbackMethod=fallbackMethod
//com.example.service.DemoClass.demoMethod.fallbackClass=CustomFallback
//com.example.service.DemoClass.demoMethod.forcedOpen=true
//com.example.service.DemoClass.demoMethod.forcedDisabled=true
//com.example.service.DemoClass.demoMethod.arg0.limitForPeriod={LiSi:20, ZhangSan : 50, wangwu: 30}
//com.example.service.DemoClass.demoMethod.arg0.maxConcurrentLimit={ZhangSan : 50, wangwu: 30, LiMing : 200}
//com.example.service.DemoClass.demoMethod.arg0.limitRefreshPeriod=5s
//com.example.service.DemoClass.demoMethod.arg0.maxTimeoutDuration=20ms
//
//servicekeeper.disable=true
//servicekeeper.arg.level.enable=true

//com.example.service.DemoClass.demoMethod.arg0.maxConcurrentLimitValueSize=99
//com.example.service.DemoClass.demoMethod.arg0.maxRateLimitValueSize=99
//com.example.service.DemoClass.demoMethod.arg0.maxCircuitBreakerValueSize=99

//group.groupA.items=[com.example.service.DemoClass.demoMethod, com.example.service.DemoClass.demoMethod1]
//group.groupA.maxConcurrentLimit=20
//group.groupA.limitForPeriod=600
//group.groupA.limitRefreshPeriod=2s
//group.groupA.maxTimeoutDuration=10ms
//group.groupA.failureRateThreshold=55.5
//group.groupA.maxSpendTimeMs=20
//group.groupA.ringBufferSizeInClosedState=100
//group.groupA.ringBufferSizeInHalfOpenState=10
//group.groupA.waitDurationInOpenState=10ms
//group.groupA.ignoreExceptions=[java.lang.IllegalArgumentException,java.lang.IllegalStateException]
//group.groupA.predicateStrategy=esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime
//group.groupA.applyScope=all
//group.groupA.fallbackValue=Custom fallbackHandler value
//group.groupA.fallbackExceptionClass=java.lang.RuntimeException
//group.groupA.fallbackMethod=fallbackMethod
//group.groupA.fallbackClass=CustomFallback
//group.groupA.forcedOpen=true
//group.groupA.forcedDisabled=true
class PropertiesUtilsTest {

    private Properties properties;

    @BeforeEach
    void setUp() throws Exception {
        properties = new Properties();
        properties.load(new FileInputStream(PropertyFileConstant.configDir() +
                File.separator + PropertyFileConstant.configName()));
    }

    @Test
    void testConfigs() {
        final Map<ResourceId, ExternalConfig> configs = PropertiesUtils.configs(properties);
        then(configs.size()).isEqualTo(6);
        final ResourceId resourceId1 = ResourceId.from("com.example.service.DemoClass.demoMethod");
        then(configs.get(resourceId1).getMaxConcurrentLimit()).isEqualTo(20);
        then(Arrays.equals(new Class[]{IllegalArgumentException.class, IllegalStateException.class},
                configs.get(resourceId1).getIncludeExceptions())).isTrue();
        then(Arrays.equals(new Class[]{IllegalArgumentException.class, IllegalStateException.class},
                configs.get(resourceId1).getExcludeExceptions())).isTrue();
        then(configs.get(resourceId1).getMaxAttempts()).isEqualTo(5);
        then(configs.get(resourceId1).getDelay()).isEqualTo(50);
        then(configs.get(resourceId1).getMaxDelay()).isEqualTo(500);
        then(configs.get(resourceId1).getMultiplier()).isEqualTo(3.0d);
        then(configs.get(resourceId1).getLimitForPeriod()).isEqualTo(600);
        then(configs.get(resourceId1).getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(2));
        then(configs.get(resourceId1).getFailureRateThreshold()).isEqualTo(55.5f);
        then(configs.get(resourceId1).getMaxSpendTimeMs()).isEqualTo(20);
        then(configs.get(resourceId1).getRingBufferSizeInClosedState()).isEqualTo(100);
        then(configs.get(resourceId1).getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(configs.get(resourceId1).getWaitDurationInOpenState()).isEqualTo(Duration.ofMillis(10));
        then(configs.get(resourceId1).getIgnoreExceptions()).isEqualTo(new Class[]{IllegalArgumentException.class,
                IllegalStateException.class});
        then(configs.get(resourceId1).getPredicateStrategy()).isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(configs.get(resourceId1).getFallbackValue()).isEqualTo("Custom fallbackHandler value");
        then(configs.get(resourceId1).getFallbackExceptionClass()).isEqualTo(RuntimeException.class);
        then(configs.get(resourceId1).getFallbackMethodName()).isEqualTo("fallbackMethod");
        then(configs.get(resourceId1).getAlsoApplyFallbackToBizException()).isEqualTo(true);
        then(configs.get(resourceId1).getFallbackClass()).isEqualTo(CustomFallback.class);
        then((configs.get(resourceId1).getForcedDisabled() == null
                && configs.get(resourceId1).getForcedOpen()) ||
                configs.get(resourceId1).getForcedDisabled()
                        && configs.get(resourceId1).getForcedOpen() == null).isTrue();

        final ResourceId resourceId2 = new ArgResourceId(
                "com.example.service.DemoClass.demoMethod.arg0", "ZhangSan");
        then(configs.get(resourceId2).getMaxConcurrentLimit()).isEqualTo(50);
        then(configs.get(resourceId2).getLimitForPeriod()).isEqualTo(50);
        then(configs.get(resourceId2).getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(5));
        then(configs.get(resourceId2).getFallbackMethodName()).isNull();

        final ResourceId resourceId3 = new ArgResourceId(
                "com.example.service.DemoClass.demoMethod.arg0", "LiSi");
        then(configs.get(resourceId3).getMaxConcurrentLimit()).isNull();
        then(configs.get(resourceId3).getLimitForPeriod()).isEqualTo(20);
        then(configs.get(resourceId3).getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(5));
        then(configs.get(resourceId3).getFallbackMethodName()).isNull();

        final ResourceId resourceId4 = new ArgResourceId(
                "com.example.service.DemoClass.demoMethod.arg0", "wangwu");
        then(configs.get(resourceId4).getMaxConcurrentLimit()).isEqualTo(30);
        then(configs.get(resourceId4).getLimitForPeriod()).isEqualTo(30);
        then(configs.get(resourceId4).getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(5));
        then(configs.get(resourceId4).getFallbackMethodName()).isNull();

        final ResourceId resourceId5 = new ArgResourceId(
                "com.example.service.DemoClass.demoMethod.arg0", "LiMing");
        then(configs.get(resourceId5).getMaxConcurrentLimit()).isEqualTo(200);
        then(configs.get(resourceId5).getLimitForPeriod()).isNull();
        then(configs.get(resourceId5).getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(5L));
        then(configs.get(resourceId5).getFallbackMethodName()).isNull();
    }

    @Test
    void testGetGlobalDisable() {
        then(PropertiesUtils.getGlobalDisable(properties)).isTrue();
    }

    @Test
    void testGetArgLevelEnable() {
        then(PropertiesUtils.getArgLevelEnable(properties)).isTrue();
    }

    @Test
    void testGetRetryEnable() {
        then(PropertiesUtils.getRetryEnable(properties)).isNull();
    }

    @Test
    void testMaxSizeLimits() {
        Map<ArgConfigKey, Integer> maxSizeLimits = PropertiesUtils.maxSizeLimits(properties);
        then(maxSizeLimits.size()).isEqualTo(3);
        then(maxSizeLimits.get(toKey("com.example.service.DemoClass.demoMethod" +
                ".arg0.maxConcurrentLimitValueSize")))
                .isEqualTo(99);
        then(maxSizeLimits.get(toKey("com.example.service.DemoClass.demoMethod.arg0" +
                ".maxRateLimitValueSize")))
                .isEqualTo(99);
        then(maxSizeLimits.get(toKey("com.example.service.DemoClass.demoMethod.arg0" +
                ".maxCircuitBreakerValueSize")))
                .isEqualTo(99);
    }

    @Test
    void testGroupConfig() {
        final Map<ResourceId, ExternalConfig> configs = PropertiesUtils.configs(properties);
        then(configs.size()).isEqualTo(6);
        final GroupResourceId groupId = GroupResourceId.from("groupA");
        ExternalGroupConfig groupConfig = (ExternalGroupConfig) configs.get(groupId);
        then(groupConfig.getMaxConcurrentLimit()).isEqualTo(20);
        then(groupConfig.getLimitForPeriod()).isEqualTo(600);
        then(groupConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(2));
        then(groupConfig.getFailureRateThreshold()).isEqualTo(55.5f);
        then(groupConfig.getMaxSpendTimeMs()).isEqualTo(20);
        then(groupConfig.getRingBufferSizeInClosedState()).isEqualTo(100);
        then(groupConfig.getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(groupConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofMillis(10));
        then(groupConfig.getIgnoreExceptions()).isEqualTo(new Class[]{IllegalArgumentException.class,
                IllegalStateException.class});
        then(groupConfig.getPredicateStrategy()).isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(groupConfig.getFallbackValue()).isEqualTo("Custom fallbackHandler value");
        then(groupConfig.getFallbackExceptionClass()).isEqualTo(RuntimeException.class);
        then(groupConfig.getFallbackMethodName()).isEqualTo("fallbackMethod");
        then(groupConfig.getFallbackClass()).isEqualTo(CustomFallback.class);
        then((groupConfig.getForcedDisabled() == null
                && groupConfig.getForcedOpen()) ||
                groupConfig.getForcedDisabled()
                        && groupConfig.getForcedOpen() == null).isTrue();
        then(groupConfig.getItems().size()).isEqualTo(2);
        then(groupConfig.getItems().contains(ResourceId.from("com.example.service.DemoClass.demoMethod")))
                .isTrue();
        then(groupConfig.getItems().contains(ResourceId.from("com.example.service.DemoClass.demoMethod1")))
                .isTrue();
    }

    @Test
    void testTryToFillArgConfigWithTemplate() {
        final ExternalConfig template1 = new ExternalConfig();
        final ExternalConfig argConfig1 = new ExternalConfig();

        then(PropertiesUtils.tryToFillArgConfigWithTemplate(template1, argConfig1)).isSameAs(argConfig1);
        then(argConfig1.getLimitRefreshPeriod()).isNull();
        then(argConfig1.getRingBufferSizeInClosedState()).isNull();
        then(argConfig1.getRingBufferSizeInHalfOpenState()).isNull();
        then(argConfig1.getWaitDurationInOpenState()).isNull();
        then(argConfig1.getIgnoreExceptions()).isNull();
        then(argConfig1.getPredicateStrategy()).isNull();
        then(argConfig1.getMaxSpendTimeMs()).isNull();

        template1.setLimitRefreshPeriod(Duration.ofSeconds(20L));
        template1.setRingBufferSizeInClosedState(101);
        template1.setRingBufferSizeInHalfOpenState(99);
        template1.setWaitDurationInOpenState(Duration.ofSeconds(110L));
        template1.setIgnoreExceptions(ClassCastUtils.cast(new Class[]{RuntimeException.class}));
        template1.setPredicateStrategy(PredicateByExceptionAndSpendTime.class);
        template1.setMaxSpendTimeMs(66L);

        then(PropertiesUtils.tryToFillArgConfigWithTemplate(template1, argConfig1)).isSameAs(argConfig1);
        then(argConfig1.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(20L));
        then(argConfig1.getRingBufferSizeInClosedState()).isEqualTo(101);
        then(argConfig1.getRingBufferSizeInHalfOpenState()).isEqualTo(99);
        then(argConfig1.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(110L));
        then(Arrays.equals(argConfig1.getIgnoreExceptions(),
                ClassCastUtils.cast(new Class[]{RuntimeException.class}))).isTrue();
        then(argConfig1.getPredicateStrategy()).isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(argConfig1.getMaxSpendTimeMs()).isEqualTo(66L);

        final ExternalConfig template2 = new ExternalConfig();
        final ExternalConfig argConfig2 = new ExternalConfig();
        argConfig2.setLimitRefreshPeriod(Duration.ofSeconds(20L));
        argConfig2.setRingBufferSizeInClosedState(101);
        argConfig2.setRingBufferSizeInHalfOpenState(99);
        argConfig2.setWaitDurationInOpenState(Duration.ofSeconds(110L));
        argConfig2.setIgnoreExceptions(ClassCastUtils.cast(new Class[]{RuntimeException.class}));
        argConfig2.setPredicateStrategy(PredicateByExceptionAndSpendTime.class);
        argConfig2.setMaxSpendTimeMs(66L);

        template2.setLimitRefreshPeriod(Duration.ofSeconds(200L));
        template2.setRingBufferSizeInClosedState(1010);
        template2.setRingBufferSizeInHalfOpenState(990);
        template2.setWaitDurationInOpenState(Duration.ofSeconds(1100L));
        template2.setIgnoreExceptions(ClassCastUtils.cast(new Class[]{RuntimeException.class, Exception.class}));
        template2.setPredicateStrategy(PredicateByException.class);
        template2.setMaxSpendTimeMs(660L);

        then(PropertiesUtils.tryToFillArgConfigWithTemplate(template2, argConfig2)).isSameAs(argConfig2);
        then(argConfig2.getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(20L));
        then(argConfig2.getRingBufferSizeInClosedState()).isEqualTo(101);
        then(argConfig2.getRingBufferSizeInHalfOpenState()).isEqualTo(99);
        then(argConfig2.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(110L));
        then(Arrays.equals(argConfig2.getIgnoreExceptions(),
                ClassCastUtils.cast(new Class[]{RuntimeException.class}))).isTrue();
        then(argConfig2.getPredicateStrategy()).isEqualTo(PredicateByExceptionAndSpendTime.class);
        then(argConfig2.getMaxSpendTimeMs()).isEqualTo(66L);
    }

}
