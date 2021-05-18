---
sort: 100
---

# ListenableFuture异步支持
`Service Keeper`内置了用于处理返回值为`ListenableFuture`的异步处理器`ListenableFutureHandler`,使用时首先需要引入依赖：
```xml
<dependency>
     <groupId>io.esastack</groupId>
     <artifactId>servicekeeper-ext-listenablefuture</artifactId>
     <version>${servicekeeper.version}</version>
</dependency>
```

如果是Spring环境则直接将该Handler注入Bean容器,如：
```java
@Bean
public listenableFutureHandler listenableFutureHandler() {
    return new ListenableFutureHandler();
}
```

```note
由于Restlight默认支持ListenableFuture，因此servicekeeper-restlight-adapter模块中默认注入了ListenableFutureHandler
```

对于非Spring容器，可在初始化时传入该Handler的实例，如：
```java
ServiceKeeperInvoker.init(Collections.singletonList(new ListenableFutureHandler<>()));
```
