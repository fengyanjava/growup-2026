# Riverpod MVVM 用户资料编辑示例

启动后自动模拟请求用户资料，将姓名、性别、年龄回填至输入框；修改后点击保存。
使用 Riverpod 的 `AsyncNotifier`，手动声明 Provider，无需代码生成。

## 运行

在 `flutter_riverpod_example` 目录执行：

```sh
flutter pub get
flutter run
```

## 目录与职责

```text
lib/
├── main.dart                              # ProviderScope 和应用入口
└── edit/
    ├── model/user_profile.dart             # 接口数据与 JSON 转换
    ├── repository/user_repository.dart     # 获取、保存接口及模拟实现
    ├── viewmodel/edit_state.dart           # 不可变表单草稿、校验和保存状态
    ├── viewmodel/edit_viewmodel.dart       # Provider 声明和业务逻辑
    └── page/edit_page.dart                 # 页面、输入框控制器和成功提示
```

## 数据流

1. 页面通过 `ref.watch(editViewModelProvider)` 监听状态，首次监听自动触发
   `EditViewModel.build()`，由 Repository 获取资料。
2. `AsyncValue<EditState>` 表达加载、加载失败、加载成功；失败时点击重试重新请求。
3. 加载成功后创建表单，并初始化控制器。输入时调用 ViewModel 更新不可变草稿。
   控制器只在文本确实变化时同步，避免每次重建覆盖输入或移动光标，并在页面销毁时释放。
4. 点击保存，ViewModel 校验姓名、性别不能为空，年龄必须是 0～150 的整数。
   年龄草稿使用字符串，校验通过后转为整数；姓名、性别提交时去除首尾空白。
5. 保存期间禁用输入框及按钮，ViewModel 也拦截重复提交；成功回填接口返回值并提示。
   保存失败保留草稿、显示错误，可再次保存，不将整个页面切换成加载失败页。
6. 页面退出后自动释放编辑状态；异步保存完成前检查 `ref.mounted`，页面提示前检查
   `mounted`，避免操作已销毁的状态。

## 模拟接口与真实接口

`MockUserRepository` 模拟 800 毫秒延时，初始数据为张三、男、28。
保存结果保留在当前 ProviderScope 的内存中，重新加载可读取，重启应用恢复初始值。
当前没有发起真实 HTTP 请求，也没有本地持久化。

接入真实接口时实现 `UserRepository.fetchProfile()` 和 `saveProfile()`，在
`userRepositoryProvider` 中替换实现。保存方法返回服务端最终资料；请求失败通过异常
交由 ViewModel 处理。页面不依赖具体请求库。

默认模拟实现始终成功。测试通过覆盖 `userRepositoryProvider` 注入失败或受控延时的
实现，验证加载失败重试、保存失败保留输入及重复提交保护等场景。

Riverpod 相关概念可参考[官方 Provider 文档](https://riverpod.dev/docs/concepts2/providers)。

## 检查

```sh
flutter analyze
flutter test
```

测试覆盖加载回填、编辑与光标保留、保存及服务端返回值同步、校验边界、重复提交、
加载失败重试、保存失败重试，以及保存期间退出页面。
