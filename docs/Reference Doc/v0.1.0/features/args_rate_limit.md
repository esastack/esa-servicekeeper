---
sort: 8
---

# 参数级QPS限制

### 概述
对于某些业务场景来说，方法级别的QPS限制可能粒度太粗，需要更精确的流量控制，此时可以使用`Service Keeper`提供的参数级限流功能。将参数级QPS限制的注解`@ArgsRateLimiter`标注在方法的参数上即可完成对指定参数的QPS限制。使用时，可以直接在注解中配置参数值对应的阈值，也可以在配置文件中配置。需要注意的是：

- 当前版本仅支持对String类型参数进行治理

- 参数级的QPS阈值支持通过本地配置文件动态配置，实时生效。

- 对于参数QPS超过限制的方法调用，如果其所在方法正确配置了降级方法则执行降级方法，否则直接抛出`RateLimitOverflowException`。

### 使用示例
```java
@EnableServiceKeeper
public Employee list(@ArgsRateLimiter(limitRefreshPeriod = "10s", limitForPeriodMap = "{LiMing: 1, WangWu: 2}") String name) {
    return new Employee();
}
```

如上配置表示，以10s为一个限流周期，该周期内限制name值为LiMing的方法调用1次，name值为WangWu的方法调用2次。此外，还可以动态新增、更新参数值及对应的周期内流量阈值。
```note
1. `@EnableServiceKeeper`用于标识该方法可以被ServiceKeeper拦截，如果方法上已存在任一方法级服务治理注解（`@RateLimiter`、`@ConcurrentLimiter`、`@CircuitBreaker`、`@Retry`）则可省略该注解。
2. 如果配置文件中存在同名参数的QPS限制，则配置文件中的配置优先级更高。
```

### 全量参数值的治理
在使用参数级服务治理的过程中，如果逐一配置每个参数值及其QPS阈值的话难度及工作量较大，首先使用者要清楚的知晓具体的参数值并且逐一配置阈值，通常情况下大部分参数值对应的QPS阈值都是相同的，这时就会出现大量重复的配置，增加了配置时的难度和工作量。为此，`Service Keeper`提供了全量参数值限流的功能，通过通配符"\*"配置任一参数值对应的QPS阈值，如limitForPeriodMap={"LiMing": 10, "\*": 20}表示参数值为LiMing的方法周期内调用数为10，除此之外的每个参数值周期内最多允许调用20次。**需要注意的是，`Service Keeper`默认每个参数进行QPS限制的参数值个数为100，对超过最大限制的参数值将不再进行QPS限制**。可以通过`@ArgsRateLimiter`的maxValueSize属性来更新该值。

### 配置文件配置
如前文所述，只要原始方法可以被`Service Keeper`拦截到，可以不使用`@ArgsRateLimiter`注解，直接在配置文件中配置参数级QPS限制。示例：

#### 1. 更新参数值的周期内流量阈值
在配置文件中加入配置：
```properties
com.example.service.Employee.list.arg0.limitForPeriod={LiMing:10,WangWu:20}

#使用通配符匹配所有参数示例
com.example.service.Employee.list.arg0.limitForPeriod={*:30}

```

其中，com.example.service.Employee.list为方法的全限定名（类名+方法名），arg0为方法第一个参数的名称。

#### 2. 动态新增参数值的周期内流量阈值
在配置文件中加入配置：
```properties
#限制list方法arg0参数值为ZhangSan的周期内阈值为30
com.example.service.Employee.list.arg0.limitForPeriod={ZhangSan:30}

#使用通配符匹配所有参数示例
com.example.service.Employee.list.arg0.limitForPeriod={*:30}

#限流周期每10秒更新一次，单位为：ms(毫秒)、s(秒)、m(分钟)等
com.example.service.Employee.list.arg0.limitRefreshPeriod={*:10s}
```

#### 3. 动态更新被治理的参数值个数限制
```properties
#限制被治理的参数值个数为50
com.example.service.Employee.list.arg0.maxRateLimitValueSize=50
```
