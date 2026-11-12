package com.example.android_basic.feature.`in`

interface Consumer<in T> {
    fun consume(value: T)
}

val anyConsumer = object : Consumer<Any> {
    override fun consume(value: Any) {
        println(value)
    }
}

val stringConsumer: Consumer<String> = anyConsumer

fun test() {
    stringConsumer.consume("")
}