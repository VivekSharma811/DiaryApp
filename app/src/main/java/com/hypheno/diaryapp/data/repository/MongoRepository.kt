package com.hypheno.diaryapp.data.repository

import com.hypheno.diaryapp.model.Diary
import com.hypheno.diaryapp.util.RequestState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>

interface MongoRepository {

    fun configureRealm()

    fun getAllDiaries(): Flow<Diaries>
}