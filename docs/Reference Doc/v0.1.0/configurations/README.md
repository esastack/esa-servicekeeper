---
sort: 3000
---

# 参数配置
{% include list.liquid all=true %}

## 说明
### maven 依赖
```xml
<dependency>
    <groupId>esa</groupId>
    <artifactId>servicekeeper-configsource-file</artifactId>
    <version>${servicekeeper.version}</version>
</dependency>
```
### 配置文件目录和名称

`Service Keeper`支持在配置文件中配置所有服务治理的参数，包括：静态参数和动态参数。配置文件名称为：`service-keeper.properties`。获取该文件路径的优先级如下：

**1. System.getProperty("servicekeeper.config.dir")**

**2. 使用默认值 System.getProperty("user.dir") + File.separator + "conf"**

**3. 如果以上路径下均不存在service-keeper.properties，则尝试从System.getProperty("user.dir") + File.separator + \"src\" + File.separator  + \"main\" + File.separator  + \"resources\"目录下获取，如果成功则以此文件为准，否则以第3步结果为配置文件父目录**

>**ServiceKeeper支持通过环境变量或者系统属性自定义配置文件名称(默认为service-keeper.properties)，如：System.setProperty("servicekeeper.config.name", "servicekeeper-test.properties")**
