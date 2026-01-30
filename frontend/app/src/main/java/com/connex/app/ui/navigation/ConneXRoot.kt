package com.connex.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.connex.app.ui.screens.auth.LoginScreen
import com.connex.app.ui.screens.auth.RegisterScreen
import com.connex.app.ui.screens.chat.ChatScreen
import com.connex.app.ui.screens.rooms.MembersScreen
import com.connex.app.ui.screens.rooms.RoomDetailScreen
import com.connex.app.ui.screens.rooms.RoomsScreen
import com.connex.app.ui.screens.splash.SplashScreen

@Composable
fun ConneXRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            SplashScreen(
                onGoLogin = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
                onGoRooms = {
                    navController.navigate(Routes.Rooms) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Login) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.Rooms) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                onGoRegister = { navController.navigate(Routes.Register) }
            )
        }

        composable(Routes.Register) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.Rooms) {
                        popUpTo(Routes.Register) { inclusive = true }
                    }
                },
                onGoLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.Rooms) {
            RoomsScreen(
                onOpenRoom = { roomId ->
                    navController.navigate("${Routes.Room}/$roomId")
                }
            )
        }

        composable(
            route = "${Routes.Room}/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            RoomDetailScreen(
                roomId = roomId,
                onBack = { navController.popBackStack() },
                onOpenChat = { channelId ->
                    navController.navigate("${Routes.Chat}/$roomId/$channelId")
                },
                onOpenMembers = {
                    navController.navigate("${Routes.Members}/$roomId")
                },
                onOpenRoles = {
                    // Navigate to roles screen or show dialog
                    // For now, no route defined, maybe do nothing or show missing
                    // But signature requires it.
                }
            )
        }

        composable(
            route = "${Routes.Chat}/{roomId}/{channelId}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("channelId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
            ChatScreen(
                roomId = roomId,
                channelId = channelId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.Members}/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            MembersScreen(
                roomId = roomId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
