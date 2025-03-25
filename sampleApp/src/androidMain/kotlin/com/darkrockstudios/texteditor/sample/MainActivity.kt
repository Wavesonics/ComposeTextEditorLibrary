package com.darkrockstudios.texteditor.sample

import App
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.darkrockstudios.texteditor.sample.ui.theme.TextEditorTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			TextEditorTheme {
				Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
					Box(modifier = Modifier
						.padding(innerPadding)
						.windowInsetsPadding(WindowInsets.ime)
						.background(color = Color.White)) {
						App()
					}
				}
			}
		}
	}
}