package com.example.wordlearn.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlearn.data.model.Word
import com.example.wordlearn.data.model.WordStatus
import com.example.wordlearn.data.repository.VocabularyRepository
import com.example.wordlearn.data.LearningPlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlin.random.Random

private const val TAG = "ReviewViewModel"
class ReviewViewModel(
    private val vocabularyRepository: VocabularyRepository,
    private val learningPlanRepository: LearningPlanRepository
) : ViewModel() {

    // 当前复习的单词
    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord = _currentWord.asStateFlow()

    // 选项列表
    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options = _options.asStateFlow()

    // 是否已回答
    private val _isAnswered = MutableStateFlow(false)
    val isAnswered = _isAnswered.asStateFlow()

    // 选择的答案
    private val _selectedAnswer = MutableStateFlow<String>("")
    val selectedAnswer = _selectedAnswer.asStateFlow()

    // 进度
    private val _progress = MutableStateFlow(0)
    val progress = _progress.asStateFlow()

    // 总单词数
    private val _totalWords = MutableStateFlow(0)
    val totalWords = _totalWords.asStateFlow()

    // 当前是否为英译中模式（否则为中译英）
    private val _isEnglishToChinese = MutableStateFlow(true)
    val isEnglishToChinese = _isEnglishToChinese.asStateFlow()

    // 显示的问题（可能是英文或中文）
    private val _question = MutableStateFlow<String>("")
    val question = _question.asStateFlow()

    // 待复习单词列表
    private var reviewWords = mutableListOf<Word>()
    
    // 缓存所有单词列表，用于生成选项
    private var allWordsCache: List<Word>? = null
    
    // 添加复习完成状态
    private val _isReviewComplete = MutableStateFlow(false)
    val isReviewComplete = _isReviewComplete.asStateFlow()

    // 添加提示消息
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    // 添加发音功能
    private val _shouldPlayPronunciation = MutableStateFlow<String?>(null)
    val shouldPlayPronunciation = _shouldPlayPronunciation.asStateFlow()

    init {
        Log.d(TAG, "初始化 ReviewViewModel")
        loadReviewWords()
    }

    private fun loadReviewWords() {
        Log.d(TAG, "开始加载需要复习的单词")
        viewModelScope.launch {
            // 获取学习计划中设置的每日复习数量
            var reviewLimit = 10 // 默认值
            
            // 从 Flow 中获取学习计划
            learningPlanRepository.learningPlan.collect { plan ->
                reviewLimit = plan?.dailyGoal?.reviewWordsCount ?: 10
                Log.d(TAG, "学习计划设置的每日复习数量: $reviewLimit")
                
                // 获取需要复习的单词
                val words = vocabularyRepository.getWordsForReview(reviewLimit)
                Log.d(TAG, "从数据库获取到 ${words.size} 个需要复习的单词")
                
                if (words.isEmpty()) {
                    Log.d(TAG, "没有找到需要复习的单词，请检查：1. 是否已加载词书 2. 单词的 nextReviewDate 是否正确设置")
                } else {
                    Log.d(TAG, "第一个单词: ${words.first().word}, 状态: ${words.first().status}, 下次复习日期: ${words.first().nextReviewDate}")
                }
                
                reviewWords = words.toMutableList()
                _totalWords.value = reviewWords.size
                
                if (reviewWords.isNotEmpty()) {
                    Log.d(TAG, "找到需要复习的单词，开始加载第一个单词")
                    loadNextWord()
                }
                
                // 只收集一次学习计划
                return@collect
            }
        }
    }

    private suspend fun generateOptions(currentWord: Word): List<String> {
        Log.d(TAG, "开始为单词 ${currentWord.word} 生成选项")
        
        // 使用缓存的单词列表
        if (allWordsCache == null) {
            allWordsCache = vocabularyRepository.getAllWords()
        }
        
        // 过滤掉当前单词
        val otherWords = allWordsCache!!.filter { it.word != currentWord.word }
        
        // 根据当前模式选择干扰项生成策略
        val isEnglishMode = _isEnglishToChinese.value
        
        // 根据以下规则选择干扰项：
        // 英译中模式：选择含有相似中文含义的词
        // 中译英模式：选择相似长度的英文单词
        val distractors = otherWords.filter { word ->
            if (isEnglishMode) {
                // 英译中模式：查找中文释义有重叠的词
                word.meaning.any { char -> currentWord.meaning.contains(char) }
            } else {
                // 中译英模式：查找长度相近的词，不再计算编辑距离以提高性能
                val lengthDiff = (word.word.length - currentWord.word.length).absoluteValue
                lengthDiff <= 2
            }
        }
        
        // 从干扰项中随机选择3个
        val selectedDistractors = if (distractors.size >= 3) {
            distractors.shuffled().take(3)
        } else {
            // 如果没有足够的相关干扰项，随机选择其他单词
            val remainingCount = 3 - distractors.size
            val randomWords = otherWords
                .filter { it !in distractors }
                .shuffled()
                .take(remainingCount)
            distractors + randomWords
        }
        
        // 根据模式返回对应的选项
        val correctAnswer = if (isEnglishMode) currentWord.meaning else currentWord.word
        val wrongAnswers = selectedDistractors.map { 
            if (isEnglishMode) it.meaning else it.word 
        }
        
        // 返回打乱顺序的选项列表
        return (wrongAnswers + correctAnswer).shuffled()
    }

    private fun loadNextWord() {
        val currentWord = reviewWords.firstOrNull()
        Log.d(TAG, "加载下一个单词: ${currentWord?.word ?: "没有更多单词"}")
        _currentWord.value = currentWord
        
        if (currentWord != null) {
            // 随机决定是英译中还是中译英模式
            _isEnglishToChinese.value = Random.nextBoolean()
            
            // 设置问题文本
            _question.value = if (_isEnglishToChinese.value) {
                // 如果是英译中模式，自动播放单词发音
                _shouldPlayPronunciation.value = currentWord.word
                currentWord.word
            } else {
                currentWord.meaning
            }
            
            viewModelScope.launch {
                // 生成选项
                Log.d(TAG, "开始生成选项，模式: ${if (_isEnglishToChinese.value) "英译中" else "中译英"}")
                _options.value = generateOptions(currentWord)
                Log.d(TAG, "生成的选项: ${_options.value}")
            }
        }
        
        // 重置答题状态
        _isAnswered.value = false
        _selectedAnswer.value = ""
    }

    private fun calculateNextReviewDate(word: Word, isCorrect: Boolean): LocalDate {
        // 获取当前日期
        val today = LocalDate.now()
        
        // 根据复习正确与否调整间隔
        val baseInterval = when {
            isCorrect -> when (word.reviewCount) {
                0 -> 1  // 第一次复习正确，1天后复习
                1 -> 3  // 第二次复习正确，3天后复习
                2 -> 7  // 第三次复习正确，7天后复习
                3 -> 14 // 第四次复习正确，14天后复习
                else -> 30 // 第五次及以上复习正确，30天后复习
            }
            else -> 1  // 复习错误，第二天重新复习
        }
        
        return today.plusDays(baseInterval.toLong())
    }

    fun checkAnswer(answer: String) {
        Log.d(TAG, "检查答案: $answer")
        _selectedAnswer.value = answer
        _isAnswered.value = true
        
        val correctAnswer = if (_isEnglishToChinese.value) {
            currentWord.value?.meaning
        } else {
            // 如果是中译英模式，在显示答案时播放正确单词的发音
            currentWord.value?.word?.also {
                _shouldPlayPronunciation.value = it
            }
        }
        
        val isCorrect = answer == correctAnswer
        Log.d(TAG, "答案 ${if (isCorrect) "正确" else "错误"}")
        
        viewModelScope.launch {
            currentWord.value?.let { word ->
                // 计算新的复习次数
                val newReviewCount = if (isCorrect) word.reviewCount + 1 else 0
                
                // 计算下次复习日期
                val nextReviewDate = calculateNextReviewDate(word, isCorrect)
                
                // 确定单词状态
                val newStatus = when {
                    isCorrect && newReviewCount >= 5 -> WordStatus.KNOWN
                    else -> WordStatus.NEEDS_REVIEW
                }
                
                // 更新单词状态
                vocabularyRepository.updateWordStatus(
                    word.id,
                    newStatus,
                    LocalDate.now(),
                    nextReviewDate,
                    newReviewCount
                )
                
                // 只有答对时才自动进入下一题
                if (isCorrect) {
                    nextWord()
                }
                // 答错时等待用户点击"下一个"按钮
            }
        }
    }

    fun nextWord() {
        if (reviewWords.isNotEmpty()) {
            Log.d(TAG, "移除当前单词，进入下一个")
            // 移除当前单词
            reviewWords.removeFirstOrNull()
            _progress.value++
            
            if (reviewWords.isNotEmpty()) {
                loadNextWord()
            } else {
                Log.d(TAG, "已完成所有单词的复习")
                _isReviewComplete.value = true
                _message.value = "今天的复习任务已完成！"
                _currentWord.value = null
            }
        }
    }
} 