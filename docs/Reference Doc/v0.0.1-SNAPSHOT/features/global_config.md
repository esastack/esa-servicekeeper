---
sort: 1300
---

# 全局配置

### 一、全局禁用
#### 1.1 概述
在实际应用过程中，可能需要暂时禁用服务治理的全部功能，如压测阶段。`Service Keeper`提供了全局开关，通过配置文件即可轻松禁用服务治理的所有功能。

#### 1.2 使用示例
##### 方式一：配置环境变量servicekeeper_disable为true或者设置系统属性
System.setProperty("servicekeeper.disable", "true")

##### 方式二：配置文件
在service-keeper.properties文件中加入如下配置，即可禁用服务治理的所有功能：
```properties
servicekeeper.disable=true
```
```note
1. 当该值为false或是未配置时表示正常开启服务治理功能。
2. 该功能支持动态配置，实时生效。
```
### 二、禁用参数级治理
#### 2.1 概述
对于不需要使用参数级服务治理功能的应用，可以选择关闭该功能，减少不必要的性能损耗。

#### 2.2 使用示例
##### 方式一：配置环境变量servicekeeper_arg_level_enable为false或者系统属性：
System.setProperty("servicekeeper.arg.level.enable", "false")
##### 方式二：配置文件
在service-keeper.properties文件中加入如下配置，即可禁用参数级治理规则：
```properties
servicekeeper.arg.level.enable=false
```
```note
1. 当该值为true或是未配置时表示正常开启参数级服务治理功能。
2. 该功能支持动态配置，实时生效。
```

### 三、禁用方式重试
#### 3.1 概述
在实际应用过程中，可能在某种场景下放弃使用重试，`Service Keeper`提供了全局开关，通过配置文件即可轻松禁用方法重试功能。

#### 3.2 使用示例
##### 方式一：配置环境变量servicekeeper_retry_enable为false或者设置系统属性：
System.setProperty("servicekeeper.retry.enable", "false")
##### 方式二：配置文件
在service-keeper.properties文件中加入如下配置，即可禁用方法重试治理规则：

```properties
servicekeeper.retry.enable=false
```

```note
1. 当该值为true或未配置时，表示正常开启方法重试
2. 该功能支持动态配置，实时生效。
```

### 四、禁用外部配置
#### 3.1 概述
在本地开发时，如果不配置管控平台相关配置，应用无法正常启动，而这些配置在开发、测试、生产环境都是自动获取的，无需业务配置。因此，临时禁用这些外部配置的功能是需要的。

#### 3.2 使用方式

配置环境变量servicekeeper_configurators_disable为true或者设置系统属性：
System.setProperty("servicekeeper.configurators.disable", "true")
