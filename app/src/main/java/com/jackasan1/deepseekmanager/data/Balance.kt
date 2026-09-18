package com.jackasan1.deepseekmanager.data

/**
 * Mirrors the official GET /user/balance payload.
 * All money fields arrive as decimal *strings* — we never parse them into
 * Double, so no precision is ever lost in display.
 */
data class BalanceInfo(
    val currency: String,
    val totalBalance: String,
    val grantedBalance: String,
    val toppedUpBalance: String,
)

data class Balance(
    val isAvailable: Boolean,
    val infos: List<BalanceInfo>,
) {
    /** Falls back to a zeroed CNY entry so the UI always has something to render. */
    val primary: BalanceInfo
        get() = infos.firstOrNull() ?: BalanceInfo("CNY", "0.00", "0.00", "0.00")
}

/** Typed failures so the UI can say something actionable instead of "请求失败". */
sealed interface BalanceError {
    data object Unauthorized : BalanceError
    data object RateLimited : BalanceError
    data class Server(val code: Int) : BalanceError
    data object Network : BalanceError
    data class Malformed(val detail: String) : BalanceError
}

class BalanceException(val error: BalanceError) : Exception(error.toString())
