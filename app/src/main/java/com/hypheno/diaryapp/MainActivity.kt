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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.hypheno.diaryapp.data.db.ImageToDeleteDao
import com.hypheno.diaryapp.data.db.ImageToUploadDao
import com.hypheno.diaryapp.navigation.Screen
import com.hypheno.diaryapp.navigation.SetupNavGraph
import com.hypheno.diaryapp.ui.theme.DiaryAppTheme
import com.hypheno.diaryapp.util.Constants.APP_ID
import com.hypheno.diaryapp.util.retryDeletingImageFromFirebase
import com.hypheno.diaryapp.util.retryUploadingImageToFirebase
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageToUploadDao: ImageToUploadDao

    @Inject
    lateinit var imageToDeleteDao: ImageToDeleteDao

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

        cleanupCheck(
            scope = lifecycleScope,
            imageToUploadDao = imageToUploadDao,
            imageToDeleteDao = imageToDeleteDao
        )
    }
}

private fun cleanupCheck(
    scope: CoroutineScope,
    imageToUploadDao: ImageToUploadDao,
    imageToDeleteDao: ImageToDeleteDao
) {
    scope.launch(Dispatchers.IO) {
        val result = imageToUploadDao.getAllImages()
        result.forEach { imageToUpload ->
            retryUploadingImageToFirebase(
                imageToUpload = imageToUpload,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imageToUploadDao.cleanupImage(imageId = imageToUpload.id)
                    }
                }
            )
        }
        val result2 = imageToDeleteDao.getAllImages()
        result2.forEach { imageToDelete ->
            retryDeletingImageFromFirebase(
                imageToDelete = imageToDelete,
                onSuccess = {
                    scope.launch(Dispatchers.IO) {
                        imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                    }
                }
            )
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return if (user != null && user.loggedIn) Screen.Home.route else Screen.Authentication.route
}
