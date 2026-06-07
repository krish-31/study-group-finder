package com.studygroupfinder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.studygroup.finder.core.navigation.AppNavGraph
import com.studygroup.finder.core.navigation.Screen
import com.studygroupfinder.app.ui.theme.StudyGroupFinderTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry-point Activity for Study Group Finder.
 *
 * Annotated with @AndroidEntryPoint so Hilt can inject dependencies
 * into this Activity and its hosted Compose content.
 *
 * Navigation always begins at [Screen.Splash], which shows an animated
 * logo and then routes to Login or Home based on the user's auth state.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyGroupFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    )
                }
            }
        }
    }
}
