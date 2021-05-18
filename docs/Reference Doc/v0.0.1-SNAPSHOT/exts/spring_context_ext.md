---
sort: 300
---

# Spring Context支持

对于Spring、Spring Boot、Restlight类型的接入方式，`ServiceKeeper`首先尝试从Spring Context中获取降级类、降级异常的实例，如果获取不到再使用反射实例化。对于其他的接入方式，如：Dubbo Extension、ESA RPC和直接通过工具类来接入的方式，则默认直接使用反射去构造配置和降级类的的实例，如果需要从Spring Context中获取配置类的Bean，则需要手动配置，具体步骤如下：
#### Step 1:引入依赖
```xml
<dependency>
      <groupId>io.esastack</groupId>
      <artifactId>servicekeeper-ext-factory</artifactId>
      <version>${project.version}</version>
</dependency>
```

#### Step 2:注入SpringContextUtils
```java
@Bean
public SpringContextUtils springContextUtils() {
    return new SpringContextUtils();
}
```

```note
Spring、Spring-Boot、Restlight等接入适配默认注入了该Bean，使用时无需重复注入；仅Dubbo Extension、ESA RPC和直接使用工具类接入的方式需要注意该问题。
```
