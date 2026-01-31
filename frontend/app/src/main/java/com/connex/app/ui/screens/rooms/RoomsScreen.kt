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
import androidx.compose.ui.res.stringResource
import com.connex.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    onOpenRoom: (String) -> Unit,
    onGoProfile: () -> Unit,
    vm: RoomsViewModel = hiltViewModel()
) {
    val roomsState by vm.rooms.collectAsState()
    val searchState by vm.searchResults.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.refresh() }

    if (showCreate) CreateRoomDialog(
        onDismiss = { showCreate = false },
        onCreate = { title -> showCreate = false; vm.createRoom(title) }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    TextButton(onClick = { showCreate = true }) { Text(stringResource(R.string.create_btn)) }
                    IconButton(onClick = onGoProfile) {
                        Text("P") 
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("방 검색", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("방 이름 또는 ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.searchRooms(query.trim()) },
                            enabled = query.isNotBlank(),
                            shape = MaterialTheme.shapes.extraLarge
                        ) { Text("검색") }
                        TextButton(onClick = { query = ""; vm.clearSearch() }) { Text("초기화") }
                    }
                }
            }
            when (val s = searchState) {
                is UiState.Idle -> {
                    item {
                        Text(
                            "검색어를 입력하세요",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Loading -> {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        Text("검색 실패", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                is UiState.Ready -> {
                    if (s.value.isEmpty()) {
                        item {
                            Text(
                                "검색 결과가 없습니다",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        items(s.value.take(5)) { room ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(room.title ?: "Untitled", style = MaterialTheme.typography.titleSmall)
                                        Text("ID: ${room.roomId}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (room.isJoined) {
                                        Text("가입됨", color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        TextButton(onClick = { vm.joinRoom(room.roomId.toString()) }) { Text("가입") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            when (val s = roomsState) {
                is UiState.Idle, is UiState.Loading -> {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            "방 목록 로드 실패",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                is UiState.Ready -> {
                    if (s.value.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_joined_rooms),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        item {
                            Text(
                                "참여 중인 방",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(s.value) { r ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenRoom(r.id.toString()) }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(r.name ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { vm.leaveRoom(r.id.toString()) }) { Text(stringResource(R.string.leave_room_btn), color = MaterialTheme.colorScheme.error) }
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
