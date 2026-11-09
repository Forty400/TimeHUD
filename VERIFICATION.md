# TimeHUD v0.1.4 验证结果

## 已在本机执行

| 项 | 结果 | 说明 |
|----|------|------|
| 源码按清单实现 | 是 | 设置页分组、尺寸表、格式、锁定穿透、前台服务、无角标渠道等 |
| 单元测试代码 | 已提供 | `SizeCalculator` / `TimeFormatter` / `PassthroughPolicy` |
| 纯 Java 单元测试运行 | **通过** | JDK 17 下 JUnit 11 tests OK（不经 Gradle） |
| Android SDK 安装 | **已完成** | cmdline-tools + platforms;android-35 + build-tools;35.0.0 |
| Gradle `test` + `assembleRelease` | **通过** | 已产出签名 APK（无 release keystore 时为 debug 签名） |
| Lint 全量 | **未单独跑完** | 可再执行 `gradlew.bat lint` |
| 真机验收 | **未验证** | 无可用设备会话 |

## 待环境就绪后执行

```bash
gradlew.bat test
gradlew.bat lint
gradlew.bat assembleRelease
```

真机建议覆盖：

1. 跨 App 显示、整屏录屏可读
2. 四种时间格式、最小尺寸、透明度 0% / 100%、六种颜色
3. 拖动、菜单、锁定穿透、重开后设置保留
4. 权限拒绝与撤销、关闭状态同步、屏幕边缘、旋转、小屏、大字体
5. 覆盖升级后通知正常且无新增角标
6. 系统安全页可能禁浮窗；毫秒显示 ≠ 毫秒级刷新说明可读
