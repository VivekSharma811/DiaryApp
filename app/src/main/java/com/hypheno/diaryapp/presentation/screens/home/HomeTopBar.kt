package com.hypheno.diaryapp.presentation.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hypheno.diaryapp.ui.theme.DiaryAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onMenuClicked: () -> Unit
) {
    TopAppBar(
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = onMenuClicked) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Text(text = "Diary")
        },
        actions = {
            IconButton(onClick = onMenuClicked) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Date",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    DiaryAppTheme {
        HomeTopBar(TopAppBarDefaults.exitUntilCollapsedScrollBehavior()) {

        }
    }
}
