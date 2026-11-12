package com.example.android_basic.feature.coroutine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CoroutineFirstViewModel : ViewModel() {

    fun go() {
        viewModelScope.launch {
            println("start")
            delay(2_000)
            println("end")
        }
    }

    fun readCache(file: File) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                file.readText()
            }
            // main thread, you can update Views
            println(text)
        }
    }

    fun loadMoreData() {
        viewModelScope.launch {
            val detailTask = async {
                loadDetail()
            }
            val someDataTask = async {
                loadSomeData()
            }
            val detailResp = detailTask.await()
            val someDataResp = someDataTask.await()
            println("all done $detailResp $someDataResp")
        }
    }

    private suspend fun loadDetail(): String {
        delay(2000)
        return "detail"
    }

    private suspend fun loadSomeData(): String {
        delay(3000)
        return "data"
    }

    fun loadPageData() {
        viewModelScope.launch {
            val data = loadPageDataWrap()
            println("the data is $data")
        }
    }

    private suspend fun loadPageDataWrap() = coroutineScope {
        val firstTask = async {
            loadDetail()
        }
        val secondTask = async {
            loadSomeData()
        }
        listOf(firstTask.await(), secondTask.await())
    }
}