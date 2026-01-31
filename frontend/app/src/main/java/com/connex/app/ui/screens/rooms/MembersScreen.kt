package com.connex.app.ui.screens.rooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.core.util.UiState
import com.connex.app.viewmodel.MembersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    roomId: String,
    onBack: () -> Unit,
    vm: MembersViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(roomId) { vm.refresh(roomId) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("멤버") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = { IconButton(onClick = onBack) { Text("<", color = MaterialTheme.colorScheme.onBackground) } },
                actions = { TextButton(onClick = { vm.refresh(roomId) }) { Text("새로고침") } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UiState.Error -> Text("멤버 로드 실패", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                is UiState.Ready -> {
                    val data = s.value
                    val isOwner = data.ownerId != null && data.ownerId == data.meId
                    val myRoles = data.members.firstOrNull { it.userId == data.meId }?.roles ?: emptyList()
                    val isManager = myRoles.contains("manager")
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(data.members) { m ->
                            val roles = m.roles
                            val isTargetManager = roles.contains("manager")
                            val isRoomOwner = data.ownerId != null && data.ownerId == m.userId
                            val canBanTarget = (isOwner || isManager) &&
                                !isRoomOwner &&
                                data.meId != m.userId &&
                                (isOwner || !isTargetManager)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        m.displayName ?: m.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (roles.isNotEmpty()) {
                                        Text(
                                            roles.joinToString(" · ") { roleLabel(it) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                    if (isOwner && !isRoomOwner) {
                                        val buttonLabel = if (isTargetManager) "부방장 해제" else "부방장 지정"
                                        Button(
                                            onClick = {
                                                if (isTargetManager) {
                                                    vm.revokeSubAdmin(roomId, m.userId)
                                                } else {
                                                    vm.assignSubAdmin(roomId, m.userId)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(buttonLabel)
                                        }
                                    }
                                    if (canBanTarget) {
                                        OutlinedButton(
                                            onClick = { vm.banMember(roomId, m.userId) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error
                                            )
                                        ) {
                                            Text("영구 밴")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "owner" -> "방장"
    "manager" -> "부방장"
    else -> role
}
