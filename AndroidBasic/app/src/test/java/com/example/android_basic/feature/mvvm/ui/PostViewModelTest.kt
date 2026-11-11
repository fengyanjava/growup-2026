package com.example.android_basic.feature.mvvm.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.android_basic.feature.mvvm.data.model.Post
import com.example.android_basic.feature.mvvm.data.repository.PostRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
    @get:Rule
    val instantExecutor = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val posts = listOf(Post(userId = 1, id = 1, title = "Title", body = "Body"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial request exposes loading then real repository result`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository { posts })
        assertEquals(PostUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
    }

    @Test
    fun `empty response exposes empty state`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository { emptyList() })
        advanceUntilIdle()
        assertEquals(PostUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `network failure can be retried successfully`() = runTest(dispatcher) {
        val repository = FakeRepository { throw IOException("offline") }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals(PostUiState.Error(PostError.NETWORK), viewModel.uiState.value)

        repository.result = { posts }
        viewModel.loadPosts()
        runCurrent()

        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `HTTP error exposes server failure`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository {
            throw HttpException(Response.error<List<Post>>(503, "unavailable".toResponseBody()))
        })
        advanceUntilIdle()
        assertEquals(PostUiState.Error(PostError.SERVER), viewModel.uiState.value)
    }

    @Test
    fun `unexpected failure exposes generic error`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository { throw IllegalStateException("bad data") })
        advanceUntilIdle()
        assertEquals(PostUiState.Error(PostError.UNKNOWN), viewModel.uiState.value)
    }

    @Test
    fun `repeated refresh during request does not issue extra requests`() = runTest(dispatcher) {
        val response = CompletableDeferred<List<Post>>()
        val repository = FakeRepository { response.await() }
        val viewModel = createViewModel(repository)
        runCurrent()

        repeat(5) { viewModel.loadPosts() }
        runCurrent()
        assertEquals(1, repository.calls)
        assertEquals(PostUiState.Loading, viewModel.uiState.value)

        response.complete(posts)
        advanceUntilIdle()
        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
    }

    @Test
    fun `refresh after success fetches latest posts`() = runTest(dispatcher) {
        val repository = FakeRepository { posts }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val updated = posts.map { it.copy(title = "Updated title") }
        repository.result = { updated }
        viewModel.loadPosts()
        advanceUntilIdle()

        assertEquals(2, repository.calls)
        assertEquals(PostUiState.Success(updated), viewModel.uiState.value)
    }

    @Test
    fun `retained ViewModelStore reuses state without another request`() = runTest(dispatcher) {
        val repository = FakeRepository { posts }
        val original = createViewModel(repository)
        advanceUntilIdle()

        // 对应配置变化后 Activity 使用保留下来的 ViewModelStore 再次获取 ViewModel。
        val retained = createViewModel(repository)
        advanceUntilIdle()

        assertSame(original, retained)
        assertEquals(PostUiState.Success(posts), retained.uiState.value)
        assertEquals(1, repository.calls)
    }

    @Test
    fun `clearing ViewModel cancels request without publishing an error`() = runTest(dispatcher) {
        var cancelled = false
        val viewModel = createViewModel(FakeRepository {
            try {
                awaitCancellation()
            } catch (exception: CancellationException) {
                cancelled = true
                throw exception
            }
        })
        runCurrent()

        store.clear()
        advanceUntilIdle()

        assertTrue(cancelled)
        assertEquals(PostUiState.Loading, viewModel.uiState.value)
    }

    private fun createViewModel(repository: PostRepository): PostViewModel =
        ViewModelProvider(store, PostViewModel.Factory(repository))[PostViewModel::class.java]

    private class FakeRepository(var result: suspend () -> List<Post>) : PostRepository {
        var calls = 0
            private set

        override suspend fun getPosts(): List<Post> {
            calls++
            return result()
        }
    }
}
