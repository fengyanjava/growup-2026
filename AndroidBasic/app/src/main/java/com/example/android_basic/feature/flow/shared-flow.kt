package com.example.android_basic.feature.flow

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun sharedFlow() {
    runBlocking {
        val signals = MutableSharedFlow<Int>()
        signals.emit(1)
        signals.collect {
            println(it)
        }
    }
}

fun sharedFlowReplay() {
    runBlocking {
        val signals = MutableSharedFlow<Int>(replay = 1)
        signals.emit(1)
        signals.emit(2)
        signals.collect {
            println(it)
        }
    }
}