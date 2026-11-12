package com.example.android_basic.feature.mvvm.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android_basic.R
import com.example.android_basic.core.network.NetworkError
import com.example.android_basic.core.ui.applySystemBarInsets
import com.example.android_basic.databinding.ActivityMvvmPostsBinding
import com.example.android_basic.feature.mvvm.MvvmDependencies

/** View 只处理交互和渲染；不直接发请求，也不持有可修改的业务状态。 */
class PostListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMvvmPostsBinding
    private val postAdapter = PostAdapter()
    private val viewModel: PostViewModel by viewModels {
        PostViewModel.Factory(MvvmDependencies.postRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMvvmPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.postList.layoutManager = LinearLayoutManager(this)
        binding.postList.adapter = postAdapter
        binding.refreshButton.setOnClickListener { viewModel.loadPosts() }
        binding.retryButton.setOnClickListener { viewModel.loadPosts() }

        // observe 与 Activity 生命周期绑定；旋转后接收已有状态，不会重复请求。
        viewModel.uiState.observe(this, ::render)
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
