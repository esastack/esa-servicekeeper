---
sort: 4000
---

# 开发注意事项

### 1、重载方法使用`@Alias`设置别名
虽然`Service Keeper`不强制要求对每个方法和参数设置别名（默认方法名为：方法的全限定名，参数名为：arg0,arg1,arg2...），但为了配置时方便，建议对每个方法和参数设置有意义的别名。**特别地，对于类中的同名方法，如果没有针对每个方法设置别名，这些同名方法会使用同一个限流器、熔断器等。**

### 2、是否开启参数级服务治理
如果确认不使用参数级服务治理功能，建议使用全局开关直接关闭，减少不必要的性能损耗，具体方式可参考**全局配置**功能；

### 3、Spring AOP（CGLIB）代理注意事项
对于Spring应用，`Service Keeper`使用CGLIB完成对原始方法的代理，织入服务治理的功能，使用时请确保了解目标方法无法被Spring AOP正确代理的情形，常见如下：
1. 同一个类中方法的相互调用

```java
@RequestMapping("/index")
public void index() {
    this.list();
}

@ConcurrentLimiter(threshold = 100)
@RateLimiter(limitForPeriod = 20)
public void list() {

}
```

如上，在list()方法上使用的并发数限制注解和流量限制注解不会生效，可以将注解迁移到index()方法上或者在将list()方法迁移到新的类中。

2. 使用final修饰的方法

```java
@RateLimiter(limitForPeriod = 10)
public final void list() {

}
```

由于final方法不允许子类重写，因此不能代理类，故对该方法的流量限制无效。

3. 使用static修饰的方法

```java
@RateLimiter(limitForPeriod = 10)
public static void list() {

}
```

使用static修饰的方法同样无法被子类重写，因此不能生成代理方法，故此种方式下流量限制不生效。

4. 使用private修饰的方法

```java
@RateLimiter(limitForPeriod = 10)
private void list() {

}
```

使用private修饰的方法同样无法被子类重写，生成的代理不包含实现类的属性，故这种方式也不可行。

{% include list.liquid all=true %}
