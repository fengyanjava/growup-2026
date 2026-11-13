package com.example.android_basic.feature.flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

fun firstFlow() {
    val numbers = flow {
        println("before send")
        emit(1)
        println("send finish")
    }
    runBlocking {
        numbers.collect { value ->
            println("received $value")
            delay(2_000)
            println("handle done")
        }
    }
}

fun flowOn() {
    runBlocking {
        flow {
            println("send on ${Thread.currentThread().name}")
            emit(1)
        }
            .flowOn(Dispatchers.IO)
            .collect {
                println("receive: $it on ${Thread.currentThread().name}")
            }
    }
}

fun flowMap() {
    runBlocking {
        flow {
            emit(1)
            emit(2)
        }
            .map { it * 10 }
            .collect {
                println(it)
            }
    }
}

suspend fun flowOnEach() {
    flow {
        emit(1)
    }
        .onEach {
            println("onEach $it")
        }
        .collect {
            println("collect $it")
        }
}

suspend fun flowCatch() {
    flow {
        emit(1)
        throw IllegalStateException()
    }
        .catch {
            println("catch $it")
            emit(-1)
        }
        .collect {
            println("collect $it")
        }
}