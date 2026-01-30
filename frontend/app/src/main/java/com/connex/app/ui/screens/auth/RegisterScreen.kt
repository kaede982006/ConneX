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
fun RegisterScreen(
    onRegistered: () -> Unit,
    onGoLogin: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is UiState.Ready) onRegistered()
    }

    Scaffold { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ConneX 회원가입", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("아이디(3~32, 영문/숫자/_)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("표시 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호(8자 이상)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            val canSubmit =
                Validators.isValidUsername(username) &&
                Validators.nonBlank(displayName) &&
                Validators.isValidPassword(password) &&
                state !is UiState.Loading

            Button(
                onClick = { vm.register(username, password, displayName) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state is UiState.Loading) "가입 중..." else "가입")
            }

            TextButton(onClick = onGoLogin) { Text("로그인으로") }

            if (state is UiState.Error) {
                Text("회원가입 실패(이미 존재하는 아이디일 수 있음)", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
