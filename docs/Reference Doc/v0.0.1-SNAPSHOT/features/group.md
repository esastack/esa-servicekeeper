---
sort: 1100
---

# 接口分组

### 概述
在特定场景下，多个方法的限流、熔断阈值等配置可能是相同的，即使动态更新也希望这一批接口的配置同时生效。此时可以使用接口分组的功能，在分配好接口所属的分组后，组级别的配置可应用组内所有的方法，做到批量配置同时生效，同时也简化了用户配置。

### 使用示例
```java
@Group("demoGroupA")
public Employee list(String name) {
    return new Employee();
}
```
如上配置表示，设置list方法所属分组为demoGroupA，后续对demoGroupA的所有配置均在该方法上生效
```note
1. 通过配置文件也可指定接口所属的分组，当一个接口同时通过注解和配置文件指定了分组时，配置文件中指定的分组优先级更高
2. 如果接口既有分组级别的配置，又有接口自身级别的配置，此时接口级别的同名配置优先级更高
3. 分组配置不支持参数级别的配置
```

### 配置文件配置
在配置文件中加入配置：

```properties
#demoGroupA中每个接口的最大并发调用数为10
group.demoGroupA.maxConcurrentLimit=10

#demoGroupA中每个接口的周期内请求数为100
group.demoGroupA.limitForPeriod=100

#demoGroupA中每个接口的限流周期为10s
group.demoGroupA.limitRefreshPeriod=10s

#demoGroupA中每个接口的熔断阈值为90%
group.demoGroupA.failureRateThreshold=90.0f

#demoGroupA中包含interface1,interface2,interface3这三个接口
group.demoGroupA.items=[interface1, interface2, interface3]
```
其中，**group为组级别配置的固定前缀**，demoGroupA为组名。此处仅列出部分组级别的配置，实时上所有方法级别的配置（熔断、降级、并发数限制、QPS限制、重试等）均可用于组级别，更多配置请参考方法级别服务治理的配置。
