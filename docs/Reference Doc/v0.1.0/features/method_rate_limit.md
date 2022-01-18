---
sort: 1
---

# 方法级QPS限制

### 概述
通常来说系统能正确处理的请求数量是有限的，大规模流量的突增往往会超出系统的能力范围，压垮整个系统。`Service Keeper`提供了方法级的限流功能，对超过QPS阈值的调用直接降级或抛出`RateLimitOverflowException`异常。使用时，可以自定义限流周期以及周期内流量阈值。
```note
1. QPS阈值支持通过本地配置文件动态更新，实时生效。
2. 对于QPS超过限制的方法调用，如果原始方法正确配置了降级方法则执行该方法，否则抛出`RateLimitOverFlowException`。
```

### 使用示例
```java
@RequestMapping("/list")
@ResponseBody
@RateLimiter(limitForPeriod = 500, limitRefreshPeriod = "10m")
public Employee list() {
    return new Employee("LiMing", 25, "1403063");
}
```

如上配置表示该方法每10分钟内最多允许500次调用，对于超过该限制的方法调用直接抛出`RateLimitOverFlowException`。

对该注解的属性说明如下：

| 属性名称         |      类型    |             描述          |       默认值    |      备注   |    是否支持动态配置                                                   
| --------------- |   :--------  | :----------------------- | -------------- |  ----------   |   ----------
|  limitForPeriod|  int                      |  周期内流量阈值  |       无    |     |    **是**
|  limitRefreshPeriod|   String    |    周期时间     |       1s               |        |  否

```note
如果配置文件中存在该方法的`@RateLimiter`注解中的同名配置则配置文件中的优先级更高。
```
### 注解配置简化
```java
@RequestMapping("/list")
@ResponseBody
@RateLimiter(500) //表示 limitForPeriod 为 500，limitRefreshPeriod为默认值
public Employee list() {
    return new Employee("LiMing", 25, "1403063");
}
```

### 配置文件配置
如前文所述，只要原始方法可以被`ServiceKeeper`拦截到，你可以不使用`@RateLimiter`注解，直接在配置文件中配限流规则即可完成对原始方法的QPS限制。示例如下：
```properties
#周期内QPS阈值为500
io.esastack.servicekeeper.demo.QpsLimitDemo.demoMethod.limitForPeriod=500

#限流周期每10分钟更新一次，单位为：ms(毫秒)、s(秒)、m(分钟)等
io.esastack.servicekeeper.demo.QpsLimitDemo.demoMethod.limitRefreshPeriod=10m
```

其中，`io.esastack.servicekeeper.demo.QpsLimitDemo.demoMethod`为原始方法的名称（类全限定名+方法名）。
```note
通过配置文件配置限流规则时周期内流量阈值 `limitForPeriod`为必需配置，限流周期`limitRefreshPeriod`如不配置默认为1s。
```

#### 动态更新参数限制
并不是所有的参数都支持动态更新，其中支持动态更新的参数项：[动态参数配置项](../configurations/dynamic.md)
