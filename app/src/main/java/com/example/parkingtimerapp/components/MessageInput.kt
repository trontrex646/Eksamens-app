package com.example.parkingtimerapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx. compose. foundation. border
@Composable
fun MessageInput(sendBluetoothMessage: (String) -> Unit) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Send a message to Arduino", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Teksta ievades lauks
        BasicTextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { sendBluetoothMessage(textState.text) },
            enabled = textState.text.isNotEmpty()
        ) {
            Text("Send")
        }
    }
}
