package com.jackasan1.deepseekmanager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Only the one endpoint DeepSeek actually documents for account data:
 *   GET https://api.deepseek.com/user/balance
 *
 * Deliberately NOT implemented: the platform.deepseek.com/*
 * "usage" endpoints. Those are the web console's private API, they
 * authenticate with a browser session token (not an API key), and
 * DeepSeek makes no stability promise about them.
 */
class DeepSeekApi(
    private val client: OkHttpClient = defaultClient(),
) {
    suspend fun fetchBalance(apiKey: String): Result<Balance> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BALANCE_URL)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Accept", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> parse(raw)
                    response.code == 401 -> failure(BalanceError.Unauthorized)
                    response.code == 403 -> failure(BalanceError.Unauthorized)
                    response.code == 429 -> failure(BalanceError.RateLimited)
                    response.code >= 500 -> failure(BalanceError.Server(response.code))
                    else -> failure(BalanceError.Malformed("HTTP ${response.code}"))
                }
            }
        } catch (e: IOException) {
            failure(BalanceError.Network)
        }
    }

    private fun parse(raw: String): Result<Balance> = try {
        val root = JSONObject(raw)
        val array = root.optJSONArray("balance_infos")
        val infos = buildList {
            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    add(
                        BalanceInfo(
                            currency = item.optString("currency", "CNY"),
                            totalBalance = item.optString("total_balance", "0.00"),
                            grantedBalance = item.optString("granted_balance", "0.00"),
                            toppedUpBalance = item.optString("topped_up_balance", "0.00"),
                        )
                    )
                }
            }
        }
        Result.success(
            Balance(
                isAvailable = root.optBoolean("is_available", false),
                infos = infos,
            )
        )
    } catch (e: Exception) {
        failure(BalanceError.Malformed(e.message ?: "响应不是合法 JSON"))
    }

    private fun <T> failure(error: BalanceError): Result<T> =
        Result.failure(BalanceException(error))

    companion object {
        const val BALANCE_URL = "https://api.deepseek.com/user/balance"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
