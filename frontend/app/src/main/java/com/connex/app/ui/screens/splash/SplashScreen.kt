package com.connex.app.ui.screens.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.connex.app.viewmodel.AuthViewModel

@Composable
fun SplashScreen(
    onGoLogin: () -> Unit,
    onGoRooms: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val authState by vm.authState.collectAsState()

    LaunchedEffect(Unit) {
        vm.checkAuth()
    }

    LaunchedEffect(authState) {
        if (authState == true) onGoRooms()
        else if (authState == false) onGoLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "ConneX",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
