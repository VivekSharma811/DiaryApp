package com.hypheno.diaryapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hypheno.diaryapp.data.db.entity.ImageToDelete
import com.hypheno.diaryapp.data.db.entity.ImageToUpload

@Database(
    entities = [ImageToUpload::class, ImageToDelete::class],
    version = 2,
    exportSchema = false
)
abstract class ImagesDatabase: RoomDatabase() {
    abstract fun imageToUploadDao(): ImageToUploadDao
    abstract fun imageToDeleteDao(): ImageToDeleteDao
}