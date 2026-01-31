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
import com.connex.app.viewmodel.RolesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolesScreen(
    roomId: String,
    onBack: () -> Unit,
    vm: RolesViewModel = hiltViewModel()
) {
    val rolesState by vm.roles.collectAsState()
    val status by vm.status.collectAsState()

    var roleName by remember { mutableStateOf("") }
    var perms by remember { mutableStateOf("room.manage,channel.create,role.grant") }
    var targetUserId by remember { mutableStateOf("") }
    var targetRoleId by remember { mutableStateOf("") }

    LaunchedEffect(roomId) { vm.refresh(roomId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("역할/권한") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = { TextButton(onClick = { vm.refresh(roomId) }) { Text("새로고침") } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!status.isNullOrBlank()) {
                Text(status ?: "", color = MaterialTheme.colorScheme.tertiary)
            }

            Text("역할 생성", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = roleName, onValueChange = { roleName = it }, label = { Text("역할 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = perms, onValueChange = { perms = it }, label = { Text("권한 CSV") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.createRole(roomId, roleName.trim(), perms) }, enabled = roleName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("생성")
            }

            Divider()

            Text("역할 부여/회수", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = targetUserId, onValueChange = { targetUserId = it }, label = { Text("대상 userId") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = targetRoleId, onValueChange = { targetRoleId = it }, label = { Text("roleId") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { vm.grant(roomId, targetUserId.trim(), targetRoleId.trim()) }, enabled = targetUserId.isNotBlank() && targetRoleId.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text("부여")
                }
                Button(onClick = { vm.revoke(roomId, targetUserId.trim(), targetRoleId.trim()) }, enabled = targetUserId.isNotBlank() && targetRoleId.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Text("회수")
                }
            }

            Divider()

            Text("역할 목록", style = MaterialTheme.typography.titleMedium)
            when (val s = rolesState) {
                is UiState.Idle, is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UiState.Error -> Text("역할 로드 실패", color = MaterialTheme.colorScheme.error)
                is UiState.Ready -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(s.value) { r ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(r.name, style = MaterialTheme.typography.titleMedium)
                                    Text("roleId: ${r.id}", style = MaterialTheme.typography.bodySmall)
                                    if (r.permissions.isNotEmpty()) {
                                        Text("perms: ${r.permissions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
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
