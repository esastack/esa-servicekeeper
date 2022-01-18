---
sort: 10
---

# 参数级熔断

### 概述
对于某些业务场景来说，方法级别的熔断可能粒度太粗，需要更精确的熔断。比如以根据方法的参数作为第三方调用的标识，如果仅是一个调用方服务出现问题就熔断整个方法显然过于粗暴，误伤了其他原本可以正常服务的其他第三方调用。此时可以使用`Service Keeper`提供的参数级熔断功能。将参数级熔断的注解`@ArgsCircuitBreaker`标注在方法的参数上即可完成对该方法的参数级熔断控制。使用时，可以直接在注解中配置参数值对应的阈值，也可以在配置文件中配置。需要注意的是：

- 当前版本仅支持对String类型参数进行治理

- 参数级的熔断阈值支持通过本地配置文件动态配置，实时生效。

- 对于参数熔断的方法调用，如果其所在方法正确配置了降级方法则执行降级方法，否则直接抛出`CircuitBreakerNotPermittedException`。

### 使用示例
```java
@EnableServiceKeeper
public Employee list(@ArgsCircuitBreaker(failureRateThresholdMap = "{LiMing: 20.0f, WangWu: 30.0f}") String name) {
    return new Employee();
}
```

如上配置表示，限制name值为LiMing的方法熔断阈值为20.0%，name值为WangWu的方法熔断阈值为30.0%；此外，还可以动态新增、更新参数值及对应的熔断阈值。
```note
1. `@EnableServiceKeeper`用于标识该方法可以被`ServiceKeeper`拦截，如果方法上已存在任一方法级服务治理注解（`@RateLimiter`、`@ConcurrentLimiter`、`@CircuitBreaker`、`@Retry`）则可省略该注解。
2. 如果配置文件中存在同名参数的并发数限制，则配置文件中的配置优先级更高。
```

### 全量参数值的治理
在使用参数级服务治理的过程中，如果逐一配置每个参数值及其熔断阈值的话难度及工作量较大，首先使用者要清楚的知晓具体的参数值并且逐一配置阈值，通常情况下大部分参数值对应的熔断阈值都是相同的，这时就会出现大量重复的配置，增加了配置时的难度和工作量。为此，`Service Keeper`提供了全量参数值熔断的功能，通过通配符"\*"配置任一参数值对应的熔断阈值，如failureRateThresholdMap ={"LiMing": 80.0, "\*": 90.0}表示参数值为LiMing的方法熔断阈值为80.0%，除此之外的每个参数值的熔断阈值为90.0%。**需要注意的是，`Service Keeper`默认每个参数进行熔断的参数值个数为100，对超过最大限制的参数值将不再进行熔断控制**。可以通过`@ArgsCircuitBreaker`的maxValueSize属性来更新该值。

### 配置文件配置
如前文所述，只要原始方法可以被`Service Keeper`拦截到，可以不使用`@ArgsCircuitBreaker`注解，直接在配置文件中配置参数级并发数限制。示例：

#### 1. 更新参数值的限制
在配置文件中加入配置：
```properties
com.example.service.Employee.list.arg0.failureRateThreshold={LiMing:80.0f,WangWu:90.0f}

#使用通配符匹配所有参数示例
com.example.service.Employee.list.arg0.failureRateThreshold={*:80.0f}
com.example.service.Employee.list.arg0.maxSpendTimeMs={*: 50}
```
其中，com.example.service.Employee.list为方法的全限定名（类名+方法名），arg0为方法第一个参数的名称。

#### 2. 动态新增参数值的周期内流量阈值
在配置文件中加入配置：
```properties
#限制list方法arg0参数值为ZhangSan的熔断阈值为30.0%
com.example.service.Employee.list.arg0.failureRateThreshold={ZhangSan:30.0f}
```

#### 3. 动态更新参数限制
并不是所有的参数都支持动态更新，其中支持动态更新的参数项：[动态参数配置项](../configurations/dynamic.md)
```properties
#限制被治理的参数值个数为50
com.example.service.Employee.list.arg0.maxCircuitBreakerValueSize=50
```
