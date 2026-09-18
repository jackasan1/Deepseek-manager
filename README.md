# DeepSeek Manager

一个用来查看 DeepSeek 开放平台 API 账户余额的 Android 原生应用。
界面按 **Material 3 Expressive** 设计，目标竖屏 **412×892dp**，同时提供浅色与深色主题。

---

## 一、先说清楚：这个 App 能拿到什么数据

这一节比代码更重要。DeepSeek 官方**只开放了一个**账户数据接口：

| 数据 | 是否可用 | 所需凭证 |
|---|---|---|
| 账户总余额、赠金余额、充值余额、账户是否可用 | ✅ 官方接口 | **API Key**（`sk-` 开头） |
| 按天 / 按模型的 Token 消耗 | ❌ 未接入 | 网页登录 Token（另一套凭证） |
| 消费金额明细、缓存命中率、请求次数 | ❌ 未接入 | 网页登录 Token（另一套凭证） |

本应用调用的唯一接口：

```
GET https://api.deepseek.com/user/balance
Authorization: Bearer <API_KEY>
```

返回：

```json
{
  "is_available": true,
  "balance_infos": [
    { "currency": "CNY", "total_balance": "110.00",
      "granted_balance": "10.00", "topped_up_balance": "100.00" }
  ]
}
```

### 为什么没有用量图表

`platform.deepseek.com` 网页端确实存在 `/api/v0/usage/amount` 与 `/api/v0/usage/cost`
两个接口，能返回按天、按模型拆分的 Token 与消费明细。但：

1. 它们**不是官方公开 API**，没有文档，也没有任何稳定性承诺；
2. 它们认的是**网页登录 Token**（存在 `localStorage.userToken`），**API Key 完全无效**；
3. 获取该 Token 需要在 WebView 中注入 JS Hook 拦截 `Authorization` 头，属于逆向网页端内部接口。

所以本版本**不显示任何用量图表**，界面上有一张卡片明确说明这一点。
画一个看起来像真的、实际是编造数据的图表，比空着更糟。

### 关于账号密码

本应用**不索取、不存储、不传输**你的 DeepSeek 账号密码。
只接受 API Key，并且：

- 通过 `EncryptedSharedPreferences` + Android Keystore 硬件级主密钥加密后存于本机；
- `AndroidManifest.xml` 中 `allowBackup="false"`，凭据不会被备份带走；
- 界面上只回显掩码形式（`sk-abc…wxyz`），完整 Key 永不显示；
- 提供「清除凭据」入口。

---

## 二、技术栈（含一个必须知道的坑）

| 组件 | 版本 | 说明 |
|---|---|---|
| Kotlin | 2.4.20 | |
| AGP | 9.4.1 | |
| Gradle | 9.7.1 | |
| compileSdk / targetSdk | 36 | |
| minSdk | 26 | |
| **material3** | **1.5.0-alpha28** | Material 3 Expressive |
| **compose ui/foundation/runtime** | **1.13.0-alpha01** | |

### ⚠️ 为什么版本全是 alpha，且不能用 compose-bom

Material 3 Expressive 的组件（`MaterialExpressiveTheme`、`MotionScheme`、
`LargeFlexibleTopAppBar`、`LoadingIndicator`、`MaterialShapes`、`ToggleButton`…）
目前**没有稳定版**：

- 稳定版 `material3` 是 `1.4.0`，里面**没有** Expressive；
- Expressive 位于 `1.5.0-alpha28`，且相关 API 需要 `@ExperimentalMaterial3ExpressiveApi`。

更关键的是版本依赖关系：

```
material3 1.5.0-alpha28  →  依赖 compose ui/foundation/runtime 1.13.0-alpha01
compose-bom 2026.09.00   →  锁定 compose ui 1.12.1、material3 1.4.0
```

两者冲突，因此**本工程刻意不使用 `compose-bom`**，改为在
`gradle/libs.versions.toml` 中显式钉住整套 alpha 版本。

**含义**：上游 API 可能在小版本间变化；升级时请对照
[compose-material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
并重新确认签名。`MaterialExpressiveTheme` 一旦进入稳定版，应尽快切回稳定版。

### 本工程用到的 Expressive 组件

- `MaterialExpressiveTheme` + `MotionScheme.expressive()` —— 弹性/空间化动效
- `LargeFlexibleTopAppBar`（带 subtitle）
- `LoadingIndicator`
- `ToggleButton`（多币种切换）
- 自定义 `Shapes`：最大圆角 40dp

> 刻意**未**使用 `ButtonGroup`：其公开签名首参是一个 `ButtonGroupMenuState` 槽位，
> 与常见文档不一致，为避免不可预期的编译问题，多币种切换改用 `ToggleButton` 行实现。

---

## 三、界面结构

```
LargeFlexibleTopAppBar  (标题 DeepSeek / 副标题 API 余额总览 / 刷新 + 设置 Key)
└── 可滚动 Column，20dp 水平内边距，16dp 间距
    ├── BalanceHero          余额主卡（primaryContainer，超大号数字 + 状态胶囊 + 赠金/充值拆分）
    ├── CurrencySelector     多币种时出现（ToggleButton 行）
    ├── LastUpdatedRow       上次刷新时间 + 立即刷新
    ├── ErrorCard            失败时出现，按 401/429/5xx/网络 分类给出可操作文案
    ├── UsageScopeNotice     诚实说明为何没有用量图表
    └── KeyFooter            掩码 Key + 清除凭据
```

状态机：`无 Key → 空状态引导` / `首次加载 → LoadingIndicator` /
`有数据 → Hero` / `失败 → ErrorCard`（保留上次成功数据）。

---

## 四、构建

CI 会自动构建 APK（`.github/workflows/android.yml`，Gradle 9.7.1 + JDK 21）。

本地构建：

```bash
gradle :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

> 注：`build-tools` 中的 `aapt2` 是 Linux x86_64 二进制，**无法在 Android/ARM64 设备上直接运行**，
> 因此本工程请在桌面或 CI 上构建，不要在 Termux 中构建。

---

## 五、许可证

MIT
