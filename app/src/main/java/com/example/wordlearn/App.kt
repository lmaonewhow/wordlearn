package com.example.wordlearn

import android.app.Application
import com.example.wordlearn.data.AppDatabase
import com.example.wordlearn.data.repository.VocabularyRepository

class App : Application() {
    // 使用 lazy 延迟初始化
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val vocabularyRepository: VocabularyRepository by lazy { 
        VocabularyRepository(
            context = this,
            vocabularyDao = database.vocabularyDao()
        )
    }
} 