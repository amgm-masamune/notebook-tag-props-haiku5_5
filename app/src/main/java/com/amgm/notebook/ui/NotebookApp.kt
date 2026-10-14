package com.amgm.notebook.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.ui.navigation.NotebookNavHost
import com.amgm.notebook.ui.theme.NotebookTheme

@Composable
fun NotebookApp(container: AppContainer) {
    NotebookTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NotebookNavHost(container)
        }
    }
}
