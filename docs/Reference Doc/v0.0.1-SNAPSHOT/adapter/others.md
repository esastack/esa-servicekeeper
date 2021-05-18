---
sort: 4
---

# 方法动态代理
`Service Keeper`提供了原始方法的代理工具类用来完成对原始方法的代理，并织入服务治理的相关功能。使用时需要引入包：

```xml
<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>servicekeeper-proxy-adapter</artifactId>
    <version>${servicekeeper.version}</version>
</dependency>

<dependency>
    <groupId>io.esastack</groupId>
    <artifactId>commons</artifactId>
    <version>${esa.commons.version}</version>
</dependency>
```

```note
使用该方式接入`Service Keeper`时需要在应用启动时主动调用ServiceKeeperAsyncInvoker的init()完成对配置文件的监听，否则只有当第一调用ServiceKeeperAsyncInvoker中的方法时才会建立对配置文件的监听。
```

动态代理默认会拦截对象中所有能被代理到的方法，使用方式根据被代理的类是否为接口而略有不同，具体如下：
- #### 情形一：代理类为接口：
接口定义示例：
```java
public interface DemoService {
    @RateLimiter(limitForPeriod = 100)
    void sayHello();
}
```
实现类示例：
```java
public class DemoServiceImpl implements DemoService {
    @Override
    public void sayHello() {

    }  
}
```

`Service Keeper`治理示例：

```java
// 使用ServiceKeeper提供的代理工厂创建接口的代理类
DemoService demoService = ServiceKeeperProxyFactory.createProxyHasInterface(new DemoServiceImpl());

// 执行方法调用
demoService.sayHello();    
```

```note
该情形下服务治理注解需要加在接口方法上
```

- #### 情形二：代理类为非接口：
原始类定义示例：

```java
public class DemoServiceImpl {
    @Override
    @RateLimiter(limitForPeriod = 100)
    public String merge(String s1, String s2) {
        return s1 + s2;
    }
}
```

`Service Keeper`代理示例：
```java
// 使用ServiceKeeper提供的代理工厂创建非接口的代理类
DemoServiceImpl demoService = ServiceKeeperProxyFactory.createProxyNoInterface(new DemoServiceImpl());

// 执行方法调用
demoService.sayHello();
```
```note
该情形下服务治理注解需要加在类方法上
```

### 工具类
`Service Keeper`提供了工具类，在原始方法执行前后织入服务治理的相关功能，具体方法如下：

```java
public class ServiceKeeperInvoker {

    private ServiceKeeperInvoker() {
    }

    public static Object invoke(Method method, Object delegate, Object[] args) throws Throwable {
        return ServiceKeeperEntryManager.getEntry().invoke(method, delegate, args);
    }

    public static Object invoke(String aliasName, Method method, Object delegate, Object[] args) throws Throwable {
        return ServiceKeeperEntryManager.getEntry().invoke(aliasName, method, delegate, args);
    }

    public static <T> T call(String name, CompositeServiceKeeperConfig immutableConfig,
                             Callable<T> callable, Object[] args) throws Throwable {
        return ServiceKeeperEntryManager.getEntry().call(name, immutableConfig, callable, args);
    }

    public static <T> T call(String name, Callable<T> callable, Object[] args) throws Throwable {
        return ServiceKeeperEntryManager.getEntry().call(name, callable, args);
    }

    public static <T> T call(String name, Supplier<CompositeServiceKeeperConfig> immutableConfigSupplier,
                             Supplier<OriginalInvocationInfo> originalInvocationInfoSupplier, Callable<T> callable,
                             Object[] args) throws Throwable {
        return ServiceKeeperEntryManager.getEntry().call(name, immutableConfigSupplier, originalInvocationInfoSupplier,
                callable, args);
    }

    public static void run(String name, Runnable runnable, Object[] args) throws Throwable {
        ServiceKeeperEntryManager.getEntry().run(name, runnable, args);
    }

    public static void run(String name, CompositeServiceKeeperConfig immutableConfig,
                           Runnable runnable, Object[] args) throws Throwable {
        ServiceKeeperEntryManager.getEntry().run(name, immutableConfig, runnable, args);
    }

    /**
     * Invoke this method only when you want to customize async result handlers. It' not necessary for you
     * to invoke this method manually. If you don't do this before you firstly tryAsyncInvoke(), the default
     * async result handlers will be empty.
     *
     * @param asyncResultHandlers asyncResultHandlers
     * @see AsyncResultHandler
     */
    public static void init(List<AsyncResultHandler> asyncResultHandlers) {
        ServiceKeeperEntryManager.initCompositeEntry(asyncResultHandlers);
    }
}
```

其中，`name`用于指定当前调用的名称，与配置文件中的配置的名称或管控平台的资源ID对应
`Callbale`、`Runnable`用于封装原始的执行逻辑。
`OriginalInvocationInfo`提供原始方法的返回值、参数列表信息，在匹配降级方法时使用（可选）。
`CompositeServiceKeeperConfig`提供当调用的初始配置（可选）。
举例说明：
自定义`HelloService`如下：
```java
public class HelloService {

    public String sayHello(String name) {
        return "Hello " + name + "!";
    }

}
```
使用`ServiceKeeper`封装逻辑如下：
```java
public static void main(String[] args) throws Throwable {

    final HelloService service = new HelloService();
    final Callable<String> callable = () -> service.sayHello("LiMing");

    ServiceKeeperInvoker.call("helloservice.hello", callable, new Object[]{"LiMing"});

}
```
如上所示，"helloservice.hello"表示当前callbable的名称，通过配置文件或者管控平台配置该名称的限流、熔断等配置将对当前callbale生效；new Object[]{"LiMing"}是用来对当前callbable做参数级服务治理，如果不需该功能可以直接使传递new Object[0]。
