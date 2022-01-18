---
sort: 9
---

# 参数级并发数限制

### 概述
对于某些业务场景来说，方法级别的隔离和并发数限制可能粒度太粗，如果需要更精确的隔离和并发数限制，此时可以使用`Service Keeper`提供的参数级并发数限制功能。将参数级并发数限制的注解`@ArgsConcurrentLimiter`标注在方法的参数上即可完成对该方法的并发数限制。使用时，可以直接在注解中配置参数值对应的阈值，也可以在配置文件中配置。需要注意的是：

- 当前版本仅支持对String类型参数进行治理

- 参数级的并发数阈值支持通过本地配置文件动态配置，实时生效。

- 对于参数并发数超过限制的方法调用，如果其所在方法正确配置了降级方法则执行降级方法，否则直接抛出`ConcurrentOverflowException`。

### 使用示例
```java
@EnableServiceKeeper
public Employee list(@ArgsConcurrentLimiter(thresholdMap = "{LiMing: 2, WangWu: 3}") String name) {
    return new Employee();
}
```
如上配置表示，限制name值为LiMing的方法最大并发数为2，name值为WangWu的方法最大并发数为3。此外，还可以动态新增、更新参数值及对应的最大并发数。
```note
1. `@EnableServiceKeeper`用于标识该方法可以被ServiceKeeper拦截，如果方法上已存在任一方法级服务治理注解（`@RateLimiter、@ConcurrentLimiter`、`@CircuitBreaker`、`@Retry`）则可省略该注解。
2. 如果配置文件中存在同名参数的并发数限制，则配置文件中的配置优先级更高。
```

### 全量参数值的治理
在使用参数级服务治理的过程中，如果逐一配置每个参数值及其并发数阈值的话难度及工作量较大，首先使用者要清楚的知晓具体的参数值并且逐一配置阈值，通常情况下大部分参数值对应的并发数阈值都是相同的，这时就会出现大量重复的配置，增加了配置时的难度和工作量。为此，`Service Keeper`提供了全量参数值并发数限制的功能，通过通配符"\*"配置任一参数值对应的QPS阈值，如thresholdMap ={"LiMing": 10, "\*": 20}表示参数值为LiMing的方法并发调用数为10，除此之外的每个参数值的最大并发调用数为20。**需要注意的是，`Service Keeper`默认每个参数的并发数控制的参数值个数为100，对超过最大限制的参数值将不再进行并发数限制**。可以通过`@ArgsConcurrentLimiter`的maxValueSize属性来更新该值。

### 配置文件配置
如前文所述，只要原始方法可以被`Service Keeper`拦截到，可以不使用`@ArgsConcurrentLimiter`注解，直接在配置文件中配置参数级并发数限制。示例：

#### 1. 更新参数值的并发数限制
在配置文件中加入配置：

```properties
com.example.service.Employee.list.arg0.maxConcurrentLimit={LiMing:10,WangWu:20}

#使用通配符匹配所有参数示例
com.example.service.Employee.list.arg0.maxConcurrentLimit={*:30,WangWu:20}
```
其中，`com.example.service.Employee.list`为方法的全限定名（类名+方法名），arg0为方法第一个参数的名称。

#### 2. 动态新增参数值的周期内流量阈值
在配置文件中加入配置：

```properties
#限制list方法arg0参数值为ZhangSan的最大并发数为30
com.example.service.Employee.list.arg0.maxConcurrentLimit={ZhangSan:30}
```

#### 3. 动态更新参数限制
并不是所有的参数都支持动态更新，其中支持动态更新的参数项：[动态参数配置项](../configurations/dynamic.md)
```properties
#限制被治理的参数值个数为50
com.example.service.Employee.list.arg0.maxConcurrentLimitValueSize=50
```
