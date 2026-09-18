package com.jackasan1.deepseekmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jackasan1.deepseekmanager.data.Balance
import com.jackasan1.deepseekmanager.data.BalanceError
import com.jackasan1.deepseekmanager.data.BalanceException
import com.jackasan1.deepseekmanager.data.CredentialStore
import com.jackasan1.deepseekmanager.data.DeepSeekApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val hasKey: Boolean = false,
    val maskedKey: String? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val balance: Balance? = null,
    val error: BalanceError? = null,
    val lastUpdatedMs: Long? = null,
    val currencyIndex: Int = 0,
    val keyDialogVisible: Boolean = false,
    val keyDraft: String = "",
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val credentials = CredentialStore(application)
    private val api = DeepSeekApi()

    private val _state = MutableStateFlow(
        DashboardUiState(
            hasKey = credentials.apiKey() != null,
            maskedKey = credentials.maskedKey(),
        )
    )
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        if (_state.value.hasKey) load()
    }

    fun load() {
        val key = credentials.apiKey()
        if (key == null) {
            _state.update { it.copy(hasKey = false, maskedKey = null) }
            return
        }
        val firstLoad = _state.value.balance == null
        _state.update { it.copy(loading = firstLoad, refreshing = !firstLoad, error = null) }

        viewModelScope.launch {
            val result = api.fetchBalance(key)
            result.fold(
                onSuccess = { balance ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            balance = balance,
                            error = null,
                            lastUpdatedMs = System.currentTimeMillis(),
                            currencyIndex = it.currencyIndex
                                .coerceIn(0, (balance.infos.size - 1).coerceAtLeast(0)),
                        )
                    }
                },
                onFailure = { throwable ->
                    val error = (throwable as? BalanceException)?.error
                        ?: BalanceError.Malformed(throwable.message ?: "未知错误")
                    _state.update { it.copy(loading = false, refreshing = false, error = error) }
                },
            )
        }
    }

    fun selectCurrency(index: Int) = _state.update { it.copy(currencyIndex = index) }

    fun openKeyDialog() = _state.update {
        it.copy(keyDialogVisible = true, keyDraft = "")
    }

    fun dismissKeyDialog() = _state.update {
        it.copy(keyDialogVisible = false, keyDraft = "")
    }

    fun onKeyDraftChange(value: String) = _state.update { it.copy(keyDraft = value) }

    fun saveKey() {
        val draft = _state.value.keyDraft.trim()
        if (draft.isEmpty()) return
        credentials.saveApiKey(draft)
        _state.update {
            it.copy(
                keyDialogVisible = false,
                keyDraft = "",
                hasKey = true,
                maskedKey = credentials.maskedKey(),
                balance = null,
                error = null,
            )
        }
        load()
    }

    fun clearKey() {
        credentials.clear()
        _state.update {
            DashboardUiState(hasKey = false, maskedKey = null, keyDialogVisible = false)
        }
    }
}
