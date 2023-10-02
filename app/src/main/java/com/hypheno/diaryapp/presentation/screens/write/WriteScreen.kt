package com.hypheno.diaryapp.presentation.screens.write

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.hypheno.diaryapp.model.Diary
import com.hypheno.diaryapp.model.Mood

@OptIn(ExperimentalPagerApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun WriteScreen(
    uiState: UiState,
    moodName: () -> String,
    pagerState: PagerState,
    onBackPressed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
) {
    LaunchedEffect(key1 = uiState.mood) {
        pagerState.scrollToPage(Mood.valueOf(uiState.mood.name).ordinal)
    }

    Scaffold(
        topBar = {
            WriteTopBar(
                selectedDiary = uiState.selectedDiary,
                onBackPressed = onBackPressed,
                onDeleteConfirmed = onDeleteConfirmed,
                moodName = moodName
            )
        }
    ) {
        WriteContent(
            paddingValues = it,
            uiState = uiState,
            pagerState = pagerState,
            title = uiState.title,
            onTitleChanged = onTitleChanged,
            description = uiState.description,
            onDescriptionChanged = onDescriptionChanged,
            onSaveClicked = onSaveClicked
        )
    }
}