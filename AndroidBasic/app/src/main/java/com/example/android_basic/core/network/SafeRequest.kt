package com.example.android_basic.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** 统一转换请求异常，继承调用方的执行环境，不额外启动协程或切换线程。 */
suspend fun <T> safeRequest(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (cancelled: CancellationException) {
        // 取消交回协程框架处理，不能转换成页面请求失败。
        throw cancelled
    } catch (exception: Exception) {
        val error = when (exception) {
            is IOException -> NetworkError.NETWORK
            is HttpException -> NetworkError.HTTP
            else -> NetworkError.UNKNOWN
        }
        NetworkResult.Failure(error)
    }
}
