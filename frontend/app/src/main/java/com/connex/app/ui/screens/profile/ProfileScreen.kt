package com.connex.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.R
import com.connex.app.core.util.UiState
import com.connex.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogoutSuccess: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val profileState by vm.profile.collectAsState()
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadProfile()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = { IconButton(onClick = onBack) { Text("<", color = MaterialTheme.colorScheme.onBackground) } }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            val username = vm.getUsername()
            val userId = vm.meUserId()
            
            Text("사용자: $username", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("ID: $userId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            when (val s = profileState) {
                is UiState.Ready -> {
                    if (displayName.isBlank()) {
                        displayName = s.value.displayName ?: ""
                    }
                }
                is UiState.Error -> {
                    Text("프로필 로드 실패", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    CircularProgressIndicator()
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("표시 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { vm.updateDisplayName(displayName) },
                enabled = displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("표시 이름 저장")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    vm.logout()
                    onLogoutSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.logout_btn))
            }
        }
    }
}
