package com.example.composefirst.first.tab_switching.article

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ArticleRepository(
    private val endpoint: String = "https://jsonplaceholder.typicode.com/posts",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getArticles(): List<Article> = withContext(Dispatchers.IO) {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Article request failed: HTTP $status")
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            ensureActive()
            json.decodeFromString<List<Article>>(response)
        } finally {
            connection.disconnect()
        }
    }
}
