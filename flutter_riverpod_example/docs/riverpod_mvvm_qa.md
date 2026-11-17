# Riverpod MVVM 示例学习问答

本文整理本次围绕用户资料编辑示例的学习过程，重点是使用方法、执行顺序，以及与
`provider + ChangeNotifier` 的区别，不展开框架底层实现。

文中的默认代码对应当前工程。多页面隔离的 `family` 代码属于讨论方案，尚未应用到工程。

## 1. 这个示例实现了什么？各层负责什么？

进入页面后获取资料，回填姓名、性别、年龄三个输入框；用户修改后点击保存。
接口先使用异步模拟，保存结果保留在本次应用运行的内存中。

```text
lib/
├── main.dart
└── edit/
    ├── model/user_profile.dart
    ├── repository/user_repository.dart
    ├── viewmodel/edit_state.dart
    ├── viewmodel/edit_viewmodel.dart
    └── page/edit_page.dart
```

| 部分 | 职责 |
|---|---|
| `main.dart` | 启动应用，配置 `ProviderScope` 和首页 |
| Model | 定义接口数据，处理 JSON 转换 |
| Repository | 提供获取、保存资料的方法 |
| EditState | 保存不可变的表单草稿、字段错误、保存状态 |
| ViewModel | 加载资料、修改草稿、校验和保存，更新状态 |
| Page | 展示页面，管理输入框控制器，响应操作和显示提示 |

## 2. 应用怎样启动并进入编辑页？

入口代码：

```dart
void main() {
  runApp(const ProviderScope(child: MyApp()));
}
```

先把 `ProviderScope` 理解为 Riverpod 管理状态的容器入口，让下方页面可以使用 Riverpod。
`MyApp` 中通过 `MaterialApp(home: const EditPage())` 指定首页。

```text
main()
  → runApp()
  → ProviderScope
  → MyApp.build()
  → EditPage
```

配置 `ProviderScope` 本身不会立即发起用户资料请求。

## 3. 编辑页如何触发第一次请求？

```dart
class EditPage extends ConsumerWidget {
  const EditPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(editViewModelProvider);
    // 页面布局……
  }
}
```

- `ConsumerWidget`：在 `build()` 中提供 `WidgetRef ref`，方便使用 Riverpod。
- `ref`：访问 Riverpod 的工具。
- `watch`：获取当前状态，并监听后续变化，让当前 Widget 在状态变化时重建。

本次首次监听会初始化 Provider，执行 ViewModel 的 `build()`：

```dart
@override
Future<EditState> build() async {
  final profile = await ref.watch(userRepositoryProvider).fetchProfile();
  return EditState.fromProfile(profile);
}
```

```text
EditPage.build()
  → ref.watch(editViewModelProvider)
  → 首次初始化 ViewModel
  → EditViewModel.build()
  → Repository.fetchProfile()
```

注意区分 `EditPage.build()` 和 `EditViewModel.build()`：前者构建界面，后者初始化
ViewModel 的状态。页面后续重建时，普通的 `watch` 不会每次重新发起请求；Provider 被
重置、销毁后重建，或者其依赖变化时，才可能重新执行加载。

## 4. 请求中、成功、失败，页面怎么切换？

页面拿到的 `profile` 类型是 `AsyncValue<EditState>`，既表示异步状态，也在成功时携带数据。
下面是展示结构的简化写法：

```dart
profile.when(
  skipLoadingOnRefresh: false,
  loading: () => const Center(child: CircularProgressIndicator()),
  error: (error, stackTrace) => const Text('加载失败'),
  data: (data) => _EditForm(data: data),
)
```

实际工程的错误布局还提供了重试按钮。

```text
ViewModel.build() 开始请求 → loading → 显示转圈
请求成功，返回 EditState  → data    → 显示表单
请求抛出异常              → error   → 显示错误和重试按钮
```

首次加载的三种状态由 `AsyncNotifier` 根据 `build()` 返回的 Future 管理，不必手动
维护 `isLoading` 和首次加载错误字段。

`profile` 是外层 `AsyncValue<EditState>`；`data: (data)` 中的 `data` 已经是成功获取的
`EditState`，可以读取姓名、性别、年龄。

## 5. 获取到的数据如何回填输入框？

加载成功后，页面通过 `_EditForm(data: data)` 把数据交给表单。
表单在 `initState()` 中创建控制器：

```dart
_nameController = TextEditingController(text: widget.data.name);
_genderController = TextEditingController(text: widget.data.gender);
_ageController = TextEditingController(text: widget.data.age);
```

再把控制器交给输入框：

```dart
TextField(controller: _nameController)
```

```text
获取 UserProfile
  → 转成 EditState
  → 传给 _EditForm
  → initState() 创建控制器并设置文字
  → TextField 显示文字
```

