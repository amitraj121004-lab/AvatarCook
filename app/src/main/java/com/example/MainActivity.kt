package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioBackground
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.StudioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBackground
                ) {
                    AvatarStudioApp()
                }
            }
        }
    }
}

@Composable
fun AvatarStudioApp() {
    val navController = rememberNavController()
    val studioViewModel: StudioViewModel = viewModel()
    val galleryViewModel: GalleryViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "studio"
    ) {
        composable("studio") {
            StudioScreen(
                viewModel = studioViewModel,
                onNavigateToGallery = {
                    navController.navigate("gallery")
                }
            )
        }
        composable("gallery") {
            GalleryScreen(
                viewModel = galleryViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
