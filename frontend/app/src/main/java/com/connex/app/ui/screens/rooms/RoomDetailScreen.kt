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
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    onOpenChat: (String) -> Unit,
    onOpenMembers: () -> Unit,
    onBack: () -> Unit,
    vm: RoomDetailViewModel = hiltViewModel()
) {
    val channelsState by vm.channels.collectAsState()
    val roomTitle by vm.roomTitle.collectAsState()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) { vm.refresh(roomId) }

    if (showCreate) CreateChannelDialog(
        onDismiss = { showCreate = false },
        onCreate = { name -> showCreate = false; vm.createChannel(roomId, name) }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(roomTitle) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = { IconButton(onClick = onBack) { Text("<", color = MaterialTheme.colorScheme.onBackground) } },
                actions = {
                    TextButton(onClick = onOpenMembers) { Text(stringResource(com.connex.app.R.string.members_btn)) }
                    TextButton(onClick = { showCreate = true }) { Text(stringResource(com.connex.app.R.string.new_channel_btn)) }
                    TextButton(onClick = { vm.refresh(roomId) }) { Text(stringResource(com.connex.app.R.string.refresh_btn)) }
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
                    Text(stringResource(com.connex.app.R.string.channel_load_fail), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                is UiState.Ready -> {
                    if (s.value.isEmpty()) {
                        Text(stringResource(com.connex.app.R.string.no_channel), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onBackground)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.value) { ch ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenChat(ch.id.toString()) }
                                            .padding(16.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("# ${ch.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
