---
sort: 14
---

# 指标统计

### 方式一：SpringBoot Actuator
`Service Keeper`通过自定义Spring Boot Actuator提供了服务状态统计和实时配置查询的接口，该功能仅支持使用`Spring Boot`和`Restlight`两种方式接入的应用（目前只支持Spring Boot2.x版本）。

#### 接入步骤

##### Step1： 引入相关依赖
```xml
<dependency>
   <groupId>io.esastack</groupId>
   <artifactId>servicekeeper-metrics-actuator</artifactId>
   <version>${servicekeeper.version}</version>
</dependency>
```

##### Step2： 服务状态统计：
请求示例：http://127.0.0.1:8080/actuator/skmetricses
返回结果示例：
```json
{
    "io.esastack.servicekeeper.keepered.test.controller.composite.CompositeTestController.list": {
        "concurrentLimitMetrics": {
            "maxConcurrentLimit": 1000,
            "currentCallCounter": 0
        },
        "rateLimitMetrics": {
            "availablePermissions": 9992,
            "waitingThreads": 0
        },
        "circuitBreakerMetrics": {
            "failureThreshold": 50,
            "maxNumberOfBufferedCalls": 2,
            "numberOfBufferedCalls": 2,
            "numberOfSuccessfulCalls": 1,
            "numberOfFailedCalls": 1,
            "numberOfNotPermittedCalls": 7,
            "state": "OPEN"
        },
	"retryMetrics": {
	    "hasRetryTimes": 2,
	    "totalRetryCount": 8
	}
    }
}
```
结果中包括，所有方法对应的限流器、熔断器的状态。

##### Step3：  实时配置查询：
请求示例：http://127.0.0.1:8080/actuator/skconfigs
返回结果示例：
```json
{
    "io.esastack.servicekeeper.keepered.test.controller.composite.CompositeTestController.list": {
        "circuitBreakerConfig": null,
        "concurrentLimitConfig": {
            "threshold": 1000
        },
        "rateLimitConfig": {
            "limitRefreshPeriod": "1m",
            "timeoutDuration": "0ns",
            "limitForPeriod": 10000
        },
        "fallbackConfig": {
            "methodName": "fallback",
            "targetClass": "io.esastack.servicekeeper.keepered.test.controller.fallback.CustomizeFallback",
            "specifiedValue": "Hello ServiceKeeper!",
            "specifiedExceptionClass": "java.lang.RuntimeException",
            "scope": "ALL"
        },
	"retryConfig":{
	   "backoffConfig":	
		{
		    "delay":100,
                    "maxDelay":0,
                    "multiplier":1.0
                },
	   "excludeExceptions":[],
	   "includeExceptions":["java.lang.Exception"],
           "maxAttempts":5
      }
    }
}
```
结果中包含所有方法对应的熔断器、限流器、降级策略等配置。
