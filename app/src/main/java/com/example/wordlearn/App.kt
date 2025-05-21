package com.example.wordlearn

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.wordlearn.data.AppDatabase
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.ui.viewmodel.HomeViewModel
import com.example.wordlearn.ui.viewmodel.LearningViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "WordLearnApp"

class App : Application() {
    // 使用 lazy 延迟初始化
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    lateinit var vocabularyRepository: VocabularyRepository
        private set
    
    // 应用级别的协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 保存HomeViewModel实例，用于跨组件通信
    var homeViewModel: HomeViewModel? = null
    
    lateinit var learningViewModelFactory: ViewModelProvider.Factory
        private set
    
    // 存储初始化的ViewModel实例
    private var learningViewModel: LearningViewModel? = null
    
    // 获取共享的ViewModel实例
    fun getLearningViewModel(): LearningViewModel {
        if (learningViewModel == null) {
            learningViewModel = LearningViewModel(this)
            // 初始化ViewModel
            learningViewModel?.initialize(this)
        }
        return learningViewModel as LearningViewModel
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化依赖
        vocabularyRepository = VocabularyRepository(
            context = this,
            vocabularyDao = database.vocabularyDao()
        )
        
        // 初始化视图模型工厂
        learningViewModelFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return LearningViewModel(this@App) as T
            }
        }
        
        // 预加载最近使用的词书
        preloadRecentWordbook()
    }
    
    // 预加载最近使用的词书
    private fun preloadRecentWordbook() {
        applicationScope.launch {
            try {
                Log.d(TAG, "【缓存】正在预加载最近使用的词书...")
                
                // 获取最近使用的词书名称
                val prefs = getSharedPreferences("learning_progress", Context.MODE_PRIVATE)
                val savedBookName = prefs.getString("current_book", null)
                
                if (savedBookName != null) {
                    // 找到对应的词书
                    val books = vocabularyRepository.getAvailableBooks()
                    val recentBook = books.find { it.name == savedBookName }
                    
                    if (recentBook != null) {
                        Log.d(TAG, "【缓存】找到最近使用的词书: ${recentBook.name}")
                        
                        // 预先初始化ViewModel并预加载词书
                        val viewModel = getLearningViewModel()
                        
                        // 异步预加载词书内容
                        Log.d(TAG, "【缓存】正在后台预加载词书内容...")
                    } else {
                        Log.d(TAG, "【缓存】未找到最近使用的词书: $savedBookName")
                    }
                } else {
                    Log.d(TAG, "【缓存】没有最近使用的词书记录")
                }
            } catch (e: Exception) {
                Log.e(TAG, "【缓存】预加载词书时出错", e)
            }
        }
    }
} 