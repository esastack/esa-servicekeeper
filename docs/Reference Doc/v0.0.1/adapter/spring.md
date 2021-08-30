---
sort: 1
---

# Spring应用接入指南

### 说明
在接入之前，希望你已经了解[Spring AOP误用导致不生效的几种场景](../../tips/README.md)。此种接入方式提供了最基本的Spring AOP代理及`CompletableFuture`异步支持的功能，具体如下：
1. 代理有`@ConcurrentLimiter`、`@RateLimiter`、`@CircuitBreaker`、`@Retryable`、`@EnableServiceKeeper`注解中任意一个的方法。
2. 默认支持返回值为`CompletableFuture`类型的异步方法治理。

### 接入步骤

#### Step 1: 引入maven依赖
```xml
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>servicekeeper-spring-adapter</artifactId>
    <version>${servicekeeper.version}</version>
</dependency>

<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>commons</artifactId>
    <version>${esa.commons.version}</version>
</dependency>
```

```note
默认支持返回值为`CompletableFuture`类型的异步方法治理，使用时无需额外配置。
```

#### Step 2: 初始化服务治理核心Bean
方式一：

```java
@Configuration
@Import(ServiceKeeperConfigurator.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ServiceKeeperConfiguration {

    @Bean
    public WebAutoSupportAop webAutoSupportAop() {
        return new WebAutoSupportAop();
    }
    
}
```

方式二：
在xml文件中配置如下内容：

```xml
<context:component-scan base-package="io.esastack.servicekeeper.adapter.spring"/>

// Spring Web项目可以根据需要添加WebAutoSupportAop
<bean class="WebAutoSupportAop"/>
```

```note
注意事项：`WebAutoSupportAop`为`Service Keeper`扩展的拦截所有带`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@PatchMapping`、`@DeleteMapping`注解方法的拦截器，如果您不想使用该功能，可以选择不注入该类。
```
