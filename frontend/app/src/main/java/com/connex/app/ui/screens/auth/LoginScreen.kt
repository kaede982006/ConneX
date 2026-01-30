package com.connex.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.core.util.UiState
import com.connex.app.core.util.Validators
import com.connex.app.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onGoRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is UiState.Ready) onLoggedIn()
    }

    Scaffold { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ConneX 로그인", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("아이디") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            val canSubmit = Validators.isValidUsername(username) && Validators.isValidPassword(password) && state !is UiState.Loading

            Button(
                onClick = { vm.login(username, password) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state is UiState.Loading) "로그인 중..." else "로그인")
            }

            TextButton(onClick = onGoRegister) { Text("회원가입") }

            if (state is UiState.Error) {
                Text("로그인 실패", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
