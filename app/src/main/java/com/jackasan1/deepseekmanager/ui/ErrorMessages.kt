package com.jackasan1.deepseekmanager.ui

import com.jackasan1.deepseekmanager.data.BalanceError

/** Every failure explains what to do next, not just what went wrong. */
fun BalanceError.toHumanMessage(): String = when (this) {
    is BalanceError.Unauthorized ->
        "API Key 无效或已被删除（HTTP 401/403）。请到开放平台核对后重新设置。"
    is BalanceError.RateLimited ->
        "请求过于频繁（HTTP 429）。已保留上次数据，请稍后再试。"
    is BalanceError.Server ->
        "DeepSeek 服务端暂时不可用（HTTP $code）。这是对方的问题，稍后重试即可。"
    is BalanceError.Network ->
        "网络连接失败。请检查网络后重试。"
    is BalanceError.Malformed ->
        "响应无法解析：$detail"
}
