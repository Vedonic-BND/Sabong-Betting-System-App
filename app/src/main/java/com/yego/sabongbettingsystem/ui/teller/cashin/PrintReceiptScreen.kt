package com.yego.sabongbettingsystem.ui.teller.cashin

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yego.sabongbettingsystem.data.api.RetrofitClient

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintReceiptScreen(
    navController: NavController,
    reference: String
) {
    val context     = LocalContext.current
    var isLoading   by remember { mutableStateOf(true) }
    var webViewRef  by remember { mutableStateOf<WebView?>(null) }

    // build receipt URL from base URL
    val baseUrl     = RetrofitClient.BASE_URL
        .replace("/api/", "")
    val receiptUrl  = "$baseUrl/receipt/$reference"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Print Receipt", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // manually trigger print again if needed
                        webViewRef?.evaluateJavascript("window.print();", null)
                    }) {
                        Icon(Icons.Default.Print, contentDescription = "Print")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
                            ) {
                                isLoading = false
                            }
                        }

                        loadUrl(receiptUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}