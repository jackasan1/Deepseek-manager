# 设计说明

## 目标

- 竖屏手机，412×892dp
- Material 3 Expressive
- 浅色 + 深色两套（不使用 Material You 动态取色）

## 设计决策

### 1. 一个巨大的数字

Expressive 的排版逻辑是「让一个元素承担主要视觉重量」。
余额是本应用的唯一核心信息，因此用 `displayMedium`（52sp / Bold / -1.5sp 字距）
直接放在 `primaryContainer` 大卡上，其余元素刻意压低音量。

### 2. 大圆角

`Shapes` 被整体放大：extraSmall 8dp → extraLarge 40dp。
Hero 卡用 `extraLarge`，信息卡用 `large`。

### 3. 颜色

品牌基色取 DeepSeek 蓝 `#4D6BFE`（色相约 228）。
浅色与深色方案均为手写，30 个 M3 语义色位逐一赋值，
包含 `surfaceContainerLowest` ~ `surfaceContainerHighest` 全档，
不使用默认回退值。

### 4. 深色模式

不依赖 `Theme.Material.DayNight`，而是 `values/` 与 `values-night/`
分别定义 `window_background`，避免启动瞬间白闪。

### 5. 不用假图表

只有余额数据时，用量区域不放任何占位图表，而是明确写出原因。
这是产品诚实性要求，不是设计取舍。

## 无障碍

- 状态胶囊同时用颜色 + 文字（「可用」/「余额不足」）表达，不单靠颜色
- 所有纯装饰图标 `contentDescription = null`，可交互图标均有描述
- 金额始终以字符串形式展示，不做 Double 转换，避免精度丢失
