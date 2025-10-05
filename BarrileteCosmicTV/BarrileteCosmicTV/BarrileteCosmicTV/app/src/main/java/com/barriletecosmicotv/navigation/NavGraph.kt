package com.barriletecosmicotv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.barriletecosmicotv.data.SessionManager
import com.barriletecosmicotv.screens.HomeScreen
import com.barriletecosmicotv.screens.LoginScreen
import com.barriletecosmicotv.screens.StreamScreen
import kotlinx.coroutines.runBlocking

@Composable
fun NavGraph(
    navController: NavHostController,
    onSessionExpired: () -> Unit = {} // callback que viene de MainActivity
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }

    // Estado en tiempo real: ¿hay sesión activa?
    val isLoggedIn by session.isLoggedIn.collectAsState(initial = false)

    // Flag para saber si en esta ejecución ya hubo sesión
    val wasLoggedIn = remember { mutableStateOf(false) }

    // Observa cambios: si antes estaba logueado y ahora no -> sesión caducó
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            wasLoggedIn.value = true
        } else if (wasLoggedIn.value) {
            // Caducó: mostramos mensaje y mandamos a login
            onSessionExpired()
            navController.navigate(Screens.LOGIN) {
                popUpTo(route = Screens.HOME) { inclusive = true }
                launchSingleTop = true
            }
            wasLoggedIn.value = false
        }
    }

    // Decide HOME o LOGIN en el arranque
    NavHost(
        navController = navController,
        startDestination = Screens.START
    ) {
        composable(route = Screens.START) {
            LaunchedEffect(Unit) {
                val hasSession = try {
                    runBlocking { session.hasValidSession() }
                } catch (_: Throwable) {
                    false
                }
                if (hasSession) {
                    wasLoggedIn.value = true
                    navController.navigate(Screens.HOME) {
                        popUpTo(route = Screens.START) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Screens.LOGIN) {
                        popUpTo(route = Screens.START) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }

        composable(route = Screens.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screens.HOME) {
                        popUpTo(route = Screens.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                    wasLoggedIn.value = true
                }
            )
        }

        composable(route = Screens.HOME) {
            HomeScreen(navController = navController)
        }

        composable(
            route = "${Screens.STREAM}/{streamId}",
            arguments = listOf(
                navArgument("streamId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId") ?: ""
            StreamScreen(
                navController = navController,
                streamId = streamId
            )
        }
    }
}