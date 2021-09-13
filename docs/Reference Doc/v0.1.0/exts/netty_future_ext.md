---
sort: 2
---

# Netty Future异步支持
`Service Keeper`内置了用于处理返回值为`io.netty.util.concurrent.Future`的异步处理器`NettyFutureHandler`,使用时首先需要引入依赖：
```xml
<dependency>
     <groupId>io.esastack</groupId>
     <artifactId>servicekeeper-ext-nettyfuture</artifactId>
     <version>${servicekeeper.version}</version>
</dependency>
```
如果是Spring环境则直接将该Handler注入Bean容器,如：
```java
@Bean
public NettyFutureHandler nettyFutureHandler() {
    return new NettyFutureHandler();
}
```

```note
由于`Restlight`默认支持`io.netty.util.concurrent.Future`，因此`servicekeeper-restlight-adapter`模块中默认注入了`NettyFutureHandler`。
```

对于非Spring容器，可在初始化时传入该Handler的实例，如：
```java
ServiceKeeperInvoker.init(Collections.singletonList(new NettyFutureHandler<>()));
```
