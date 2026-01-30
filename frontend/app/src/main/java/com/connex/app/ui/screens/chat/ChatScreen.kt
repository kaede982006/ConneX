package com.connex.app.ui.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.core.util.UiState
import com.connex.app.domain.model.ChatPayload
import com.connex.app.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    channelId: String,
    onBack: () -> Unit,
    vm: ChatViewModel = hiltViewModel()
) {
    val msgState by vm.messages.collectAsState()
    val typing by vm.typing.collectAsState()
    val status by vm.status.collectAsState()
    val meUserId = vm.meUserId()

    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            vm.sendFile(roomId, channelId, uri)
        }
    }

    DisposableEffect(roomId, channelId) {
        vm.connect(roomId, channelId)
        onDispose { vm.disconnect() }
    }

    LaunchedEffect(roomId, channelId) {
        snapshotFlow { text }
            .map { it.isNotBlank() }
            .distinctUntilChanged()
            .debounce(500)
            .collect { isTyping ->
                vm.setTyping(roomId, channelId, isTyping)
            }
    }

    val typingUsers = typing.filterValues { it }.keys.take(3).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("채팅: $channelId") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } },
                actions = {
                    TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) { Text("파일") }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            if (!status.isNullOrBlank()) {
                Text(status ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }

            val messages = when (val s = msgState) {
                is UiState.Ready -> s.value
                else -> emptyList()
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { m ->
                    val payload = vm.decrypt(roomId, channelId, m)
                    MessageItem(
                        senderId = m.senderId,
                        isMe = meUserId != null && m.senderId == meUserId,
                        payload = payload,
                        onOpenUrl = { url ->
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            ctx.startActivity(i)
                        }
                    )
                }
            }

            if (typingUsers.isNotEmpty()) {
                Text("입력 중: ${typingUsers.joinToString(", ")}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지") },
                    singleLine = true
                )
                Button(
                    onClick = {
                        val t = text.trim()
                        text = ""
                        vm.setTyping(roomId, channelId, false)
                        vm.sendText(roomId, channelId, t)
                    },
                    enabled = text.isNotBlank()
                ) { Text("전송") }
            }
        }
    }
}

@Composable
private fun MessageItem(
    senderId: String,
    isMe: Boolean,
    payload: ChatPayload,
    onOpenUrl: (String) -> Unit
) {
    val label = if (isMe) "나" else senderId
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            when (payload.type) {
                ChatPayload.TYPE_TEXT, ChatPayload.TYPE_SYSTEM -> {
                    Text(payload.text.orEmpty())
                }
                ChatPayload.TYPE_ATTACHMENT -> {
                    val a = payload.attachment
                    if (a == null) {
                        Text("[attachment]")
                    } else {
                        Text("첨부: ${a.name} (${a.sizeBytes} bytes)")
                        Text(a.downloadUrl, style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { onOpenUrl(a.downloadUrl) })
                    }
                }
                else -> Text("[unknown]")
            }
        }
    }
}