`initState()` 在这份表单 State 创建时执行一次，普通重建不会重新创建控制器。
控制器在 `dispose()` 中释放。

## 6. 输入变化如何同步到 ViewModel？

```dart
TextField(
  controller: _nameController,
  onChanged: viewModel.updateName,
)
```

`onChanged: viewModel.updateName` 等价于：

```dart
onChanged: (value) {
  viewModel.updateName(value);
},
```

用户修改姓名后，ViewModel 执行：

```dart
void updateName(String value) => _updateDraft(name: value);

void _updateDraft({String? name, String? gender, String? age}) {
  final current = state.asData?.value;
  if (current == null || current.isSaving) return;

  state = AsyncData(current.copyWith(name: name, gender: gender, age: age));
}
```

1. `state.asData?.value` 获取成功状态里的 `EditState`。
2. `copyWith()` 创建新数据，替换传入的字段，保留其他字段。
3. 给 `state` 赋新值，通知页面更新。

```text
旧数据：张三、男、28
  → copyWith(name: '李四')
新数据：李四、男、28
```

此时只更新草稿，没有调用保存接口。性别、年龄同理。

## 7. 输入变化时，didUpdateWidget 会调用吗？为什么？

在当前实现中，会在父页面重建、更新表单 Widget 配置时调用。

```text
输入变化
  → onChanged
  → ViewModel 更新 state
  → EditPage 的 watch 感知变化
  → EditPage.build() 重新执行
  → 创建新的 _EditForm(data: data)
  → 复用原来的 _EditFormState
  → didUpdateWidget()
  → 表单 build()
```

Widget 是配置对象，父页面可以不断创建新的 Widget。相同位置的 Widget 类型和 key
没有变化时，Flutter 会复用原来的 State，并用 `didUpdateWidget()` 通知它配置已更新。

因此这里不会再次执行 `initState()`。状态更新、界面重建可能被合并，不应把这条流程
理解为每次输入回调都会立即、同步执行一次 `didUpdateWidget()`。

## 8. 控制器里已经有新文字了，_syncText 还有意义吗？

对于“用户输入 → ViewModel → 页面重建”这一轮同步，确实不需要再次赋值，因为两边文字相同。
当前代码会跳过：

```dart
void _syncText(TextEditingController controller, String text) {
  if (controller.text == text) return;

  controller.value = TextEditingValue(
    text: text,
    selection: TextSelection.collapsed(offset: text.length),
  );
}
```

它实际处理的是另一条数据流：**保存返回结果 → 输入框**。

例如用户输入姓名 `" 李四 "`、年龄 `"030"`，保存时会去掉首尾空白并将年龄转成整数。
保存成功后，State 中是 `"李四"`、`"30"`，但控制器还保留原来的文字。
此时 `_syncText()` 才会真正更新输入框。

仅修改 State，并不会自动修改 `TextEditingController.text`。

如果只要求首次回填，保存后无需同步返回数据，可以删掉这段同步逻辑。当前保留它，
是为了让输入框与最终保存结果一致。

## 9. 点击保存后，怎么校验并显示字段错误？

按钮根据保存状态决定是否可点击：

```dart
onPressed: data.isSaving ? null : _save,
```

页面 `_save()` 调用：

```dart
final success = await ref.read(editViewModelProvider.notifier).save();
```

| 写法 | 含义 |
|---|---|
| `ref.watch(editViewModelProvider)` | 获取状态并监听，供页面展示 |
| `ref.read(editViewModelProvider.notifier)` | 获取 ViewModel，调用业务方法 |

ViewModel 先检查是否有数据、是否正在保存，再整理输入：

```dart
final name = current.name.trim();
final gender = current.gender.trim();
final age = int.tryParse(current.age.trim());
```

姓名和性别必填；年龄必须是 0～150 的整数。校验失败，将错误放入 `EditState` 并返回 `false`，
不调用保存接口。页面重建后，通过输入框的 `errorText` 显示错误：

```dart
InputDecoration(labelText: '姓名', errorText: data.nameError)
```

年龄草稿使用字符串，允许暂时为空或不合法；校验通过后，才转为接口模型中的整数。

## 10. 校验通过后，保存状态与结果如何处理？

先设置保存中状态：

```dart
state = AsyncData(current.copyWith(isSaving: true));
```

这里仍然是 `AsyncData`，因此页面继续显示表单，只禁用输入框、按钮，并显示“保存中”。
随后调用 Repository 的 `saveProfile()`。

保存成功：

```dart
if (!ref.mounted) return false;
state = AsyncData(EditState.fromProfile(profile));
return true;
```

新 `EditState` 默认 `isSaving = false`。页面恢复可操作，同步最终资料，并根据返回的
`true` 显示“保存成功”。

