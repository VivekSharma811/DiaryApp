package com.hypheno.diaryapp.presentation.screens.write

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.hypheno.diaryapp.data.db.ImageToUploadDao
import com.hypheno.diaryapp.data.db.entity.ImageToUpload
import com.hypheno.diaryapp.data.repository.MongoDB
import com.hypheno.diaryapp.model.Diary
import com.hypheno.diaryapp.model.GalleryImage
import com.hypheno.diaryapp.model.GalleryState
import com.hypheno.diaryapp.model.Mood
import com.hypheno.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.hypheno.diaryapp.model.RequestState
import com.hypheno.diaryapp.model.rememberGalleryState
import com.hypheno.diaryapp.util.fetchImagesFromFirebase
import com.hypheno.diaryapp.util.toRealmInstant
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mongodb.kbson.ObjectId
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imagesToUploadDao: ImageToUploadDao
) : ViewModel() {

    val galleryState = GalleryState()

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
                MongoDB.getSelectedDiary(diaryId = diaryId)
                    .catch {
                        emit(RequestState.Error<Diary>(Exception("Diary does not exist")))
                    }
                    .collect { diary ->
                        if (diary is RequestState.Success) {
                            withContext(Dispatchers.Main) {
                                setSelectedDiary(diary.data)
                                setTitle(diary.data.title)
                                setDescription(diary.data.description)
                                setMood(Mood.valueOf(diary.data.mood))

                                fetchImagesFromFirebase(
                                    remoteImagePaths = diary.data.images,
                                    onImageDownloaded = { downloadedImage ->
                                        galleryState.addImage(
                                            GalleryImage(
                                                image = downloadedImage,
                                                remoteImagePath = extractImagePath(
                                                    fullImageUrl = downloadedImage.toString()
                                                ),
                                            )
                                        )
                                    }
                                )
                            }
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

    fun updateDateTime(zonedDateTime: ZonedDateTime) {
        uiState.value =
            uiState.value.copy(updatedDateTime = zonedDateTime.toInstant().toRealmInstant())
    }

    fun upsertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.selectedDiaryId != null) {
                updateDiary(diary, onSuccess, onError)
            } else {
                insertDiary(diary, onSuccess, onError)
            }
        }
    }

    private suspend fun insertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = MongoDB.insertDiary(diary.apply {
            if (uiState.value.updatedDateTime != null) {
                date = uiState.value.updatedDateTime!!
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error<*>) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    private suspend fun updateDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = MongoDB.updateDiary(diary.apply {
            _id = ObjectId.invoke(uiState.value.selectedDiaryId!!)
            date = if (uiState.value.updatedDateTime != null) {
                uiState.value.updatedDateTime!!
            } else {
                uiState.value.selectedDiary!!.date
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error<*>) {
            withContext(Dispatchers.Main) {
                onError(result.error.message.toString())
            }
        }
    }

    fun deleteDiary(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (uiState.value.selectedDiaryId != null) {
            val diaryId = ObjectId.invoke(uiState.value.selectedDiaryId!!)
            viewModelScope.launch(Dispatchers.IO) {
                val result = MongoDB.deleteDiary(diaryId)
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

    fun addImage(image: Uri, imageType: String) {
        val remoteImagePath =
            "images/${FirebaseAuth.getInstance().currentUser?.uid}/${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"
        galleryState.addImage(GalleryImage(image = image, remoteImagePath = remoteImagePath))
    }

    fun uploadImagesToFirebase() {
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { galleryImage ->
            val imagePath = storage.child(galleryImage.remoteImagePath)
            imagePath.putFile(galleryImage.image)
                .addOnProgressListener {
                    val sessionUri = it.uploadSessionUri
                    if (sessionUri != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = galleryImage.remoteImagePath,
                                    imageUri = galleryImage.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun extractImagePath(fullImageUrl: String): String {
        val chunks = fullImageUrl.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"
    }
}

data class UiState(
    val selectedDiaryId: String? = null,
    val selectedDiary: Diary? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime: RealmInstant? = null
)