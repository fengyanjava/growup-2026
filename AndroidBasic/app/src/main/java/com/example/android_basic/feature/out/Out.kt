package com.example.android_basic.feature.out

interface Producer<out T> {
    fun produce(): T

//    fun consume(value: T)
}

val stringProducer = object : Producer<String> {
    override fun produce(): String {
        return "Android"
    }
}

val anyProducer: Producer<Any> = stringProducer


class Box<T>(var value: T)

val box = Box(123)
fun test() {
    box.value = 333
}