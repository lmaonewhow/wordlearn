package com.example.wordlearn

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.wordlearn.data.AppDatabase
import com.example.wordlearn.data.dao.VocabularyDao
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
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
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
    
    // 获取当日学习目标数量
    fun getLearningPlanGoal(): Int? {
        return try {
            // 通过获取LearningViewModel来间接访问学习目标
            getLearningViewModel().dailyGoal.value
        } catch (e: Exception) {
            Log.e(TAG, "获取学习目标失败", e)
            null
        }
    }
    
    // 获取今日已学习单词数量
    fun getTodayLearnedCount(): Int? {
        return try {
            // 直接从LearningViewModel中获取
            getLearningViewModel().todayLearned.value
        } catch (e: Exception) {
            Log.e(TAG, "获取今日已学习单词数失败", e)
            null
        }
    }
    
    // 公共方法来获取 VocabularyDao 实例
    fun getAppVocabularyDao(): VocabularyDao {
        return database.vocabularyDao()
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "应用程序启动")
        
        // 初始化仓库
        vocabularyRepository = VocabularyRepository(this, database.vocabularyDao())
        
        // 初始化ViewModel工厂
        learningViewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(LearningViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return getLearningViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
} 