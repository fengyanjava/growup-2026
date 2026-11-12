# Android / Kotlin 复习笔记

整理日期：2026-11-12。根据本次会话中的问题、回答和项目实际代码整理，以问答、示例及易错点为主。

## 阅读顺序

| 笔记 | 内容 |
| --- | --- |
| [构建与 MVVM](android-build-and-mvvm.md) | Gradle JVM 配置、Compile / Assemble、项目分包、ViewModel 委托与初始化 |
| [Kotlin 特性](kotlin-features.md) | 属性与接口委托、函数类型、尾随 Lambda、作用域函数、闭包、接收者 Lambda 与 Compose、内联、函数引用 |
| [Kotlin 泛型](kotlin-generics.md) | out / in、声明处与使用处的型变、星投影、泛型上界、where |

## 整理约定

- 已排除明确标记“不记入”的独立知识点：安全调用 `?.`、Elvis 运算符 `?:`、`is` 与智能转换、`when` 表达式、扩展函数、Lambda 的返回与标签返回、数据类、解构、密封类型、单例对象、伴生对象、匿名对象。
- 上述语法可能出现在其他知识点的代码中，但不再单独展开讲解。
- 协程已转到另一会话，本次不整理 `suspend` 等协程课程；MVVM 笔记仅保留理解现有项目所需的调用关系。
- 示例中的“编译错误”用于说明边界，不应直接复制到可运行代码中。
- 笔记中的代码展开是理解语义的示意，不能视为编译器实际生成代码的逐字还原。

## 项目定位

Android Studio / Gradle 中的工程名是 `Android-Basic`；仓库中的实际目录是 `AndroidBasic`，因此笔记保存在本目录下。

功能说明、运行与验证方法见 [项目 README](../README.md)。
