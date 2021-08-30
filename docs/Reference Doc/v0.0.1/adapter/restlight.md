---
sort: 3
---
# Restlight接入指南

### 说明

在接入之前，希望你已经了解[Spring AOP误用导致不生效的几种场景](../../tips/README.md)。此种方式在Spring接入的基础上，还默认提供了对**Restlight(非SpringMVC)中**带`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@PatchMapping`、`@DeleteMapping`注解方法的代理功能。总体而言，该种接入方式提供了以下功能：
1. 代理有`@ConcurrentLimiter`、`@RateLimiter`、`@CircuitBreaker`、`@Retryable`、`@EnableServiceKeeper`注解中任意一个的方法(`DefaultServiceKeeperAop`)
2. 默认支持返回值为`CompletableFuture`、`ListenableFuture`类型的异步方法治理
3. 代理有`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@PatchMapping`、`@DeleteMapping`注解中任意一个的方法（`RestlightAutoSupportAop`）

```note
该种接入方式无需重复引入servicekeeper-spring-adapter或者servicekeeper-springboot-adapter依赖包
```

### 接入步骤
Step 1: 引入maven依赖
```xml
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>servicekeeper-restlight-adapter</artifactId>
    <version>${servicekeeper.version}</version>
</dependency>

<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>commons</artifactId>
    <version>${esa.commons.version}</version>
</dependency>
```
```note
默认支持返回值为CompletableFuture和ListenableFuture类型的异步方法治理，使用时无需额外配置。
```
```note
该方式默认开启了拦截所有带`@RequestMapping`、`@GetMapping`、`@PostMapping`、`@PutMapping`、`@PatchMapping`、`@DeleteMapping`注解方法的功能，如果您想关闭该功能，可以在application.properties中做如下配置：
```

```properties
io.esastack.servicekeeper.adapter.aop.restlight.support.enable=false
```
