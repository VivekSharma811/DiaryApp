package com.hypheno.diaryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.hypheno.diaryapp.navigation.Screen
import com.hypheno.diaryapp.navigation.SetupNavGraph
import com.hypheno.diaryapp.ui.theme.DiaryAppTheme
import com.hypheno.diaryapp.util.Constants.APP_ID
import io.realm.kotlin.mongodb.App

class MainActivity : ComponentActivity() {

    var keepSplashOpened = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setKeepOnScreenCondition {
            keepSplashOpened
        }

        FirebaseApp.initializeApp(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            DiaryAppTheme(dynamicColor = false) {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController
                ) {
                    keepSplashOpened = false
                }
            }
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return if (user != null && user.loggedIn) Screen.Home.route else Screen.Authentication.route
}
