package com.example.ghostframe.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ghostframe.BuildConfig
import com.example.ghostframe.R
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
        Text(
            text = stringResource(
                R.string.app_version_label,
                BuildConfig.VERSION_NAME,
                stringResource(R.string.build_identifier)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
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