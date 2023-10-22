package com.hypheno.diaryapp.data.repository

import com.hypheno.diaryapp.model.Diary
import com.hypheno.diaryapp.util.Constants.APP_ID
import com.hypheno.diaryapp.model.RequestState
import com.hypheno.diaryapp.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.mongodb.kbson.ObjectId
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object MongoDB : MongoRepository {

    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm

    init {
        configureRealm()
    }

    override fun configureRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(user, setOf(Diary::class))
                .initialSubscriptions { sub ->
                    add(
                        query = sub.query<Diary>("ownerId == $0", user.id),
                        name = "User's Diaries"
                    )
                }
                .log(LogLevel.ALL)
                .build()

            realm = Realm.open(config)
        }
    }

    override fun getAllDiaries(): Flow<Diaries> {
        return if (user != null) {
            try {
                realm.query<Diary>(query = "ownerId == $0", user.id)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                        RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }
            } catch (e: Exception) {
                flow { emit(RequestState.Error<Diaries>(e)) }
            }
        } else {
            flow { emit(RequestState.Error<Diaries>(UserNotAuthenticatedException())) }
        }
    }

    override fun getFilteredDiaries(zonedDateTime: ZonedDateTime): Flow<Diaries> {
        return if (user != null) {
            try {
                realm.query<Diary>(
                    "ownerId == $0 AND date < $1 AND date > $2",
                    user.id,
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate().plusDays(1),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                    RealmInstant.from(
                        LocalDateTime.of(
                            zonedDateTime.toLocalDate(),
                            LocalTime.MIDNIGHT
                        ).toEpochSecond(zonedDateTime.offset), 0
                    ),
                ).asFlow().map { result ->
                    RequestState.Success(
                        data = result.list.groupBy {
                            it.date.toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                    )
                }
            } catch (e: Exception) {
                flow { emit(RequestState.Error<Diaries>(e)) }
            }
        } else {
            flow { emit(RequestState.Error<Diaries>(UserNotAuthenticatedException())) }
        }
    }

    override fun getSelectedDiary(diaryId: ObjectId): Flow<RequestState<Diary>> {
        return if (user != null) {
            try {
                realm.query<Diary>(query = "_id == $0", diaryId).asFlow()
                    .map { RequestState.Success(data = it.list.first()) }
            } catch (e: Exception) {
                flow { emit(RequestState.Error<Diary>(e)) }
            }
        } else {
            flow { emit(RequestState.Error<Diary>(UserNotAuthenticatedException())) }
        }
    }

    override suspend fun insertDiary(diary: Diary): RequestState<Diary> {
        return if (user != null) {
            realm.write {
                try {
                    val addedDiary = copyToRealm(diary.apply { ownerId = user.id })
                    RequestState.Success(addedDiary)
                } catch (e: Exception) {
                    RequestState.Error<Diary>(e)
                }
            }
        } else {
            RequestState.Error<Diary>(UserNotAuthenticatedException())
        }
    }

    override suspend fun updateDiary(diary: Diary): RequestState<Diary> {
        return if (user != null) {
            realm.write {
                val queriedDiary = query<Diary>(query = "_id ==  $0", diary._id).first().find()
                if (queriedDiary != null) {
                    queriedDiary.title = diary.title
                    queriedDiary.description = diary.description
                    queriedDiary.mood = diary.mood
                    queriedDiary.images = diary.images
                    queriedDiary.date = diary.date
                    RequestState.Success(data = queriedDiary)
                } else {
                    RequestState.Error<Diary>(Exception("Diary does not exist"))
                }
            }
        } else {
            RequestState.Error<Diary>(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteDiary(id: ObjectId): RequestState<Boolean> {
        return if (user != null) {
            realm.write {
                try {
                    val diary = query<Diary>(query = "_id == $0 AND ownerId == $1", id, user.id)
                        .first().find()
                    if (diary != null) {
                        delete(diary)
                        RequestState.Success(true)
                    } else {
                        RequestState.Error<Boolean>(Exception("Diary does not exist"))
                    }
                } catch (e: Exception) {
                    RequestState.Error<Boolean>(e)
                }
            }
        } else {
            RequestState.Error<Diary>(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteAllDiaries(): RequestState<Boolean> {
        return if (user != null) {
            realm.write {
                val diaries = this.query<Diary>("ownerId == $0", user.id).find()
                try {
                    delete(diaries)
                    RequestState.Success(true)
                } catch (e: Exception) {
                    RequestState.Error<Boolean>(e)
                }
            }
        } else {
            RequestState.Error<Diary>(UserNotAuthenticatedException())
        }
    }
}

private class UserNotAuthenticatedException : Exception("User not Logged in.")