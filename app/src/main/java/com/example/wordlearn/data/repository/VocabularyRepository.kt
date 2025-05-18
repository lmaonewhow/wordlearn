package com.example.wordlearn.data.repository

import android.content.Context
import com.example.wordlearn.data.model.BookType
import com.example.wordlearn.data.model.VocabularyBook
import com.example.wordlearn.data.model.Word
import java.io.BufferedReader
import java.io.InputStreamReader

class VocabularyRepository(private val context: Context) {
    
    // 获取所有可用的词汇书
    fun getAvailableBooks(): List<VocabularyBook> {
        return listOf(
            VocabularyBook(
                id = "book1",
                name = "20天背完四级核心词汇",
                filePath = "m-word/20天背完四级核心词汇.csv",
                totalWords = 1200,
                type = BookType.CSV
            ),
            VocabularyBook(
                id = "book2",
                name = "20天背完高考核心词汇",
                filePath = "m-word/20天背完高考核心词汇.csv",
                totalWords = 3500,
                type = BookType.CSV
            ),
            VocabularyBook(
                id = "book3",
                name = "24天突破高考大纲词汇3500主词",
                filePath = "m-word/24天突破高考大纲词汇3500主词.csv",
                totalWords = 3500,
                type = BookType.CSV
            )
        )
    }

    // 从CSV文件读取单词
    suspend fun loadWordsFromCsv(filePath: String): List<Word> {
        return try {
            val inputStream = context.assets.open(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val words = mutableListOf<Word>()
            
            // 跳过标题行
            reader.readLine()
            
            // 读取每一行
            reader.useLines { lines ->
                lines.forEach { line ->
                    try {
                        val columns = line.split(",").map { it.trim() }
                        if (columns.size >= 2) {
                            words.add(
                                Word(
                                    word = columns[0],
                                    meaning = columns[1],
                                    ukPhonetic = columns.getOrNull(2) ?: "",
                                    usPhonetic = columns.getOrNull(3) ?: "",
                                    example = columns.getOrNull(4) ?: ""
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 跳过错误的行，继续处理下一行
                    }
                }
            }
            
            words
            
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 从TXT文件读取单词
    suspend fun loadWordsFromTxt(filePath: String): List<Word> {
        return try {
            val inputStream = context.assets.open(filePath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val words = mutableListOf<Word>()
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { 
                    val parts = it.split(" - ")
                    if (parts.size >= 2) {
                        words.add(
                            Word(
                                word = parts[0].trim(),
                                meaning = parts[1].trim()
                            )
                        )
                    }
                }
            }
            
            reader.close()
            words
            
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
} 