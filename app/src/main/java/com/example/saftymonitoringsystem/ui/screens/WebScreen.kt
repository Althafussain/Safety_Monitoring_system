package com.example.saftymonitoringsystem.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.saftymonitoringsystem.ui.theme.DarkBackground
import com.example.saftymonitoringsystem.ui.theme.DarkSurface
import com.example.saftymonitoringsystem.ui.theme.OnDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Web Portal", color = OnDark)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        WebContent(modifier = Modifier.padding(padding))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebContent(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl("https://www.google.com")
            }
        },
        update = { webView ->
            if (webView.url.isNullOrEmpty()) {
                webView.loadUrl("https://www.google.com")
            }
        }
    )
}
