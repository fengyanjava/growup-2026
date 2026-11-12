# Android 功能演示

运行 `app`，在首页点击「打开 MVVM 示例」。项目沿用 XML + Activity，使用 ViewBinding 获取视图，不使用 Flow。

## 分包

```text
com.example.android_basic
├── MainActivity                  # 功能入口页
├── core                          # 多个功能可复用的基础能力
│   ├── network                   # 公共网络能力
│   │   ├── NetworkClient         # HTTP 客户端、超时和 JSON 转换
│   │   ├── NetworkResult         # 请求结果及 NetworkError 错误分类
│   │   └── SafeRequest           # safeRequest：统一捕获异常、传递取消
│   └── ui/SystemBarInsets         # 系统栏适配
└── feature
    └── mvvm                      # 完整、独立的 MVVM 演示
        ├── MvvmDependencies      # 手动组装 API、Repository
        ├── data
        │   ├── model/Post
        │   ├── remote/PostApi    # 本功能的 Retrofit 接口
        │   └── repository        # 仓库接口及网络实现
        └── ui
            ├── PostListActivity  # View：交互、观察状态、渲染
            ├── PostViewModel     # ViewModel：发起协程、管理状态
            ├── PostUiState       # 加载 / 成功 / 空数据 / 失败
            └── PostAdapter       # 文章列表展示
```

后续功能新增 `feature/功能名` 包，业务自己的模型、数据源和 UI 留在功能内部。只将确实共用的能力放入 `core`。在首页增加按钮，并在 Manifest 注册对应 Activity 即可接入，不必修改 MVVM 示例。

当前只有 `:app` 一个 Gradle 模块，`core.network` 是供各功能复用的公共包。各 feature 依赖公共网络包，公共网络包不依赖具体 feature；以后拆分多个 Gradle 模块时，可将其提取为独立的 `:core:network` library。

## 请求与状态更新

```text
点击打开 / 刷新 / 重试
  → PostViewModel.loadPosts()
  → viewModelScope.launch
  → PostRepository.getPosts()
  → safeRequest { Retrofit suspend API }
  → GET https://jsonplaceholder.typicode.com/posts
  → NetworkResult（成功数据 / 分类错误）
  → LiveData<PostUiState>
  → PostListActivity.render()
```

- Model 是 `Post` 与 Repository/API 数据层；ViewModel 不引用 Activity、View 或 Context。
- `_uiState` 是私有的 `MutableLiveData`，界面只能观察公开的 `LiveData`。
- 第一次创建 ViewModel 自动请求，刷新和重试复用同一入口；正在加载时忽略重复请求。
- Retrofit 的 `suspend` 调用异步执行 HTTP，不阻塞主线程，因此不额外包 `Dispatchers.IO`。以后加入阻塞式文件或数据库操作时，在数据层使用 `withContext(Dispatchers.IO)`。
- Repository 使用 `core.network` 中的 `safeRequest` 统一捕获请求异常并返回 `NetworkResult`，ViewModel 只将结果转换为页面状态。
- `viewModelScope` 在 ViewModel 被清理时自动取消，Retrofit 会取消对应网络请求；`safeRequest` 显式重新抛出 `CancellationException`，不把取消显示为错误。
- `by viewModels` 在旋转等配置变化后复用 ViewModel 和已有状态，不会重新发请求。进程被系统杀死后重新进入会重新请求；此示例没有磁盘缓存。
- HTTP 错误、网络连接/超时、其他异常分别映射为错误状态，UI 提供重试。
- 简单演示共用接口和列表模型，未引入额外的 domain/use case 层或依赖注入框架。

接口使用 [JSONPlaceholder](https://jsonplaceholder.typicode.com/)，是无需 API Key 的在线演示服务。请求真实经过网络，返回的是示例文章，不是本地硬编码列表。需要设备能够访问该域名。

## 演示与验证

1. 正常联网打开示例：显示加载状态，再显示文章列表；点击刷新重新请求。
2. 滚动后旋转屏幕：列表状态和滚动位置保留，正在执行的请求继续。
3. 关闭设备网络后点击刷新：出现错误提示；恢复网络点击「重新请求」。
4. 加载时退出页面：ViewModel 清理并取消请求。
5. `/posts` 正常返回非空列表；空态由单元测试覆盖，无需改动线上接口或加入假数据开关。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`SafeRequestTest` 使用普通数据验证公共请求包装的成功结果、错误分类和取消传播；`NetworkPostRepositoryTest` 使用假 API 验证文章仓库接入公共请求包装；`PostViewModelTest` 使用可替换 Repository 和测试调度器验证状态转换、重试、请求去重、刷新、ViewModel 保留和协程取消。测试均不依赖公网稳定性。
