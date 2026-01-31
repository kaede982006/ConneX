package com.connex.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(24.dp), // Increased padding
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ConneX Join",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("아이디(3~32, 영문/숫자/_)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("표시 이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호(8자 이상)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            val canSubmit =
                Validators.isValidUsername(username) &&
                Validators.nonBlank(displayName) &&
                Validators.isValidPassword(password) &&
                state !is UiState.Loading

            Button(
                onClick = { vm.register(username, password, displayName) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (state is UiState.Loading) "가입 중..." else "가입")
            }
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onGoLogin) { Text("로그인으로") }

            if (state is UiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text((state as UiState.Error).message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
