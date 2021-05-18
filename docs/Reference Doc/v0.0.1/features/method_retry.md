---
sort: 6
---

# 方法重试

### 概述
在某些场景下，用户希望在执行某个方法失败后可以自动重试1次或多次，当然这也涉及到方法内部的实现是否可以在多次重试后成功。`Service Keeper`提供了方法级的重试，使用时只需在方法上添加`@Retryable`，通过配置maxAttempts、includeExceptions、excludeExceptions等属性来控制最大执行次数(默认3次，包括第1次)、可重试异常，不可重试异常，同时提供了Backoff补偿策略，控制重试的时间间隔，**使用Thread.sleep(ms)来实现时间间隔(默认delay=0即重试前不sleep)。**
重试时，可能出现一些异常情况：
- 持续异常，执行次数耗尽
- 抛出不需重试的异常，此时`ServiceKeeper`将抛出`ServiceRetryException`异常

上述两种情况下，用户可以通过配置`@Fallback`进行重试失败后的降级操作，详情见[方法降级](method_fallback.md)

**重要说明**
- `Service Keeper`不支持异步方法重试
- 方法重试适用于业务执行时发生的偶发异常（且重试时可能执行正常）或幂等的操作，这些场景有一个共同的特点：多次执行不影响正确性，不产生副作用，如HTTP GET方法。
- 重试功能不适用于有数据库事务、MQ生产消息等操作（可能出现一份内容生成多个message）的方法，如果方法中包含此类业务，需将对应的异常加入到excludeExceptions中，遇到此类异常时禁止重试。

### 使用示例
```java
private int count = 0;

@Retryable(maxAttempts = 5, includeExceptions = {Exception.class},
        excludeExceptions = {RuntimeException.class},
        backoff = @Backoff(delay = 1000, maxDelay = 5000, multiplier = 1.5))
public int service(int arg) throws Exception {
    if (count++ < 4) {
        throw new IllegalAccessException("Planned");
    }
return count;	
}
```

该方法重试时拦截到Excpetion将会重试,但当异常为RuntimeException时不会重试，直接抛出异常，IllegalAccessException不是RuntimeException的子类，它是满足重试条件的，执行时第一次重试会先等待1000ms，第二次重试会重试1000 * 1.5 = 1500ms，以此类推，如果重试等待时间大于5000ms，则只会等待5000ms，如果5次都执行失败，则会抛出ServiceRetryException，提示执行5次后仍然调用失败

重试前会判断异常是否为RuntimeException，如果是将会终止重试，由于抛出的异常也可能是RuntimeException的子类，为了匹配类似的情况，异常匹配有以下特点：
- 默认情况拦截Exception，不拦截Error
- 支持继承关系匹配
- 如果异常的继承关系匹配无法命中某个异常，将尝试通过cause来匹配

使用时，可以自定义重试次数、可重试异常、不可重试异常以及重试时间间隔。对该注解的属性说明如下：

| 属性名称         |      类型    |             描述          |       默认值    |      备注   |    是否支持动态配置                                                   
| --------------- |   :--------  | :----------------------- | -------------- |  ----------   |   ----------
|  maxAttempts|  int               |  最大尝试执行次数  |       3    | 包括第一次执行    |    **是**
|  includeExceptions|   Class<? extends Throwable>[]    |    可重试异常     |      Exception.class               |    默认情况Error不重试    |  是
|  excludeExceptions|   Class<? extends Throwable>[]    |    不可重试异常     |      无             |    优先级高于includeExceptions   |  是
|  backoff.delay|      long     |  重试补偿策略，重试间隔时间初始值 |     0 ms         |  0表示不补偿，立即重试        |     是
|  backoff.maxDelay|      long     |  重试补偿策略，重试间隔时间最大值 |     0        |  delay>maxDelay时忽略maxDelay，使用默认值30s        |     是
|  backoff.multiplier|      double|  重试补偿策略，重试时间累积因子 |     1.0|          |     是

当backoff.delay >0 时，方法重试的间隔睡眠时间计算公式：
```properties
n:执行次数
  n = 1 : sleeptime = 0 
  n > 1 : sleeptime = delay * multiplier^(n-2)   
  sleeptime > maxDelay 时，sleepttime = maxDelay
```

### 配置文件配置
如前文所述，只要原始方法可以被`ServiceKeeper`拦截到，你可以不使用`@Retryable`注解，直接在配置文件中配限流规则即可完成对原始方法的QPS限制。示例如下：
```properties
#方法重试
com.example.service.DemoClass.demoMethod.maxAttempts=2
#多个异常用英文逗号分隔
com.example.service.DemoClass.demoMethod.includeExceptions=[java.lang.Exception]
#多个异常用英文逗号分隔
com.example.service.DemoClass.demoMethod.excludeExceptions=[java.lang.RuntimeException]
com.example.service.DemoClass.demoMethod.delay=100
com.example.service.DemoClass.demoMethod.maxDelay=3000
com.example.service.DemoClass.demoMethod.multiplier=1.1
```
```note
1. 如果想拦截Error,需要显示设置includeExceptions = {Error.class或Throwable.class}。一般情况下，出现Error再重试没有太大的意义，非特殊场景不推荐拦截Error
2. 如果在同一个类中包含了多个Retryable方法，同时包含一个或多个Recover方法，请确保每个Recover方法返回值类型只会和其中一个Retryable方法的返回类型相同，否则可能会出现Recover匹配错误,执行时将抛出异常
```
