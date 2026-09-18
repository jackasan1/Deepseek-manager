package com.jackasan1.deepseekmanager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jackasan1.deepseekmanager.R
import com.jackasan1.deepseekmanager.data.BalanceInfo
import com.jackasan1.deepseekmanager.ui.components.ApiKeyDialog
import com.jackasan1.deepseekmanager.ui.components.BalanceHero
import com.jackasan1.deepseekmanager.ui.components.EmptyKeyCard
import com.jackasan1.deepseekmanager.ui.components.ErrorCard
import com.jackasan1.deepseekmanager.ui.components.InfoCard
import com.jackasan1.deepseekmanager.ui.components.UsageScopeNotice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("DeepSeek") },
                subtitle = { Text("API 余额总览") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = "刷新余额",
                        )
                    }
                    IconButton(onClick = { viewModel.openKeyDialog() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_key),
                            contentDescription = "设置 API Key",
                        )
                    }
                },
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.hasKey) {
                EmptyKeyCard(onSetKey = viewModel::openKeyDialog)
            } else {
                val balance = state.balance

                if (state.loading && balance == null) {
                    LoadingBlock()
                }

                if (balance != null) {
                    val info = balance.infos.getOrElse(state.currencyIndex) { balance.primary }
                    BalanceHero(balance = balance, info = info)

                    if (balance.infos.size > 1) {
                        CurrencySelector(
                            infos = balance.infos,
                            selectedIndex = infosSafeIndex(balance.infos, state.currencyIndex),
                            onSelect = viewModel::selectCurrency,
                        )
                    }

                    LastUpdatedRow(
                        lastUpdatedMs = state.lastUpdatedMs,
                        refreshing = state.refreshing,
                        onRefresh = viewModel::load,
                    )
                }

                state.error?.let { error ->
                    ErrorCard(error = error, onRetry = viewModel::load)
                }

                if (balance == null && state.error == null && !state.loading) {
                    InfoCard(
                        iconRes = R.drawable.ic_info,
                        title = "暂无数据",
                        body = "尚未成功获取余额。点击右上角刷新，或检查 API Key 是否正确。",
                    )
                }

                UsageScopeNotice()

                KeyFooter(
                    maskedKey = state.maskedKey,
                    onClear = viewModel::clearKey,
                )
            }
        }
    }

    if (state.keyDialogVisible) {
        ApiKeyDialog(
            draft = state.keyDraft,
            onDraftChange = viewModel::onKeyDraftChange,
            onConfirm = viewModel::saveKey,
            onDismiss = viewModel::dismissKeyDialog,
        )
    }
}

private fun infosSafeIndex(infos: List<BalanceInfo>, index: Int): Int =
    index.coerceIn(0, (infos.size - 1).coerceAtLeast(0))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator(modifier = Modifier.size(52.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrencySelector(
    infos: List<BalanceInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        infos.forEachIndexed { index, info ->
            ToggleButton(
                checked = index == selectedIndex,
                onCheckedChange = { checked -> if (checked) onSelect(index) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = info.currency,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun LastUpdatedRow(
    lastUpdatedMs: Long?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = if (refreshing) "正在刷新…" else "上次刷新",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = lastUpdatedMs?.let(::formatClock) ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        TextButton(onClick = onRefresh) {
            Text("立即刷新", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun KeyFooter(maskedKey: String?, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_key),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = maskedKey ?: "未设置",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClear) { Text("清除凭据") }
    }
}

private fun formatClock(ms: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
