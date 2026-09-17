package com.example.ghostframe.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ghostframe.ui.theme.GhostFrameTheme

@Composable
fun HomeScreen(
    hasSelectedPhoto: Boolean,
    onChoosePhoto: () -> Unit,
    onOpenOverlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Button(onClick = onChoosePhoto) {
            Text("Choose photo")
        }

        if (hasSelectedPhoto) {
            Text("Photo selected")
        }

        Button(
            onClick = onOpenOverlay,
            enabled = hasSelectedPhoto
        ) {
            Text("Open overlay")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    GhostFrameTheme {
        HomeScreen(
            hasSelectedPhoto = true,
            onChoosePhoto = {},
            onOpenOverlay = {}
        )
    }
}