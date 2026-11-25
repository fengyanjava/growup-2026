package com.example.composefirst.first.tab_switching.article

import com.example.composefirst.network.ApiClient
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class ArticleApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: ArticleApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ApiClient.createRetrofit(server.url("/").toString())
            .create(ArticleApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun parsesArticlesAndIgnoresAdditionalFields() = runBlocking {
        enqueueJson("""
            [{"id":1,"userId":2,"title":"文章标题","body":"第一行\n第二行","extra":true}]
        """.trimIndent())

        assertEquals(
            listOf(Article(1, 2, "文章标题", "第一行\n第二行")),
            api.getArticles(offset = 0, limit = 20),
        )
        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("GET", request!!.method)
        assertEquals("/posts", request.requestUrl!!.encodedPath)
        assertEquals("0", request.requestUrl!!.queryParameter("_start"))
        assertEquals("20", request.requestUrl!!.queryParameter("_limit"))
        assertEquals("application/json", request.getHeader("Accept"))
    }

    @Test
    fun handlesEmptyList() = runBlocking {
        enqueueJson("[]")
        assertEquals(emptyList<Article>(), api.getArticles(offset = 0, limit = 20))
    }

    @Test(expected = HttpException::class)
    fun rejectsHttpFailure(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            api.getArticles(offset = 0, limit = 20)
        } catch (error: HttpException) {
            assertEquals(503, error.code())
            throw error
        }
    }

    @Test(expected = SerializationException::class)
    fun rejectsInvalidResponse(): Unit = runBlocking {
        enqueueJson("not json")
        api.getArticles(offset = 0, limit = 20)
        Unit
    }

    @Test
    fun cancellingCoroutineCancelsHttpCall() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val cancelled = CountDownLatch(1)
        val client = OkHttpClient.Builder()
            .eventListener(object : EventListener() {
                override fun callFailed(call: Call, ioe: IOException) {
                    if (call.isCanceled()) cancelled.countDown()
                }
            })
            .build()
        val api = ApiClient.createRetrofit(server.url("/").toString())
            .newBuilder()
            .client(client)
            .build()
            .create(ArticleApi::class.java)
        val request = async(start = CoroutineStart.UNDISPATCHED) {
            api.getArticles(offset = 0, limit = 20)
        }
        try {
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            request.cancelAndJoin()
            assertTrue("Underlying HTTP call should be cancelled", cancelled.await(5, TimeUnit.SECONDS))
        } finally {
            request.cancelAndJoin()
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    private fun enqueueJson(body: String) {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body),
        )
    }
}