保存失败：保留原来的三个字段，将 `saveError` 设为错误信息，恢复可操作，并返回 `false`。
这里不会把整个页面切成首次加载失败的错误页。

请求完成时，`ref.mounted` 检查 Provider 是否仍然有效；页面显示成功提示前的 `mounted`
检查页面 State 是否仍然有效。两者检查的是不同对象。

## 11. ViewModel 如何拿到 Repository？

```dart
final userRepositoryProvider = Provider<UserRepository>(
  (ref) => MockUserRepository(),
);
```

这段配置提供一个普通依赖对象：需要 `UserRepository` 时，默认创建 `MockUserRepository`。
在当前同一作用域内，只要对应 Provider 没被重置或销毁，后续访问会复用同一个对象。

ViewModel 初始化时：

```dart
final repository = ref.watch(userRepositoryProvider);
```

保存操作时：

```dart
final repository = ref.read(userRepositoryProvider);
```

- `watch` 声明依赖，依赖变化时 ViewModel 会重新构建。
- `read` 获取当前对象，执行本次操作。

以后接入真实接口，可以实现 `UserRepository`，然后在这个 Provider 中替换具体实现，
无需让页面了解 HTTP 请求细节。

## 12. autoDispose 什么时候释放状态？

当前编辑 Provider 使用了 `.autoDispose`。当它持续没有监听者，经过一帧后，Riverpod
会释放它管理的状态。

```text
页面开始监听 → 创建 ViewModel、加载资料
所有监听者移除 → 自动释放编辑状态
之后再次监听 → 重新创建、重新加载
```

不能简单地把“离开页面”当作“状态已释放”：push 新页面盖住原页面时，原页面可能仍保留
监听；其他页面也可能在监听同一个 Provider。

| 对象 | 当前生命周期 |
|---|---|
| 编辑 ViewModel、EditState | 没有监听者后自动释放 |
| 模拟 Repository | 未配置 autoDispose，在当前 ProviderScope 中保留 |

在编辑 Provider 确实释放后，未保存草稿会消失；已保存的数据仍在 Repository 中。
`autoDispose` 不会自动取消已发出的真实网络请求，取消请求需要额外实现。

## 13. 同一个页面 push 两次，状态会共享吗？如何隔离？

会。当前两个编辑页访问的是同一个 `editViewModelProvider`，在相同作用域下共享状态。
`autoDispose` 只管理清理时机，不会为每个页面自动创建一份状态。

```text
编辑页 A ─┐
          ├─ editViewModelProvider → 同一个 ViewModel
编辑页 B ─┘
```

讨论中的一种解决方式是 `family + 每次打开时生成的唯一标识`。以下是改法示意，
当前工程尚未采用：

```dart
final editViewModelProvider =
    AsyncNotifierProvider.autoDispose.family<EditViewModel, EditState, Object>(
      EditViewModel.new,
      retry: (retryCount, error) => null,
    );

class EditViewModel extends AsyncNotifier<EditState> {
  EditViewModel(this.sessionId);

  final Object sessionId;

  // 原有 build、修改、保存方法继续保留。
}
```

每次 push 前创建标识，传给页面：

```dart
final sessionId = Object();
Navigator.of(context).push(
  MaterialPageRoute(builder: (_) => EditPage(sessionId: sessionId)),
);
```

页面构造函数需要接收该标识，所有操作使用同一个参数；子表单也需要接收它：

```dart
ref.watch(editViewModelProvider(sessionId));
ref.read(editViewModelProvider(sessionId).notifier).save();
ref.invalidate(editViewModelProvider(sessionId));
```

```text
页面 A → provider(sessionA) → ViewModel A
页面 B → provider(sessionB) → ViewModel B
```

注意：

- 标识在一次打开期间保持不变，不能在页面 `build()` 中反复创建。
- `family` 参数相同就共享，参数不同才隔离。
- 使用 `userId` 时，同一个用户打开两次仍会共享；每次编辑独立需要会话标识。
- 隔离的是草稿、错误和保存状态。Repository 仍可共享，保存同一用户会修改同一份业务数据。

## 14. 与 provider + ChangeNotifier 相比，主要写法有什么区别？

两者都能实现 MVVM，Model、Repository 和业务规则可以相同。

| 功能 | Provider + ChangeNotifier | 当前 Riverpod + AsyncNotifier |
|---|---|---|
| 提供 ViewModel | Widget 树中的 `ChangeNotifierProvider` | 顶层 Provider 声明和 `ProviderScope` |
| 监听状态 | `context.watch<EditViewModel>()` | `ref.watch(editViewModelProvider)` |
| 调用方法 | `context.read<EditViewModel>().save()` | `ref.read(editViewModelProvider.notifier).save()` |
| 更新页面 | 修改状态后 `notifyListeners()` | 给 `state` 赋新值 |
| 首次加载 | 自己定义并调用 `load()` | 首次监听触发 ViewModel 的 `build()` |
| 异步展示 | 自行维护加载和错误状态，或封装状态类型 | 使用 `AsyncValue` |
| 局部监听 | `Selector` / `context.select` | `select` |
| 依赖注入 | 常用构造函数注入、Provider/ProxyProvider 装配 | 在 Provider 中通过 `ref` 声明依赖 |

