package com.example.android_basic.feature.mvvmflow.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.android_basic.core.network.NetworkError
import com.example.android_basic.core.network.NetworkResult
import com.example.android_basic.feature.mvvmflow.data.model.Post
import com.example.android_basic.feature.mvvmflow.data.repository.PostRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
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
    fun `initial request exposes loading then repository result`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository { NetworkResult.Success(posts) })
        assertEquals(PostUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
    }

    @Test
    fun `empty response exposes empty state`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository { NetworkResult.Success(emptyList()) })
        advanceUntilIdle()
        assertEquals(PostUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `network failure can be retried successfully`() = runTest(dispatcher) {
        val repository = FakeRepository { NetworkResult.Failure(NetworkError.NETWORK) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals(PostUiState.Error(NetworkError.NETWORK), viewModel.uiState.value)

        repository.result = { NetworkResult.Success(posts) }
        viewModel.loadPosts()
        runCurrent()

        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `HTTP error exposes shared HTTP failure`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository {
            NetworkResult.Failure(NetworkError.HTTP)
        })
        advanceUntilIdle()
        assertEquals(PostUiState.Error(NetworkError.HTTP), viewModel.uiState.value)
    }

    @Test
    fun `unexpected failure exposes generic error`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeRepository {
            NetworkResult.Failure(NetworkError.UNKNOWN)
        })
        advanceUntilIdle()
        assertEquals(PostUiState.Error(NetworkError.UNKNOWN), viewModel.uiState.value)
    }

    @Test
    fun `repeated refresh during request does not issue extra requests`() = runTest(dispatcher) {
        val response = CompletableDeferred<List<Post>>()
        val repository = FakeRepository { NetworkResult.Success(response.await()) }
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
        val repository = FakeRepository { NetworkResult.Success(posts) }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val updated = posts.map { it.copy(title = "Updated title") }
        repository.result = { NetworkResult.Success(updated) }
        viewModel.loadPosts()
        advanceUntilIdle()

        assertEquals(2, repository.calls)
        assertEquals(PostUiState.Success(updated), viewModel.uiState.value)
    }

    @Test
    fun `retained ViewModelStore reuses state without another request`() = runTest(dispatcher) {
        val repository = FakeRepository { NetworkResult.Success(posts) }
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

    @Test
    fun `stopping collection keeps request alive and new collector receives latest state`() = runTest(dispatcher) {
        val response = CompletableDeferred<List<Post>>()
        val repository = FakeRepository { NetworkResult.Success(response.await()) }
        val viewModel = createViewModel(repository)
        val firstStates = mutableListOf<PostUiState>()
        val firstCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { firstStates.add(it) }
        }
        runCurrent()
        assertEquals(listOf(PostUiState.Loading), firstStates)

        // 对应页面退到后台：取消收集，但 ViewModel 中的请求继续执行。
        firstCollector.cancel()
        response.complete(posts)
        advanceUntilIdle()
        assertEquals(PostUiState.Success(posts), viewModel.uiState.value)
        assertEquals(listOf(PostUiState.Loading), firstStates)

        // 对应重新进入页面：立即收到最新状态，不触发新请求。
        val resumedStates = mutableListOf<PostUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { resumedStates.add(it) }
        }
        assertEquals(listOf(PostUiState.Success(posts)), resumedStates)
        assertEquals(1, repository.calls)
    }

    private fun createViewModel(repository: PostRepository): PostViewModel =
        ViewModelProvider(store, PostViewModel.Factory(repository))[PostViewModel::class.java]

    private class FakeRepository(
        var result: suspend () -> NetworkResult<List<Post>>
    ) : PostRepository {
        var calls = 0
            private set

        override suspend fun getPosts(): NetworkResult<List<Post>> {
            calls++
            return result()
        }
    }
}
