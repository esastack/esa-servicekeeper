---
sort: 200
---

# 方法级并发数限制
### 概述
`Service Keeper`提供了方法级的隔离和并发数限制，底层基于信号量实现。使用时只需通过`@ConcurrentLimiter`注解的threshold属性设置并发数阈值即可。业界通常采用基于线程池的隔离术来实现资源间的隔离，这种方式效果更好，但也存在明显的不足：
- 线程池数量太多，不便于管理
- 线程之间切换频繁，增加了系统负荷

`Service Keeper`通过限制方法的并发调用数，使其占用的系统资源保持在可控的范围类（通常指线程资源），避免出现因某个方法耗尽系统资源导致整个系统不可用的“悲剧”。`Service Keeper`实现了与基于线程池隔离相似的效果，但占用的系统资源更少，使用方式更加灵活。
```note
1. 并发数阈值(threshold参数)支持通过本地配置文件动态配置，实时生效。
2. 对并发数超过限制的方法调用，如果原始方法正确配置了降级方法则执行该方法，否则直接抛出`ConcurrentOverFlowException`。
```

### 使用示例
在方法上使用`@ConcurrentLimiter`注解来完成方法的并发数限制，该注解只有一个属性threshold，表示并发数阈值，要求该值必须大于0。示例如下：
```java
@RequestMapping("/list")
@ResponseBody
@ConcurrentLimiter(threshold = 100)
public Employee list() {
    return new Employee();
}
```

该配置表示同一时刻最多只允许100个线程执行该方法。
```note
如果配置文件中配置了该方法的最大并发数，则配置文件中的优先级更高。
```

### 配置文件配置
如前文所述，只要原始方法可以被`ServiceKeeper`拦截到，可以不使用`@ConcurrentLimiter`注解，直接在配置文件中配置并发数阈值即可完成对该方法的隔离和并发数限制。示例如下：

```properties
#并发数限制为10
esa.servicekeeper.demo.ConcurrentLimitDemo.demoMethod.maxConcurrentLimit=10
```

其中，`esa.servicekeeper.demo.ConcurrentLimitDemo.demoMethod`原始方法的名称（类全限定名+方法名）。
```note
通过配置文件配置并发数限制时 `maxConcurrentLimit`为必需配置。
```
