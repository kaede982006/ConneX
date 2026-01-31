package com.connex.app.ui.screens.chat

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
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
import com.connex.app.BuildConfig
import com.connex.app.core.util.UiState
import com.connex.app.domain.model.ChatPayload
import com.connex.app.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.Alignment

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

    val channelName by vm.channelName.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("# $channelName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = { IconButton(onClick = onBack) { Text("<", color = MaterialTheme.colorScheme.onBackground) } },
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { m ->
                    val payload = vm.decrypt(roomId, channelId, m)
                    MessageItem(
                        senderId = m.senderId,
                        senderName = m.senderName,
                        isMe = meUserId != null && m.senderId == meUserId,
                        payload = payload,
                        createdAtMs = m.createdAtMs,
                        onDownload = { url, name ->
                            val resolved = resolveDownloadUrl(BuildConfig.API_BASE_URL, url)
                            val token = vm.accessToken()
                            val didEnqueue = enqueueDownload(ctx, resolved, name, token)
                            if (!didEnqueue) {
                                val i = Intent(Intent.ACTION_VIEW, Uri.parse(resolved))
                                if (i.resolveActivity(ctx.packageManager) != null) {
                                    ctx.startActivity(i)
                                } else {
                                    Toast.makeText(ctx, "다운로드를 열 수 있는 앱이 없습니다", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(ctx, "다운로드를 시작했습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            if (typingUsers.isNotEmpty()) {
                Text("입력 중: ${typingUsers.joinToString(", ")}", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
                Button(
                    onClick = {
                        val t = text.trim()
                        text = ""
                        vm.setTyping(roomId, channelId, false)
                        vm.sendText(roomId, channelId, t)
                    },
                    enabled = text.isNotBlank(),
                    shape = MaterialTheme.shapes.extraLarge
                ) { Text("전송") }
            }
        }
    }
}

@Composable
private fun MessageItem(
    senderId: String,
    senderName: String?,
    isMe: Boolean,
    payload: ChatPayload,
    createdAtMs: Long,
    onDownload: (String, String?) -> Unit
) {
    val align = if (isMe) Alignment.End else Alignment.Start
    val containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (isMe) "나" else senderName?.ifBlank { null } ?: senderId
    val timeLabel = formatMessageTime(createdAtMs)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        if (!isMe) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
            shape = if (isMe) MaterialTheme.shapes.large.copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)) 
                    else MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                            Text(
                                "다운로드",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable { onDownload(a.downloadUrl, a.name) },
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    else -> Text("[unknown]")
                }
            }
        }
        if (timeLabel.isNotBlank()) {
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun resolveDownloadUrl(baseUrl: String, downloadUrl: String): String {
    if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
        return downloadUrl
    }
    val trimmedBase = baseUrl.removeSuffix("/")
    val normalizedPath = if (downloadUrl.startsWith("/")) downloadUrl else "/$downloadUrl"
    return "$trimmedBase$normalizedPath"
}

private fun enqueueDownload(
    context: Context,
    url: String,
    filename: String?,
    token: String?
): Boolean {
    return try {
        val uri = Uri.parse(url)
        val safeName = filename?.ifBlank { null } ?: uri.lastPathSegment ?: "attachment"
        val request = DownloadManager.Request(uri)
            .setTitle(safeName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        if (!token.isNullOrBlank()) {
            request.addRequestHeader("Authorization", "Bearer $token")
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        true
    } catch (e: Exception) {
        false
    }
}

private fun formatMessageTime(createdAtMs: Long): String {
    if (createdAtMs <= 0L) return ""
    return java.time.Instant.ofEpochMilli(createdAtMs)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
}
