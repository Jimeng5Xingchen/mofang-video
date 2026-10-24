# Android CI 配置说明

本目录包含 GitHub Actions 工作流，用于自动构建**手机端**与 **TV 端**签名 APK，并产出源码压缩包。

## 工作流

| 文件 | 触发条件 | 作用 |
|------|----------|------|
| `android.yml` | push 到 main/master、tag `v*`、PR、手动 | JVM 单测 + 双端签名 APK + 源码包 + Release 发布 |

## 产物

| 产物 | Artifact 名 | 说明 |
|------|-------------|------|
| 手机端 APK | `mofang-video-phone-apk` | `:app:assembleRelease` |
| TV 端 APK | `mofang-video-tv-apk` | `:app-tv:assembleRelease` |
| 源码 ZIP | `mofang-video-source-zip` | 通用 zip 格式（.git / build / .gradle / .github / apk 已排除） |
| 源码 tar.gz | `mofang-video-source-targz` | 通用 tar.gz 格式（同上排除） |
| `SHA256SUMS.txt` | 同上 | 所有 Release 产物的校验和 |

推 `v*` tag 时会自动创建 GitHub Release 并附加全部产物。

## 必需的 Repository Secrets

签名工作流依赖以下 4 个 Secret。没有它们时构建不会失败，但产物是**未签名** APK（只能安装到允许未签名的场景，且无法覆盖安装）。

| Secret 名 | 说明 | 示例 |
|-----------|------|------|
| `ANDROID_KEYSTORE_BASE64` | `.jks` 密钥库的 base64 编码（单行） | `base64 -w0 release.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库密码 | `storepassword123` |
| `ANDROID_KEY_ALIAS` | 密钥别名 | `mofang-release` |
| `ANDROID_KEY_PASSWORD` | 密钥密码 | `keypassword123` |

设置路径：仓库 `Settings` → `Secrets and variables` → `Actions` → `New repository secret`。

### 生成密钥库

```bash
keytool -genkeypair -v \
  -keystore release.jks \
  -alias mofang-release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass storepassword123 \
  -keypass keypassword123 \
  -dname "CN=MofangVideo, OU=Release, O=MofangVideo, L=, S=, C=CN"
```

### 生成 base64 并上传

```bash
base64 -w0 release.jks
```

把输出整行粘贴到 `ANDROID_KEYSTORE_BASE64` Secret。

> 本地构建时也可用环境变量或 JVM 系统属性（`-D`）提供同样的四项：
> `RELEASE_STORE_FILE` / `RELEASE_STORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD`。
> `Signing` 对象优先读系统属性（`-D`），其次读同名环境变量。

## 工作原理

`buildSrc/src/main/kotlin/Signing.kt` 在配置期检查签名信息是否齐备且密钥库文件真实存在：

- 四项齐备 → 注册 `release` 签名配置，`release` 构建产物带签名。
- 缺任一项 / 密钥库不存在 → 跳过签名配置，构建继续（warning），产物未签名。

因此缺少 Secret 不会让 CI 红灯，只是产物不可分发。

## 本地复现 CI

```bash
cd mofang-video
gradle :app:testDebugUnitTest

gradle :app:assembleRelease \
  -DRELEASE_STORE_FILE=/path/to/release.jks \
  -DRELEASE_STORE_PASSWORD=storepass \
  -DRELEASE_KEY_ALIAS=alias \
  -DRELEASE_KEY_PASSWORD=keypass

gradle :app-tv:assembleRelease \
  -DRELEASE_STORE_FILE=/path/to/release.jks \
  -DRELEASE_STORE_PASSWORD=storepass \
  -DRELEASE_KEY_ALIAS=alias \
  -DRELEASE_KEY_PASSWORD=keypass
```

或用环境变量（等价效果）：

```bash
export RELEASE_STORE_FILE=/path/to/release.jks
export RELEASE_STORE_PASSWORD=storepass
export RELEASE_KEY_ALIAS=alias
export RELEASE_KEY_PASSWORD=keypass
gradle :app:assembleRelease :app-tv:assembleRelease
```
