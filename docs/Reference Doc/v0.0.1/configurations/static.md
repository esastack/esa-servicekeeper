---
sort: 2
---

# 静态参数配置

除了在注解中配置服务治理规则外，`Service Keeper`也支持直接在service-keeper.properties中配置所有的服务治理规则。此类配置只会在生成相应的限流器、熔断器等组件时读取一次，如果相应组件已经存在则配置不会生效。建议在启动前或者动态生成相应组件时配置此类参数，具体列表如下：

| 参数名称         |      类型    |        描述      |     默认值   |  示例值   |  注意事项   |                                             
| --------------- |   :--------  | :-------- |   -------   | -------   | -------    |
|  limitRefreshPeriod  |  String     |        限流周期       |  1s | 10ms,10s,10m,10h... |    |  
|  ringBufferSizeInClosedState    |  int |        熔断器关闭状态下ring buffer大小    |  100 | 100  |  | 
|  ringBufferSizeInHalfOpenState  |  int  |      熔断器关闭状态下ring buffer大小     |   10 |10   |   |
|  waitDurationInOpenState        |  String  |      熔断器从打开切换到半开的时长     |  60s |10ms,10s,10m,10h...|  | 
|  ignoreExceptions    |  Class<? extends Throwable  |      可以忽略的异常      |  Class[0] |     [java.lang.RuntimeException, java.lang.IllegalArgumentException]            |           |
|  predicateStrategy   |  Class<? extends PredicateStrategy  |       熔断策略     |   esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByException |esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime |    |  
|  fallbackMethod      |  String     |       降级方法名       |  ""  |   list    |    |  
|  fallbackClass       |  Class<?>   |        降级类全限定名    |   null  |   CustomizeFallback.class  |  | 
|  fallbackValue       |  String     |      指定的降级值        |   null  |   Fallback    |        |
|  fallbackExceptionClass    | Class<? extends Throwable>  |   null   |  指定降级异常类的全限定名    |  java.lang.RuntimeException |  |
|  items              |  String     |      指定组包含的接口        |   null  |   [interface1,interface2]    |        |

完整的配置文件示例如下：
```properties
esa.servicekeeper.TestController.list.limitForPeriod=10000
esa.servicekeeper.TestController.list.maxConcurrentLimit=1000
esa.servicekeeper.TestController.list.limitRefreshPeriod=60s

esa.servicekeeper.TestController.list.maxTimeoutDuration=0s
esa.servicekeeper.TestController.list.failureRateThreshold=1
esa.servicekeeper.TestController.list.ringBufferSizeInClosedState=2

esa.servicekeeper.TestController.list.ringBufferSizeInHalfOpenState=2
esa.servicekeeper.TestController.list.waitDurationInOpenState=59s
esa.servicekeeper.TestController.list.ignoreExceptions=[java.lang.RuntimeException, java.lang.IllegalArgumentException]

esa.servicekeeper.TestController.list.maxSpendTimeMs=100
esa.servicekeeper.TestController.list.predicateStrategy=\
  esa.servicekeeper.core.moats.circuitbreaker.predicate.PredicateByExceptionAndSpendTime
esa.servicekeeper.TestController.list.fallbackClass=\
  esa.servicekeeper.keepered.test.controller.fallback.CustomizeFallback

esa.servicekeeper.TestController.list.fallbackMethod=fallback

esa.servicekeeper.TestController.list.fallbackValue=Hello ServiceKeeper!
esa.servicekeeper.TestController.list.fallbackExceptionClass=java.lang.RuntimeException
```
