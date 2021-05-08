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

import esa.servicekeeper.core.config.CircuitBreakerConfig;
import esa.servicekeeper.core.config.ConcurrentLimitConfig;
import esa.servicekeeper.core.config.FallbackConfig;
import esa.servicekeeper.core.config.RateLimitConfig;
import esa.servicekeeper.core.config.ServiceKeeperConfig;
import esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException;
import esa.servicekeeper.core.utils.DurationUtils;
import esa.servicekeeper.core.utils.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompositeServiceKeeperConfigTest {

    @Test
    void testMethodLevelConfig() {
        ServiceKeeperConfig methodLevelConfig = ServiceKeeperConfig.builder()
                .fallbackConfig(FallbackConfig.ofDefault())
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                .rateLimiterConfig(RateLimitConfig.ofDefault())
                .concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault()).build();
        CompositeServiceKeeperConfig compositeConfig = CompositeServiceKeeperConfig.builder()
                .methodConfig(methodLevelConfig).build();
        then(compositeConfig.getMethodConfig()).isNotNull();
        then(compositeConfig.getMethodConfig().getRateLimitConfig()).isNotNull();
        then(compositeConfig.getMethodConfig().getConcurrentLimitConfig()).isNotNull();
        then(compositeConfig.getMethodConfig().getCircuitBreakerConfig()).isNotNull();
        then(compositeConfig.getMethodConfig().getFallbackConfig()).isNotNull();
    }

    @Test
    void testOnlySetArgName() {
        // ConcurrentLimit Config
        CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder()
                .argConcurrentLimit(0, "arg0", null, null).build();
        then(config.getArgConfig().getArgConfigMap().get(0)).isNull();

        config = CompositeServiceKeeperConfig.builder()
                .argConcurrentLimit(0, "testOnlySetArgName", null, null)
                .build();
        CompositeServiceKeeperConfig.CompositeArgConfig argConfig = config.getArgConfig().getArgConfigMap().get(0);
        then(argConfig.getTemplate()).isNull();
        then(argConfig.getValueToConfig()).isEmpty();
        then(argConfig.getIndex()).isEqualTo(0);
        then(argConfig.getMaxCircuitBreakerSizeLimit()).isNull();
        then(argConfig.getMaxConcurrentLimitSizeLimit()).isNull();
        then(argConfig.getMaxRateLimitSizeLimit()).isNull();
        then(argConfig.getArgName()).isEqualTo("testOnlySetArgName");

        // RateLimit Config
        config = CompositeServiceKeeperConfig.builder().argRateLimitConfig(1, "arg1", null,
                null, null).build();
        then(config.getArgConfig().getArgConfigMap().get(1)).isNull();

        config = CompositeServiceKeeperConfig.builder().argRateLimitConfig(1, "testOnlySetArgName",
                null, null, null).build();
        argConfig = config.getArgConfig().getArgConfigMap().get(1);
        then(argConfig.getTemplate()).isNull();
        then(argConfig.getValueToConfig()).isEmpty();
        then(argConfig.getIndex()).isEqualTo(1);
        then(argConfig.getMaxCircuitBreakerSizeLimit()).isNull();
        then(argConfig.getMaxConcurrentLimitSizeLimit()).isNull();
        then(argConfig.getMaxRateLimitSizeLimit()).isNull();
        then(argConfig.getArgName()).isEqualTo("testOnlySetArgName");

        // CircuitBreaker Config
        config = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(2, "arg2", null, null,
                        null).build();
        then(config.getArgConfig().getArgConfigMap().get(2)).isNull();

        config = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(2, "testOnlySetArgName", null,
                        null, null).build();
        argConfig = config.getArgConfig().getArgConfigMap().get(2);
        then(argConfig.getTemplate()).isNull();
        then(argConfig.getValueToConfig()).isEmpty();
        then(argConfig.getIndex()).isEqualTo(2);
        then(argConfig.getMaxCircuitBreakerSizeLimit()).isNull();
        then(argConfig.getMaxConcurrentLimitSizeLimit()).isNull();
        then(argConfig.getMaxRateLimitSizeLimit()).isNull();
        then(argConfig.getArgName()).isEqualTo("testOnlySetArgName");
    }

    @Test
    void testOnlySetMaxValueSize() {
        // ConcurrentLimit Config
        int maxValueSize = RandomUtils.randomInt(200);
        CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder().argConcurrentLimit(0,
                "arg0", null, null).build();
        then(config.getArgConfig().getArgConfigMap().get(0)).isNull();
        config = CompositeServiceKeeperConfig.builder().argConcurrentLimit(0, "arg0",
                null, maxValueSize).build();
        then(config.getArgConfig().getArgConfigMap().get(0).getMaxConcurrentLimitSizeLimit()).isEqualTo(maxValueSize);
        then(config.getArgConfig().getArgConfigMap().get(0).getMaxCircuitBreakerSizeLimit()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(0).getMaxRateLimitSizeLimit()).isNull();

        // RateLimit Config
        maxValueSize = RandomUtils.randomInt(200);
        config = CompositeServiceKeeperConfig.builder().argRateLimitConfig(1, "arg1",
                null, null, null).build();
        then(config.getArgConfig().getArgConfigMap().get(1)).isNull();
        config = CompositeServiceKeeperConfig.builder().argRateLimitConfig(1, "arg1",
                null, null, maxValueSize).build();
        then(config.getArgConfig().getArgConfigMap().get(1).getMaxRateLimitSizeLimit()).isEqualTo(maxValueSize);
        then(config.getArgConfig().getArgConfigMap().get(1).getMaxCircuitBreakerSizeLimit()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(1).getMaxConcurrentLimitSizeLimit()).isNull();

        // CircuitBreaker Config
        maxValueSize = RandomUtils.randomInt(200);
        config = CompositeServiceKeeperConfig.builder().argCircuitBreakerConfig(2, "arg2",
                null, null, null).build();
        then(config.getArgConfig().getArgConfigMap().get(2)).isNull();
        config = CompositeServiceKeeperConfig.builder().argCircuitBreakerConfig(2, "arg2",
                null, null, maxValueSize).build();
        then(config.getArgConfig().getArgConfigMap().get(2).getMaxCircuitBreakerSizeLimit()).isEqualTo(maxValueSize);
        then(config.getArgConfig().getArgConfigMap().get(2).getMaxRateLimitSizeLimit()).isNull();
        then(config.getArgConfig().getArgConfigMap().get(2).getMaxConcurrentLimitSizeLimit()).isNull();
    }

    @Test
    void testOnlySetRateLimitTemplate() {
        int limitForPeriod = RandomUtils.randomInt(1000);
        String limitRefreshPeriod = "20s";
        RateLimitConfig template = RateLimitConfig.builder()
                .limitRefreshPeriod(DurationUtils.parse(limitRefreshPeriod))
                .limitForPeriod(limitForPeriod).build();
        CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder()
                .argRateLimitConfig(1, "arg1", template, Collections.emptyMap(), null)
                .build();
        CompositeServiceKeeperConfig.CompositeArgConfig argConfig = config.getArgConfig().getArgConfigMap().get(1);
        then(argConfig.getValueToConfig()).isEmpty();
        then(argConfig.getArgName()).isEqualTo("arg1");
        then(argConfig.getIndex()).isEqualTo(1);
        then(argConfig.getMaxConcurrentLimitSizeLimit()).isNull();
        then(argConfig.getMaxRateLimitSizeLimit()).isNull();
        then(argConfig.getMaxCircuitBreakerSizeLimit()).isNull();
        then(argConfig.getTemplate().getCircuitBreakerConfig()).isNull();
        then(argConfig.getTemplate().getConcurrentLimitConfig()).isNull();
        then(argConfig.getTemplate().getRetryConfig()).isNull();
        then(argConfig.getTemplate().getFallbackConfig()).isNull();
        then(argConfig.getTemplate().getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(20));
        then(argConfig.getTemplate().getRateLimitConfig().getLimitForPeriod()).isEqualTo(limitForPeriod);
    }

    @Test
    void testOnlySetCircuitBreakerTemplate() {
        float failureRateThreshold = RandomUtils.randomFloat(100);
        int ringBufferSizeInHalfOpenState = RandomUtils.randomInt(200);
        int ringBufferSizeInClosedState = RandomUtils.randomInt(200);
        CircuitBreakerConfig template = CircuitBreakerConfig.builder()
                .ringBufferSizeInHalfOpenState(ringBufferSizeInHalfOpenState)
                .ringBufferSizeInClosedState(ringBufferSizeInClosedState)
                .failureRateThreshold(failureRateThreshold).build();
        CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder().argCircuitBreakerConfig(2,
                "arg2", template, Collections.emptyMap(), null).build();

        CompositeServiceKeeperConfig.CompositeArgConfig argConfig = config.getArgConfig().getArgConfigMap().get(2);
        then(argConfig.getValueToConfig()).isEmpty();
        then(argConfig.getArgName()).isEqualTo("arg2");
        then(argConfig.getIndex()).isEqualTo(2);
        then(argConfig.getMaxConcurrentLimitSizeLimit()).isNull();
        then(argConfig.getMaxRateLimitSizeLimit()).isNull();
        then(argConfig.getMaxCircuitBreakerSizeLimit()).isNull();
        then(argConfig.getTemplate().getConcurrentLimitConfig()).isNull();
        then(argConfig.getTemplate().getRetryConfig()).isNull();
        then(argConfig.getTemplate().getFallbackConfig()).isNull();
        then(argConfig.getTemplate().getRateLimitConfig()).isNull();
        then(argConfig.getTemplate().getCircuitBreakerConfig().getFailureRateThreshold())
                .isEqualTo(failureRateThreshold);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState())
                .isEqualTo(ringBufferSizeInHalfOpenState);
        then(argConfig.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInClosedState())
                .isEqualTo(ringBufferSizeInClosedState);
    }

    @Test
    void testAddArgConcurrentLimit() {
        final Map<Object, Integer> thresholdMap = new LinkedHashMap<>(6);
        thresholdMap.put("LiMing", 10);
        thresholdMap.put("ZhangSan", 20);
        thresholdMap.put("WangWu", 30);

        CompositeServiceKeeperConfig config = CompositeServiceKeeperConfig.builder()
                .argConcurrentLimit(0, Collections.emptyMap())
                .argConcurrentLimit(1, Collections.emptyMap())
                .argConcurrentLimit(2, "name", thresholdMap)
                .argConcurrentLimit(3, "address", thresholdMap).build();

        then(config.getMethodConfig()).isNull();
        final CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = config.getArgConfig();
        then(argsConfig.getArgConfigMap().size()).isEqualTo(2);

        // Config of arg0
        final CompositeServiceKeeperConfig.CompositeArgConfig arg0Config = argsConfig.getArgConfigMap().get(0);
        then(arg0Config).isNull();

        // Config of arg1
        final CompositeServiceKeeperConfig.CompositeArgConfig arg1Config = argsConfig.getArgConfigMap().get(1);
        then(arg1Config).isNull();

        // Config of arg2
        final CompositeServiceKeeperConfig.CompositeArgConfig arg2Config = argsConfig.getArgConfigMap().get(2);
        then(arg2Config.getIndex()).isEqualTo(2);
        then(arg2Config.getArgName()).isEqualTo("name");
        then(arg2Config.getTemplate()).isNull();
        then(arg2Config.getValueToConfig().size()).isEqualTo(3);

        then(arg2Config.getValueToConfig().get("LiMing").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getFallbackConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig().getThreshold()).isEqualTo(10);

        then(arg2Config.getValueToConfig().get("ZhangSan").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getFallbackConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig().getThreshold()).isEqualTo(20);

        then(arg2Config.getValueToConfig().get("WangWu").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig().getThreshold()).isEqualTo(30);

        // Config of arg3
        final CompositeServiceKeeperConfig.CompositeArgConfig arg3Config = argsConfig.getArgConfigMap().get(3);
        then(arg3Config.getIndex()).isEqualTo(3);
        then(arg3Config.getArgName()).isEqualTo("address");
        then(arg3Config.getTemplate()).isNull();
        then(arg3Config.getValueToConfig().size()).isEqualTo(3);

        then(arg3Config.getValueToConfig().get("LiMing").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig().getThreshold()).isEqualTo(10);

        then(arg3Config.getValueToConfig().get("ZhangSan").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig().getThreshold()).isEqualTo(20);

        then(arg3Config.getValueToConfig().get("WangWu").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig().getThreshold()).isEqualTo(30);
    }

    @Test
    void testAddRateLimit() {
        final Map<Object, Integer> thresholdMap = new LinkedHashMap<>(6);
        thresholdMap.put("LiMing", 10);
        thresholdMap.put("ZhangSan", 20);
        thresholdMap.put("WangWu", 30);

        CompositeServiceKeeperConfig compositeConfig = CompositeServiceKeeperConfig.builder()
                .argRateLimitConfig(0, Collections.emptyMap())
                .argRateLimitConfig(1, "arg1", Collections.emptyMap())
                .argRateLimitConfig(2, "name", thresholdMap)
                .argRateLimitConfig(3, "address", RateLimitConfig.ofDefault(), thresholdMap)
                .build();

        assertNull(compositeConfig.getMethodConfig());
        final CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = compositeConfig.getArgConfig();
        then(argsConfig.getArgConfigMap().size()).isEqualTo(2);

        // Config of arg0
        then(argsConfig.getArgConfigMap().get(0)).isNull();

        // Config of arg1
        then(argsConfig.getArgConfigMap().get(1)).isNull();

        // Config of arg2
        final CompositeServiceKeeperConfig.CompositeArgConfig arg2Config = argsConfig.getArgConfigMap().get(2);
        then(arg2Config.getIndex()).isEqualTo(2);
        then(arg2Config.getArgName()).isEqualTo("name");
        then(arg2Config.getTemplate()).isNull();

        then(arg2Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod()).isEqualTo(10);

        then(arg2Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod()).isEqualTo(20);

        then(arg2Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getRateLimitConfig().getLimitForPeriod()).isEqualTo(30);

        // Config of arg3
        final CompositeServiceKeeperConfig.CompositeArgConfig arg3Config = argsConfig.getArgConfigMap().get(3);
        then(arg3Config.getIndex()).isEqualTo(3);
        then(arg3Config.getArgName()).isEqualTo("address");
        then(arg3Config.getTemplate().getCircuitBreakerConfig()).isNull();
        then(arg3Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg3Config.getTemplate().getRateLimitConfig().getLimitForPeriod()).isEqualTo(Integer.MAX_VALUE);
        then(arg3Config.getTemplate().getRateLimitConfig().getLimitRefreshPeriod()).isEqualTo(Duration.ofSeconds(1L));

        then(arg3Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod()).isEqualTo(10);

        then(arg3Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod()).isEqualTo(20);

        then(arg3Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getRateLimitConfig().getLimitForPeriod()).isEqualTo(30);
    }

    @Test
    void testAddCircuitBreaker() {
        final Map<Object, Float> failureRateThresholdMap = new LinkedHashMap<>(6);
        failureRateThresholdMap.put("LiMing", 10.0f);
        failureRateThresholdMap.put("ZhangSan", 20.0f);
        failureRateThresholdMap.put("WangWu", 30.0f);

        CompositeServiceKeeperConfig compositeConfig = CompositeServiceKeeperConfig.builder()
                .argCircuitBreakerConfig(0, Collections.emptyMap())
                .argCircuitBreakerConfig(1, "arg1", Collections.emptyMap())
                .argCircuitBreakerConfig(2, "name", failureRateThresholdMap)
                .argCircuitBreakerConfig(3, "address", CircuitBreakerConfig.ofDefault(),
                        failureRateThresholdMap)
                .build();

        assertNull(compositeConfig.getMethodConfig());
        final CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = compositeConfig.getArgConfig();
        then(argsConfig.getArgConfigMap().size()).isEqualTo(2);

        // Config of arg0
        then(argsConfig.getArgConfigMap().get(0)).isNull();

        // Config of arg1
        then(argsConfig.getArgConfigMap().get(1)).isNull();

        // Config of arg2
        final CompositeServiceKeeperConfig.CompositeArgConfig arg2Config = argsConfig.getArgConfigMap().get(2);
        then(arg2Config.getIndex()).isEqualTo(2);
        then(arg2Config.getArgName()).isEqualTo("name");
        then(arg2Config.getTemplate()).isNull();

        then(arg2Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(10.0f);

        then(arg2Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(20.0f);

        then(arg2Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getRateLimitConfig()).isNull();
        then(arg2Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(30.0f);

        // Config of arg3
        final CompositeServiceKeeperConfig.CompositeArgConfig arg3Config = argsConfig.getArgConfigMap().get(3);
        then(arg3Config.getIndex()).isEqualTo(3);
        then(arg3Config.getArgName()).isEqualTo("address");
        then(arg3Config.getTemplate().getRateLimitConfig()).isNull();
        then(arg3Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getRingBufferSizeInClosedState()).isEqualTo(100);
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(60));
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(-1);
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByException.class);
        then(arg3Config.getTemplate().getCircuitBreakerConfig().getIgnoreExceptions()).isEqualTo(new Class[0]);

        then(arg3Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getRingBufferSizeInClosedState()).isEqualTo(100);
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(60));
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(-1);
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByException.class);
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getIgnoreExceptions()).isEqualTo(new Class[0]);
        then(arg3Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(10.0f);

        then(arg3Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getRingBufferSizeInClosedState()).isEqualTo(100);
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(60));
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(-1);
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByException.class);
        then(arg3Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(20.0f);

        then(arg3Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getRateLimitConfig()).isNull();
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()
                .getRingBufferSizeInHalfOpenState()).isEqualTo(10);
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()
                .getRingBufferSizeInClosedState()).isEqualTo(100);
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig().getWaitDurationInOpenState())
                .isEqualTo(Duration.ofSeconds(60));
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig().getMaxSpendTimeMs()).isEqualTo(-1);
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig().getPredicateStrategy())
                .isEqualTo(PredicateByException.class);
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()
                .getIgnoreExceptions()).isEqualTo(new Class[0]);
        then(arg3Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()
                .getFailureRateThreshold()).isEqualTo(30.0f);
    }

    @Test
    void testMerged() {
        final Map<Object, Integer> thresholdMap = new LinkedHashMap<>(6);
        thresholdMap.put("LiMing", 10);
        thresholdMap.put("ZhangSan", 20);
        thresholdMap.put("WangWu", 30);

        CompositeServiceKeeperConfig compositeConfig = CompositeServiceKeeperConfig.builder()
                .methodConfig(ServiceKeeperConfig.builder().concurrentLimiterConfig(ConcurrentLimitConfig.ofDefault())
                        .rateLimiterConfig(RateLimitConfig.ofDefault())
                        .circuitBreakerConfig(CircuitBreakerConfig.ofDefault())
                        .fallbackConfig(FallbackConfig.ofDefault()).build())
                .argConcurrentLimit(0, Collections.emptyMap())
                .argRateLimitConfig(0, "arg0", RateLimitConfig.ofDefault(), Collections.emptyMap())
                .argConcurrentLimit(1, "arg1", thresholdMap)
                .argRateLimitConfig(1, "name", RateLimitConfig.ofDefault(), thresholdMap)
                .argConcurrentLimit(2, "address", thresholdMap)
                .argRateLimitConfig(2, "arg0", RateLimitConfig.ofDefault(), thresholdMap).build();

        ServiceKeeperConfig methodLevelConfig = compositeConfig.getMethodConfig();
        then(methodLevelConfig).isNotNull();

        final CompositeServiceKeeperConfig.ArgsServiceKeeperConfig argsConfig = compositeConfig.getArgConfig();
        then(argsConfig.getArgConfigMap().size()).isEqualTo(3);

        // Config of arg0
        final CompositeServiceKeeperConfig.CompositeArgConfig arg0Config = argsConfig.getArgConfigMap().get(0);
        then(arg0Config.getIndex()).isEqualTo(0);
        then(arg0Config.getArgName()).isEqualTo("arg0");
        then(arg0Config.getTemplate().getRateLimitConfig()).isNotNull();
        then(arg0Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg0Config.getTemplate().getCircuitBreakerConfig()).isNull();

        // Config of arg1
        final CompositeServiceKeeperConfig.CompositeArgConfig arg1Config = argsConfig.getArgConfigMap().get(1);
        assertEquals(1, arg1Config.getIndex());
        then(arg1Config.getIndex()).isEqualTo(1);
        then(arg1Config.getArgName()).isEqualTo("arg1");
        then(arg1Config.getTemplate().getRateLimitConfig()).isNotNull();
        then(arg1Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg1Config.getTemplate().getCircuitBreakerConfig()).isNull();

        then(arg1Config.getValueToConfig().size()).isEqualTo(3);
        then(arg1Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod()).isEqualTo(10);
        then(arg1Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig().getThreshold()).isEqualTo(10);
        then(arg1Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();

        then(arg1Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod()).isEqualTo(20);
        then(arg1Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig().getThreshold()).isEqualTo(20);
        then(arg1Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();

        then(arg1Config.getValueToConfig().get("WangWu").getRateLimitConfig().getLimitForPeriod()).isEqualTo(30);
        then(arg1Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig().getThreshold()).isEqualTo(30);
        then(arg1Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();

        // Config of arg2
        final CompositeServiceKeeperConfig.CompositeArgConfig arg2Config = argsConfig.getArgConfigMap().get(2);
        then(arg2Config.getIndex()).isEqualTo(2);
        then(arg2Config.getArgName()).isEqualTo("address");
        then(arg2Config.getTemplate().getRateLimitConfig()).isNotNull();
        then(arg2Config.getTemplate().getConcurrentLimitConfig()).isNull();
        then(arg2Config.getTemplate().getCircuitBreakerConfig()).isNull();

        then(arg2Config.getValueToConfig().size()).isEqualTo(3);
        then(arg2Config.getValueToConfig().get("LiMing").getRateLimitConfig().getLimitForPeriod()).isEqualTo(10);
        then(arg2Config.getValueToConfig().get("LiMing").getConcurrentLimitConfig().getThreshold()).isEqualTo(10);
        then(arg2Config.getValueToConfig().get("LiMing").getCircuitBreakerConfig()).isNull();

        then(arg2Config.getValueToConfig().get("ZhangSan").getRateLimitConfig().getLimitForPeriod()).isEqualTo(20);
        then(arg2Config.getValueToConfig().get("ZhangSan").getConcurrentLimitConfig().getThreshold()).isEqualTo(20);
        then(arg2Config.getValueToConfig().get("ZhangSan").getCircuitBreakerConfig()).isNull();

        then(arg2Config.getValueToConfig().get("WangWu").getRateLimitConfig().getLimitForPeriod()).isEqualTo(30);
        then(arg2Config.getValueToConfig().get("WangWu").getConcurrentLimitConfig().getThreshold()).isEqualTo(30);
        then(arg2Config.getValueToConfig().get("WangWu").getCircuitBreakerConfig()).isNull();
    }

    @Test
    void testMathAll() {
        final Map<Object, Integer> thresholdMap = new LinkedHashMap<>(6);
        thresholdMap.put("*", 10);
        thresholdMap.put("ZhangSan", 20);

        CompositeServiceKeeperConfig compositeConfig = CompositeServiceKeeperConfig.builder()
                .argRateLimitConfig(0, Collections.emptyMap())
                .argRateLimitConfig(1, "arg1", Collections.emptyMap())
                .argRateLimitConfig(2, "name", thresholdMap)
                .argRateLimitConfig(3, "address", RateLimitConfig.ofDefault(), thresholdMap)
                .build();

        assertNull(compositeConfig.getMethodConfig());
    }
}
