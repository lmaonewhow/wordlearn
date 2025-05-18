package com.example.wordlearn.data.model

// 单词卡片数据类
data class WordCard(
    val word: String,
    val ukPhonetic: String,
    val usPhonetic: String,
    val example: String,
    val phonetic: String = ukPhonetic,  // 默认使用英式音标
    val definition: String = ""  // 中文释义
) 