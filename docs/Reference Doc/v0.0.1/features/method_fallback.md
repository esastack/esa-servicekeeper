---
sort: 4
---

# 方法降级

### 概述
通常来说，在服务不稳定时仍然坚持访问肯定是不明智的，此时可使用`Service Keeper`的降级功能，避免持续访问不稳定资源导致级联错误，等待服务恢复后再正常访问。`Service Keeper`提供了三种不同的降级策略，包括：降级到指定方法、返回固定值(String类型)、抛出指定异常，优先级依次递减。

### 使用示例
#### 1. 降级到指定方法
```java
@ResponseBody
@RequestMapping("/list")
@RateLimiter(limitForPeriod = 10, limitRefreshPeriod = "60s")
@Fallback(fallbackClass = CustomizeFallback.class, fallbackMethod = "fallback")
public String list() {
    return "Hello, ServiceKeeper!";
}
```

如上，当list()方法降级时会执行CustomizeFallback的fallback()方法。需要注意的是：
- 降级方法必须声明为public，并且为非final，否则cglib无法正常代理；
- 必须将CustomizeFallback注入Spring容器或提供无参构造方法（或者降级方法为类的静态方法）；
- 降级方法和原方法的参数列表及返回值必须相同或者没有参数（允许新增一个导致降级的异常参数，详见下方示例）；
- fallbackClass和fallbackMethod可以省略其中之一，但不能同时省略(**仅适用于`@Fallback`**)；当fallbackClass省略时，默认为原始方法所在类；当fallbackMethod省略时，默认降级方法名与原始方法名称相同；

在使用使用时，允许根据需要在降级方法参数列表中声明导致降级的异常(**该异常必须为ServiceKeeper框架自身抛出的异常**)，如下：
```java
@Component
public class CustomizeFallback {
    /**
     * 并发数超过限制导致的降级
     *
     * @param ex
     * @return
     */
    public String fallback(ConcurrentOverFlowException ex) {
        return "Fallback caused by concurrentOverFlow!";
    }

    /**
     * QPS超过限制导致的降级
     *
     * @param ex
     * @return
     */
    public String fallback(RateLimitOverFlowException ex) {
        return "Fallback caused by rateLimitOverFlow!";
    }

    /**
     * 熔断导致的降级
     *
     * @param ex
     * @return
     */
    public String fallback(CircuitBreakerNotPermittedException ex) {
        return "Fallback caused by circuitBreaker!";
    }

    /**
     * 重试失败导致的降级
     *
     * @param ex
     * @return
     */
    public String fallback(ServiceRetryException ex) {
        return "Fallback caused by retry!";
    }

}
```
如上所示，当熔断、并发数超过限制、QPS超过限制后的降级会执行不同的降级方法，据此可以实现不同异常类型使用不同降级方法。需要注意的是：
```note
如果声明异常，那么该异常必须作为降级方法的第一个参数
```

#### 2. 返回固定值(String类型)
```java
@ResponseBody
@RequestMapping("/list")
@RateLimiter(limitForPeriod = 10, limitRefreshPeriod = "60s")
@Fallback(fallbackValue = "Fallback!")
public String list() {
    return "Hello, ServiceKeeper!";
}
```
如上，当list()方法降级时会直接返回"Fallback!"。**注意，该方式仅适用于返回值为String类型的方法的降级。**

#### 3. 抛出指定异常
```java
@ResponseBody
@RequestMapping("/list")
@RateLimiter(limitForPeriod = 10, limitRefreshPeriod = "60s")
@Fallback(fallbackExceptionClass = IllegalArgumentException.class)
public String list() {
    return "Hello, ServiceKeeper!";
}
```

如上，当list()方法降级时会抛出IllegalArgumentException异常。**注意：必须将指定的降级异常注入Spring容器或提供无参构造方法。**

### 配置文件配置
`@Fallback`注解中的所有参数均支持通过配置文件进行配置，但**暂不支持实时生效**，也就是说如果你需要通过此种方式进行方法的降级配置，必须在程序启动前在配置文件中完成相应的配置。示例如下：
```properties
#降级方法所在类名
io.esastack.servicekeeper.TestController.list.fallbackClass=\
  io.esastack.servicekeeper.keepered.test.controller.fallback.CustomizeFallback

#降级方法名称
io.esastack.servicekeeper.TestController.list.fallbackMethod=fallback

#降级到指定值
io.esastack.servicekeeper.TestController.list.fallbackValue=Hello ServiceKeeper!

#降级到指定异常
io.esastack.servicekeeper.TestController.list.fallbackExceptionClass=java.lang.RuntimeException
```
```note
如前文所述，不同策略的优先级为：降级到指定方法 > 降级到指定值 > 降级到指定异常
```
