---
sort: 7
---

# 异步方法治理

### 概述
`Service Keeper`需要在方法真正执行结束后进行并发数减一、返回值（包括异常、执行耗时等）记录等操作。对于同步方法而言，`Service Keeper`会在方法执行结束时自动执行这部分逻辑，无需用户关心；对于异步的返回值，则需要在异步真正执行结束时再回调这部分逻辑。`Service Keeper`内置了对常用的`CompletableFuture`、`ListenableFuture`的异步支持，在异步真正结束时进行回调，完成对异步方法的治理，使用时无需额外配置，详情见下文。

### 使用方式
`Service Keeper`对不同类型的应用异步支持如下：
#### 类型一： Spring&Spring Boot
默认支持返回值为CompletableFuture类型的异步方法治理，使用时无需额外配置。

```note
1. Spring应用指通过 `servicekeeper-spring-adapter`方式接入的应用
2. Spring Boot应用指通过`servicekeeper-springboot-adapter`方式接入的应用
```

#### 类型二： Restlight
默认支持返回值为`CompletableFuture`和`ListenableFuture`类型的异步方法治理，使用时无需额外配置。
```note
Restlight应用指通过 `servicekeeper-restlight-adapter`方式接入的应用
```

### 自定义异步返回值处理
异步方法治理的实现原理如下：获取到原始方法返回结果后判断是否为异步类型，如果不是则立即执行`Service Keeper`的后置处理操作，如：并发数减一、熔断器状态的更新、请求上下文清理等操作；否则等到异步方法真正结束时再执行前述操作。
`AsyncResultHandler`中定义了两个方法：
- boolean supports(Class<?> returnType) : 该类支持的异步结果类型
- T handle(T returnValue, RequestHandle requestHandle)：处理异步执行结果，主要在原始返回值上添加监听器，在异步真正执行结束后回调`Service Keeper`的后置操作。


如果需要支持其他返回值类型的异步方法，需要实现上述`AsyncResultHandler`接口并实现类注入Spring容器。附用于支持返回值类型为`CompletableFuture`的异步方法治理类`CompletableStageHandler`作为实现时的参考。
```java
public class CompletableStageHandler<M> implements AsyncResultHandler<CompletionStage<M>> {

    @Override
    public boolean supports(Class<?> returnType) {
        return CompletionStage.class.isAssignableFrom(returnType);
    }

    @Override
    public CompletionStage<M> handle(CompletionStage<M> returnValue, RequestHandle requestHandle) {
        returnValue.whenComplete((r, t) -> {
            if (t != null) {
                requestHandle.endWithError(t);
            } else {
                requestHandle.endWithResult(r);
            }
        });

        return returnValue;
    }
}
```
