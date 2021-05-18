---
sort: 5
---

# 人工降级
紧急状况下，可能需要人为控制服务的状态，此时可使用`Service Keeper`提供的人工降级功能，强制切换熔断器到打开状态，此后所有对原始方法的调用都会执行降级方法，直到手动关闭该功能(**默认关闭**)。
```note
当前版本该功能的实现依赖于熔断器，即强制设置熔断器为FORCED_OPEN状态
```

### 配置文件配置
在配置文件中加入如下配置：
```properties
esa.servicekeeper.TestController.list.forcedOpen=true
```
```note
1. 当该值为true或未配置时，表示正常开启方法重试
2. 人工降级功能支持动态配置，实时生效
```
