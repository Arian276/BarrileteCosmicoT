package com.barriletecosmicotv.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.barriletecosmicotv.screens.HomeScreen
import com.barriletecosmicotv.screens.StreamScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screens.HOME
    ) {
        composable(Screens.HOME) {
            HomeScreen(
                onStreamClick = { streamId ->
                    navController.navigate("${Screens.STREAM}/$streamId")
                }
            )
        }
        
        composable("${Screens.STREAM}/{streamId}") { backStackEntry ->
            val streamId = backStackEntry.arguments?.getString("streamId") ?: ""
            StreamScreen(
                streamId = streamId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}