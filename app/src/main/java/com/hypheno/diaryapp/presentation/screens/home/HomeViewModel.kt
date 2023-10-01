package com.hypheno.diaryapp.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hypheno.diaryapp.data.repository.Diaries
import com.hypheno.diaryapp.data.repository.MongoDB
import com.hypheno.diaryapp.util.RequestState
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    init {
        getAllDiaries()
    }

    private fun getAllDiaries() {
        viewModelScope.launch {
            MongoDB.getAllDiaries().collect { result ->
                diaries.value = result
            }
        }
    }
}