package com.example.ghostframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ghostframe.ui.theme.GhostFrameTheme
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostFrameTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val selectedPhoto = remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedPhoto.value = uri
        }
    }
    
    Column(modifier = modifier) {
      Button(
          onClick = {
              photoPicker.launch(
                  PickVisualMediaRequest(
                      ActivityResultContracts.PickVisualMedia.ImageOnly
                  )
              )
          }
      ) {
          Text("Choose photo")
      }

      if (selectedPhoto.value != null) {
          Text("Photo selected")
      }
        
      Button(
          modifier = modifier,
          onClick = {
              if (Settings.canDrawOverlays(context)) {
                  val intent = Intent(context, OverlayService::class.java).apply {
                      data = selectedPhoto.value
                  }
                  androidx.core.content.ContextCompat.startForegroundService(
                      context,
                      intent
                  )
                  
                  (context as? Activity)?.moveTaskToBack(true)
              } else {
                  val intent = Intent(
                      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:${context.packageName}")
                  )
                  context.startActivity(intent)
              }
          }
      ) {
          Text("Open overlay")
      }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GhostFrameTheme {
        Greeting("Android")
    }
}