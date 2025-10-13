package com.example.myfirstapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Before calling super.onCreate()
        window.setBackgroundDrawableResource(android.R.color.transparent)

        super.onCreate(savedInstanceState)

        // Set transparent theme programmatically
        setTheme(R.style.Theme_MyFirstApp)

        // Remove status bar / fullscreen
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN

        // Set the background color directly
        window.setBackgroundDrawable(
            android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF00BCD4.toInt(), 0xFF00838F.toInt())
            )
        )

        setContent {
            // your existing code unchanged
            val context = LocalContext.current
            val isSystemInDarkTheme = remember {
                val nightModeFlags = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }

            val PremiumAqua = Color(0xFF00BCD4)
            val PremiumDarkAqua = Color(0xFF00838F)
            val imageColor = if (isSystemInDarkTheme) Color(0xFF2F2F2F) else Color.White

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PremiumAqua, PremiumDarkAqua)
                        )
                    )
                    .padding(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vit_logo),
                    contentDescription = "VIT Logo",
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(imageColor)
                )
            }

            LaunchedEffect(Unit) {
                delay(4000L)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        }
    }
}
