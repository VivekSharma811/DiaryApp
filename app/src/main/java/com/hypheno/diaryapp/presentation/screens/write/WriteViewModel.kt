package com.hypheno.diaryapp.presentation.screens.write

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hypheno.diaryapp.data.repository.MongoDB
import com.hypheno.diaryapp.model.Diary
import com.hypheno.diaryapp.model.Mood
import com.hypheno.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.hypheno.diaryapp.util.RequestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId

class WriteViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var uiState = mutableStateOf(UiState())
        private set

    init {
        getDiaryIdArgument()
        getSelectedDiary()
    }

    private fun getDiaryIdArgument() {
        uiState.value =
            uiState.value.copy(
                selectedDiaryId = savedStateHandle.get<String>(
                    WRITE_SCREEN_ARGUMENT_KEY
                )
            )
    }

    private fun getSelectedDiary() {
        if (uiState.value.selectedDiaryId != null) {
            val diaryId = ObjectId.invoke(uiState.value.selectedDiaryId!!)
            viewModelScope.launch(Dispatchers.IO) {
                val diary = MongoDB.getSelectedDiary(
                    diaryId = diaryId
                )
                if (diary is RequestState.Success) {
                    withContext(Dispatchers.Main) {
                        setSelectedDiary(diary.data)
                        setTitle(diary.data.title)
                        setDescription(diary.data.description)
                        setMood(Mood.valueOf(diary.data.mood))
                    }
                }
            }
        }
    }

    private fun setSelectedDiary(diary: Diary) {
        uiState.value = uiState.value.copy(selectedDiary = diary)
    }

    fun setTitle(title: String) {
        uiState.value = uiState.value.copy(title = title)
    }

    fun setDescription(description: String) {
        uiState.value = uiState.value.copy(description = description)
    }

    private fun setMood(mood: Mood) {
        uiState.value = uiState.value.copy(mood = mood)
    }

    fun insertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = MongoDB.addNewDiary(diary)
            if (result is RequestState.Success) {
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } else if (result is RequestState.Error<*>) {
                withContext(Dispatchers.Main) {
                    onError(result.error.message.toString())
                }
            }
        }
    }
}

data class UiState(
    val selectedDiaryId: String? = null,
    val selectedDiary: Diary? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral
)