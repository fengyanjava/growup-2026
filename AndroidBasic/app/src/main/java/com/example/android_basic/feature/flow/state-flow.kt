package com.example.android_basic.feature.flow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun stateFlow() {
    val state = MutableStateFlow(0)
    state.value = 1
    state.value = 2
    val readState = state.asStateFlow()

    runBlocking {
        launch {
            state.collect {
                println(it)
            }
        }
        launch {
            readState.collect {
                println(it)
            }
        }
    }
}