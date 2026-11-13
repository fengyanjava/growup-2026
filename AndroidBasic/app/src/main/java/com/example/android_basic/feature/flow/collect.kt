package com.example.android_basic.feature.flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

fun collectLatest() {
    runBlocking {
        flow {
            emit(1)
            delay(100)
            emit(2)
        }.collectLatest {
            print(it)
            delay(500)
            print("ok")
        }
    }
}