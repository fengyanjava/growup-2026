package com.example.composefirst.first.tab_switching.article

import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ArticleRepositoryTest {
    private lateinit var server: HttpServer
    private lateinit var repository: ArticleRepository
    private var status = 200
    private var response = "[]"

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/posts") { exchange ->
            val bytes = response.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        repository = ArticleRepository("http://127.0.0.1:${server.address.port}/posts")
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun parsesArticlesAndIgnoresAdditionalFields() = runBlocking {
        response = """
            [{"id":1,"userId":2,"title":"文章标题","body":"第一行\n第二行","extra":true}]
        """.trimIndent()

        assertEquals(
            listOf(Article(1, 2, "文章标题", "第一行\n第二行")),
            repository.getArticles(),
        )
    }

    @Test
    fun handlesEmptyList() = runBlocking {
        assertEquals(emptyList<Article>(), repository.getArticles())
    }

    @Test(expected = IOException::class)
    fun rejectsHttpFailure(): Unit = runBlocking {
        status = 503
        repository.getArticles()
        Unit
    }

    @Test(expected = SerializationException::class)
    fun rejectsInvalidResponse(): Unit = runBlocking {
        response = "not json"
        repository.getArticles()
        Unit
    }
}
