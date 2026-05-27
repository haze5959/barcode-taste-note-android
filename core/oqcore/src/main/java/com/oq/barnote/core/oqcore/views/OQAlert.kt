package com.oq.barnote.core.oqcore.views

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun OQAlert(
    title: String,
    message: String,
    confirmText: String = "OK",
    cancelText: String? = null,
    onConfirm: () -> Unit,
    onCancel: () -> Unit = {},
    onDismissRequest: () -> Unit = onCancel
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(confirmText)
            }
        },
        dismissButton = cancelText?.let {
            {
                TextButton(onClick = {
                    onCancel()
                    onDismissRequest()
                }) {
                    Text(it)
                }
            }
        }
    )
}
