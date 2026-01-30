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
import com.connex.app.viewmodel.RoomsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onOpenRoom: (String) -> Unit,
    vm: RoomsViewModel = hiltViewModel()
) {
    val roomsState by vm.rooms.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refresh() }

    if (showCreate) CreateRoomDialog(
        onDismiss = { showCreate = false },
        onCreate = { title -> showCreate = false; vm.createRoom(title) }
    )

    if (showJoin) JoinRoomDialog(
        onDismiss = { showJoin = false },
        onJoin = { roomId -> showJoin = false; vm.joinRoom(roomId) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ConneX") },
                actions = {
                    TextButton(onClick = { showJoin = true }) { Text("가입") }
                    TextButton(onClick = { showCreate = true }) { Text("생성") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            when (val s = roomsState) {
                is UiState.Idle, is UiState.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is UiState.Error -> {
                    Text(
                        "방 목록 로드 실패",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is UiState.Ready -> {
                    if (s.value.isEmpty()) {
                        Text("가입된 방이 없다.", modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(s.value) { r ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRoom(r.id.toString()) }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(r.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                                            Text("roomId: ${r.id}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = { vm.leaveRoom(r.id.toString()) }) { Text("나가기") }
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
private fun CreateRoomDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (title.isNotBlank()) onCreate(title.trim()) },
                enabled = title.isNotBlank()
            ) { Text("생성") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        title = { Text("방 생성") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("방 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun JoinRoomDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var roomId by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { if (roomId.isNotBlank()) onJoin(roomId.trim()) },
                enabled = roomId.isNotBlank()
            ) { Text("가입") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        title = { Text("방 가입") },
        text = {
            OutlinedTextField(
                value = roomId,
                onValueChange = { roomId = it },
                label = { Text("roomId") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}
