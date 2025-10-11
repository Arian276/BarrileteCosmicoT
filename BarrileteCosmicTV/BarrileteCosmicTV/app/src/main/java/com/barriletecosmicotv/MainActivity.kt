package com.barriletecosmicotv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.barriletecosmicotv.navigation.NavGraph
import com.barriletecosmicotv.ui.theme.BarrileteCosmicTVTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BarrileteCosmicTVTheme {
                val snackbarHostState = SnackbarHostState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            onSessionExpired = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Tu sesión caducó"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}