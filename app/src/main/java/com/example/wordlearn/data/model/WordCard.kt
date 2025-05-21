package com.example.wordlearn.data.model

// 单词卡片数据类
data class WordCard(
    val id: Long = 0,
    val word: String,
    val ukPhonetic: String,
    val usPhonetic: String,
    val example: String,
    val phonetic: String = ukPhonetic,  // 默认使用英式音标
    val definition: String = "",  // 中文释义
    val isFavorite: Boolean = false,  // 是否收藏
    val errorCount: Int = 0  // 错误次数
) 