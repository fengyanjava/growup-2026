package com.example.android_basic.feature.mvvm.data.repository

import com.example.android_basic.core.network.NetworkError
import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.feature.mvvm.data.model.Post
import com.example.android_basic.feature.mvvm.data.remote.PostApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class NetworkPostRepositoryTest {
    private val posts = listOf(Post(userId = 1, id = 1, title = "Title", body = "Body"))

    @Test
    fun `successful request returns posts`() = runTest {
        val repository = repository { posts }

        assertEquals(NetworkResult.Success(posts), repository.getPosts())
    }

    @Test
    fun `API failure uses shared request handling`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertEquals(NetworkResult.Failure(NetworkError.NETWORK), repository.getPosts())
    }

    private fun repository(block: suspend () -> List<Post>): NetworkPostRepository =
        NetworkPostRepository(object : PostApi {
            override suspend fun getPosts(): List<Post> = block()
        })
}
