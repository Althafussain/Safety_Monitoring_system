package com.example.saftymonitoringsystem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saftymonitoringsystem.ui.SafetyViewModel
import com.example.saftymonitoringsystem.ui.screens.MonitoringScreen
import com.example.saftymonitoringsystem.ui.theme.SaftyMonitoringSystemTheme

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.saftymonitoringsystem.ui.screens.ContactsScreen
import com.example.saftymonitoringsystem.ui.screens.DashboardScreen
import com.example.saftymonitoringsystem.ui.screens.HistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaftyMonitoringSystemTheme {
                val navController = rememberNavController()
                val viewModel: SafetyViewModel = viewModel()
                
                var permissionsGranted by remember {
                    mutableStateOf(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.SEND_SMS
                        ).all {
                            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        permissionsGranted = permissions.values.all { it }
                    }
                )

                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.SEND_SMS
                            )
                        )
                    }
                }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            onStartMonitoring = {
                                if (permissionsGranted) {
                                    navController.navigate("monitoring")
                                } else {
                                    launcher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.SEND_SMS
                                        )
                                    )
                                }
                            },
                            onNavigateToContacts = {
                                navController.navigate("contacts")
                            },
                            onNavigateToHistory = {
                                navController.navigate("history")
                            }
                        )
                    }
                    composable("monitoring") {
                        MonitoringScreen(viewModel)
                    }
                    composable("contacts") {
                        ContactsScreen(viewModel, onBack = { navController.popBackStack() })
                    }
                    composable("history") {
                        HistoryScreen(viewModel, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