例如相同的姓名修改：

```dart
// Provider + ChangeNotifier：假设 _data 是当前 EditState。
_data = current.copyWith(name: value);
notifyListeners();

// 当前 Riverpod：state 是 AsyncValue<EditState>。
state = AsyncData(current.copyWith(name: value));
```

不可变状态不是 Riverpod 独有的；Provider 也可以复用 `EditState`、`copyWith()`。
Provider 通过构造函数注入 Repository，同样容易单独测试，不需要在 ViewModel 中持有 context。

## 15. 多页面隔离这么麻烦，Riverpod 的优势是什么？

对于每次打开都要求独立草稿的简单编辑页，在每个路由下创建一个 Provider ViewModel
确实更直观。Riverpod 不保证每个场景都更少代码。

本例把表单草稿也放进 Riverpod，方便演示状态更新，但多页面实例需要额外处理隔离。
官方建议将表单这类临时状态留在页面本地，避免路由之间相互覆盖。

Riverpod 的优势更多体现在：

- **异步数据的加载与复用**：按用户 ID 提供资料，让多个页面共享加载结果和异步状态。
- **依赖联动**：通过 `ref.watch` 表达依赖，登录用户变化时，相关资料和页面可以随之更新。
- **统一获取依赖**：业务 Provider 之间通过 `ref` 组合，不需要依赖 Widget 的 BuildContext。

针对本例，更贴近日常项目的一种组织方式是：Riverpod 管理 Repository、共享用户资料，
页面自己的状态或局部 ViewModel 管理输入草稿。这是设计建议，当前示例仍保留原实现。

| 需求 | 可以采用的组织方式 |
|---|---|
| 页面独立的输入、校验、保存状态 | 页面本地状态或局部 ViewModel |
| 多个页面共享用户资料 | Riverpod |
| 根据 userId 加载、复用资料 | Riverpod family |
| 登录变化后联动其他数据 | Riverpod 依赖机制 |

## 16. EditViewModel.new 是什么写法？

这是 Dart 的构造函数引用（constructor tear-off），表示把创建对象的函数传过去。

在当前不带参数的 Provider 声明中：

```dart
EditViewModel.new
```

等价于：

```dart
() => EditViewModel()
```

区别是：

```dart
EditViewModel()    // 立即创建对象。
EditViewModel.new  // 引用构造函数，供之后调用。
```

Provider 接收创建函数，在需要初始化时调用它，而不是在声明 Provider 时立即创建 ViewModel。
如果构造函数有参数，引用也会保留这些参数，例如前文 family 方案中的构造函数可以理解为
`(Object sessionId) => EditViewModel(sessionId)`。

这和 `onChanged: viewModel.updateName` 类似：一个传递构造函数，一个传递普通方法。

## 17. 整个执行流程怎么串起来？

```text
启动应用，配置 ProviderScope
  → 打开 EditPage
  → watch 编辑 Provider
  → ViewModel.build() 调用 Repository 获取资料
  → 加载中显示转圈，失败显示错误，成功显示表单
  → 初始化控制器，回填姓名、性别、年龄
  → 用户输入，onChanged 调用 ViewModel 更新草稿
  → state 更新，页面重建
  → 点击保存，通过 read 获取 ViewModel 并调用 save()
  → 校验失败：显示字段错误，不请求接口
  → 校验通过：isSaving = true，禁用表单，调用保存接口
  → 成功：更新最终资料，回填输入框，页面提示成功
  → 失败：保留草稿，显示错误，允许重试
```

先掌握三个操作：

```dart
// 页面监听状态。
ref.watch(editViewModelProvider);

// 页面调用业务方法。
ref.read(editViewModelProvider.notifier).save();

// ViewModel 更新状态。
state = AsyncData(newState);
```

## 官方参考

- [Riverpod Provider 概念](https://riverpod.dev/docs/concepts2/providers)
- [Family：按参数区分状态](https://riverpod.dev/docs/concepts2/family)
- [自动释放](https://riverpod.dev/docs/concepts2/auto_dispose)
- [Provider 与 Riverpod 对比](https://riverpod.dev/docs/from_provider/provider_vs_riverpod)
- [Riverpod 使用建议：临时状态和表单](https://riverpod.dev/docs/root/do_dont)
- [Provider 包文档](https://pub.dev/packages/provider)
- [Dart 构造函数引用](https://dart.dev/language/constructors#constructor-tear-offs)
