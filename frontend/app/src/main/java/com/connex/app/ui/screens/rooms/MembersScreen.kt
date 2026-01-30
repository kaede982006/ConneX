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
        topBar = {
            TopAppBar(
                title = { Text("멤버") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = { TextButton(onClick = { vm.refresh(roomId) }) { Text("새로고침") } }
            )
        }
    ) { pad ->
        Column(modifier = Modifier.padding(pad).fillMaxSize()) {
            when (val s = state) {
                is UiState.Idle, is UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is UiState.Error -> Text("멤버 로드 실패", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                is UiState.Ready -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(s.value) { m ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("${m.displayName ?: m.username} (${m.username})", style = MaterialTheme.typography.titleMedium)
                                    Text("userId: ${m.userId}", style = MaterialTheme.typography.bodySmall)
                                    if (m.role != null) {
                                        Text("role: ${m.role}", style = MaterialTheme.typography.bodySmall)
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
