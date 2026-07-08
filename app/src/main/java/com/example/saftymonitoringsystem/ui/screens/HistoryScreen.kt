package com.example.saftymonitoringsystem.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saftymonitoringsystem.data.model.SafetyIncident
import com.example.saftymonitoringsystem.ui.SafetyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: SafetyViewModel, onBack: () -> Unit) {
    val incidents by viewModel.incidents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No incidents recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(incidents) { incident ->
                    IncidentItem(incident)
                }
            }
        }
    }
}

@Composable
fun IncidentItem(incident: SafetyIncident) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val dateString = remember(incident.timestamp) { sdf.format(Date(incident.timestamp)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateString, style = MaterialTheme.typography.labelMedium)
                Text("Threat: ${incident.threatLevel}%", 
                    color = if (incident.threatLevel > 70) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Emotion: ${incident.emotion}", style = MaterialTheme.typography.bodyLarge)
            if (incident.detectedObjects.isNotEmpty()) {
                Text("Objects: ${incident.detectedObjects.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Location: ${incident.location}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
