---
sort: 400
---

# JAX-RS接入适配
对于JAX-RS应用，如果需要默认拦截带有@Path注解的接口，可以直接引入依赖：
```xml
<dependency>
      <groupId>io.esastack</groupId>
      <artifactId>servicekeeper-jaxrs-adapter</artifactId>
      <version>${project.version}</version>
</dependency>
```

并将`JaxRsAutoSupportAop`注入Spring容器中：
```java
@Bean
public JaxRsAutoSupportAop jaxRsAutoSupportAop() {
    return new JaxRsAutoSupportAop();
}
```
