package com.example.android_basic.core.network

enum class NetworkError {
    NETWORK,
    HTTP,
    UNKNOWN,
}

/** 请求的业务结果；协程取消不属于失败结果，会继续向外传播。 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}
