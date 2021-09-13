---
sort: 1
---

# 动态参数配置
`Service Keeper`提供了动态更改配置参数的功能。在应用运行期间，通过更新本地配置文件来实现配置规则的实时更改，目前支持修改的动态参数列表如下：

| 参数名称         |      类型    |             描述          |          默认值  |                                             
| --------------- |   :--------  | :----------------------- |  :-----------------------
|  maxConcurrentLimit  |  int     |        并发数阈值         |     Integer.MAX_VALUE  |
|  limitForPeriod      |  int     |        QPS阈值	    |      Integer.MAX_VALUE  |
|  maxSpendTimeMs      |  long |        方法最大耗时（ms）    |      -1                 |
|  failureRateThreshold|  float  |      熔断率阈值           |       50.0f             |
|  forcedOpen          |  boolean  |      强制打开熔断器      |      false              |
|  forcedDisabled      |  boolean  |      强制禁用熔断器      |      false              |
|  maxAttempts         |  int|      方法重试最大执行次数(包括第一次)      |       3       |
|  includeExceptions   |  string  |      方法重试可重试异常      |      "java.lang.Exception"   |
|  excludeExceptions   |  string  |      方法重试不可重试异常      |    ""                       |
|  delay               |  long  |        方法重试间隔时间初始值(ms)      |   0L                    |
|  maxDelay            |  long  |     方法重试间隔时间最大值(ms)       |     0L                   |
|  multiplier            |  double  |     方法重试间隔时间累积因子       |   1.0d                 
|  maxConcurrentLimitValueSize      |  int  |     并发数限制的最大参数值个数       |   100       
|  maxRateLimitValueSize            |  int  |     QPS限制的最大参数值个数       |   100       
|  maxCircuitBreakerValueSize       |  int  |     允许进行熔断的最大参数值个数       |   100        

具体示例如下：
```
#方法级动态配置规则：方法别名.配置名称=配置值
com.example.service.DemoClass.demoMethod.maxConcurrentLimit=20
com.example.service.DemoClass.demoMethod.failureRateThreshold=55.5
com.example.service.DemoClass.demoMethod.forcedOpen=false
com.example.service.DemoClass.demoMethod.limitForPeriod=600

#方法重试
com.example.service.DemoClass.demoMethod.maxAttempts=2
com.example.service.DemoClass.demoMethod.includeExceptions=[java.lang.Exception] #英文逗号分隔多个异常
com.example.service.DemoClass.demoMethod.excludeExceptions=[java.lang.RuntimeException]#英文逗号分隔多个异常
com.example.service.DemoClass.demoMethod.delay=100
com.example.service.DemoClass.demoMethod.maxDelay=3000
com.example.service.DemoClass.demoMethod.multiplier=1.1

#参数级动态配置规则：方法别名.参数别名.配置名称=配置值列表
com.example.service.DemoClass.demoMethod.arg0.limitForPeriod={LiSi:20,ZhangSan:50}
```
使用时，通过`io.esastack.servicekeeper.core.annotation.Alias`注解设置方法和参数的别名，如果不设置或设置的值为空，则使用默认值。方法别名默值为方法的全限定名，参数别名默认值为：arg0，arg1，arg2...，上述配置文件为方法和参数均为未配置别名时的配置格式。
使用`@Alias`自定义方法和参数别名的示例如下：
```java
@ConcurrentLimiter(threshold = 500)
@Alias("employeeList")
public Employee list(@Alias("name") @ArgsConcurrentLimiter String name) {
    return new Employee();
}
```
当配置了方法和参数别名时，对应的配置格式如下：
```properties
employeeList.maxConcurrentLimit=20
employeeList.name.maxConcurrentLimit={LiSi:20,ZhangSan:50}
```
