package com.connex.app.ui.screens.rooms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.core.util.UiState
import com.connex.app.viewmodel.RoomDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    onOpenChat: (String) -> Unit,
    onOpenMembers: () -> Unit,
    onOpenRoles: () -> Unit,
    onBack: () -> Unit,
    vm: RoomDetailViewModel = hiltViewModel()
) {
    val channelsState by vm.channels.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) { vm.refresh(roomId) }

    if (showCreate) CreateChannelDialog(
        onDismiss = { showCreate = false },
        onCreate = { name -> showCreate = false; vm.createChannel(roomId, name) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("방: $roomId") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = {
                    TextButton(onClick = onOpenMembers) { Text("멤버") }
                    TextButton(onClick = onOpenRoles) { Text("권한") }
                    TextButton(onClick = { showCreate = true }) { Text("채널+") }
                    TextButton(onClick = { vm.refresh(roomId) }) { Text("새로고침") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            when (val s = channelsState) {
                is UiState.Idle, is UiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is UiState.Error -> {
                    Text("채널 로드 실패", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                is UiState.Ready -> {
                    if (s.value.isEmpty()) {
                        Text("채널이 없다.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.value) { ch ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenChat(ch.id.toString()) }
                                            .padding(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("# ${ch.name}", style = MaterialTheme.typography.titleMedium)
                                            Text("channelId: ${ch.id}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(">")
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

@Composable
private fun CreateChannelDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("생성") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        title = { Text("채널 생성") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("채널 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
