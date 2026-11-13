package com.example.android_basic.feature.mvvmflow.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_basic.R
import com.example.android_basic.core.network.NetworkError
import com.example.android_basic.core.ui.applySystemBarInsets
import com.example.android_basic.databinding.ActivityMvvmPostsBinding
import com.example.android_basic.feature.mvvmflow.MvvmFlowDependencies
import kotlinx.coroutines.launch

/** View 只处理交互和渲染；不直接发请求，也不持有可修改的业务状态。 */
class PostListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMvvmPostsBinding
    private val postAdapter = PostAdapter()
    private val viewModel: PostViewModel by viewModels {
        PostViewModel.Factory(MvvmFlowDependencies.postRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMvvmPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        // 新示例复用原 XML，这里单独设置标题，方便区分 LiveData / Flow 页面。
        binding.toolbar.setTitle(R.string.mvvm_flow_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.postList.layoutManager = LinearLayoutManager(this)
        binding.postList.adapter = postAdapter
        binding.refreshButton.setOnClickListener { viewModel.loadPosts() }
        binding.retryButton.setOnClickListener { viewModel.loadPosts() }

        // 与 LiveData 版不同：原来用 uiState.observe(this, ::render)，这里改用协程中的 collect。
        // lifecycleScope 随 Activity 销毁而取消；页面停止时的收集取消由下面的 repeatOnLifecycle 处理。
        lifecycleScope.launch {
            // 与 LiveData.observe 不同：collect 本身不会自动根据页面生命周期停止收集。
            // 达到 STARTED 时启动内部收集协程；低于 STARTED 时取消它，恢复后重新启动。
            // 只影响收集，ViewModel 在 viewModelScope 中发起的请求不会因此取消或重新执行。
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 每次开始收集先收到 StateFlow 的当前值，再接收后续变化；慢速收集可能跳过中间状态。
                viewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
    }

    private fun render(state: PostUiState) = with(binding) {
        progressIndicator.isVisible = state is PostUiState.Loading
        postList.isVisible = state is PostUiState.Success
        messageGroup.isVisible = state is PostUiState.Empty || state is PostUiState.Error
        retryButton.isVisible = state is PostUiState.Error
        refreshButton.isEnabled = state !is PostUiState.Loading

        statusText.text = when (state) {
            PostUiState.Loading -> getString(R.string.posts_loading)
            is PostUiState.Success -> getString(R.string.posts_loaded, state.posts.size)
            PostUiState.Empty -> getString(R.string.posts_empty_status)
            is PostUiState.Error -> getString(R.string.posts_error_status)
        }

        when (state) {
            is PostUiState.Success -> postAdapter.submitList(state.posts)
            PostUiState.Empty -> messageText.setText(R.string.posts_empty)
            is PostUiState.Error -> messageText.setText(
                when (state.reason) {
                    NetworkError.NETWORK -> R.string.posts_network_error
                    NetworkError.HTTP -> R.string.posts_server_error
                    NetworkError.UNKNOWN -> R.string.posts_unknown_error
                }
            )
            PostUiState.Loading -> Unit
        }
    }
}
