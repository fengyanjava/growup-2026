package com.example.android_basic.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SafeRequestTest {
    @Test
    fun `successful request returns data`() = runTest {
        val result = safeRequest { "response" }

        assertEquals(NetworkResult.Success("response"), result)
    }

    @Test
    fun `successful empty response remains a success`() = runTest {
        val result = safeRequest { emptyList<String>() }

        assertEquals(NetworkResult.Success(emptyList<String>()), result)
    }

    @Test
    fun `IO exception becomes network failure`() = runTest {
        val result = safeRequest<String> { throw IOException("offline") }

        assertEquals(NetworkResult.Failure(NetworkError.NETWORK), result)
    }

    @Test
    fun `HTTP errors become HTTP failures`() = runTest {
        for (code in listOf(404, 503)) {
            val result = safeRequest<String> {
                throw HttpException(Response.error<String>(code, "error".toResponseBody()))
            }

            assertEquals(NetworkResult.Failure(NetworkError.HTTP), result)
        }
    }

    @Test
    fun `unexpected exception becomes unknown failure`() = runTest {
        val result = safeRequest<String> { throw IllegalStateException("bad data") }

        assertEquals(NetworkResult.Failure(NetworkError.UNKNOWN), result)
    }

    @Test
    fun `cancellation exception is rethrown unchanged`() = runTest {
        val cancelled = CancellationException("cancelled")

        try {
            safeRequest<String> { throw cancelled }
            fail("Cancellation must not become a failure result")
        } catch (actual: CancellationException) {
            assertSame(cancelled, actual)
        }
    }

    @Test
    fun `cancelling caller cancels request without returning failure`() = runTest {
        var blockCancelled = false
        var result: NetworkResult<String>? = null
        val request = launch {
            result = safeRequest {
                try {
                    awaitCancellation()
                } finally {
                    blockCancelled = true
                }
            }
        }
        runCurrent()

        request.cancelAndJoin()

        assertTrue(request.isCancelled)
        assertTrue(blockCancelled)
        assertNull(result)
    }
}
