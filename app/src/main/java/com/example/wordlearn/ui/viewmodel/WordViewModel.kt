package com.example.wordlearn.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class WordViewModel : ViewModel() {
    var currentWord = mutableStateOf("Default")
}