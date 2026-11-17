import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod_example/edit/model/user_profile.dart';
import 'package:flutter_riverpod_example/edit/repository/user_repository.dart';
import 'package:flutter_riverpod_example/edit/viewmodel/edit_viewmodel.dart';
import 'package:flutter_riverpod_example/main.dart';

class TestUserRepository implements UserRepository {
  UserProfile profile = const UserProfile(name: '张三', gender: '男', age: 28);
  final fetchGate = Completer<void>();
  Completer<void>? saveGate;
  bool failFetch = false;
  bool failSave = false;
  int fetchCalls = 0;
  int saveCalls = 0;

  @override
  Future<UserProfile> fetchProfile() async {
    fetchCalls++;
    await fetchGate.future;
    if (failFetch) throw Exception('加载失败');
    return profile;
  }

  @override
  Future<UserProfile> saveProfile(UserProfile value) async {
    saveCalls++;
    if (saveGate != null) await saveGate!.future;
    if (failSave) throw Exception('保存失败');
    profile = value;
    return profile;
  }
}

Future<void> showApp(WidgetTester tester, TestUserRepository repository) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [userRepositoryProvider.overrideWithValue(repository)],
      child: const MyApp(),
    ),
  );
}

Future<void> finishLoading(
  WidgetTester tester,
  TestUserRepository repository,
) async {
  repository.fetchGate.complete();
  await tester.pumpAndSettle();
}

Finder get nameField => find.widgetWithText(TextField, '姓名');
Finder get genderField => find.widgetWithText(TextField, '性别');
Finder get ageField => find.widgetWithText(TextField, '年龄');

void main() {
  testWidgets('加载完成后回填资料，重建和编辑保持输入及光标', (tester) async {
    final repository = TestUserRepository();
    await showApp(tester, repository);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.byType(TextField), findsNothing);
    await finishLoading(tester, repository);

    expect(find.text('张三'), findsOneWidget);
    expect(find.text('男'), findsOneWidget);
    expect(find.text('28'), findsOneWidget);
    await tester.enterText(nameField, '李四');
    await tester.pump();
    final controller = tester.widget<TextField>(nameField).controller!;
    controller.selection = const TextSelection.collapsed(offset: 1);
    await tester.enterText(ageField, '30');
    await tester.pump();
    expect(controller.text, '李四');
    expect(controller.selection.baseOffset, 1);
    expect(repository.fetchCalls, 1);
  });

  testWidgets('保存提交编辑后的数据，期间禁用表单并阻止重复提交', (tester) async {
    final repository = TestUserRepository()..saveGate = Completer<void>();
    await showApp(tester, repository);
    await finishLoading(tester, repository);
    await tester.enterText(nameField, ' 李四 ');
    await tester.enterText(genderField, '女');
    await tester.enterText(ageField, '030');
    await tester.tap(find.text('保存'));
    await tester.pump();

    expect(find.text('保存中…'), findsOneWidget);
    expect(
      tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
      isNull,
    );
    for (final field in tester.widgetList<TextField>(find.byType(TextField))) {
      expect(field.enabled, isFalse);
    }
    final context = tester.element(find.byType(MyApp));
    final container = ProviderScope.containerOf(context);
    expect(
      await container.read(editViewModelProvider.notifier).save(),
      isFalse,
    );
    expect(repository.saveCalls, 1);

    repository.saveGate!.complete();
    await tester.pumpAndSettle();
    expect(repository.profile.toJson(), {
      'name': '李四',
      'gender': '女',
      'age': 30,
    });
    expect(find.text('保存成功'), findsOneWidget);
    expect(tester.widget<TextField>(nameField).controller!.text, '李四');
    expect(tester.widget<TextField>(ageField).controller!.text, '30');

    // 重新加载仍能获取保存后的资料。
    container.invalidate(editViewModelProvider);
    await tester.pumpAndSettle();
    expect(find.text('李四'), findsOneWidget);
    expect(repository.fetchCalls, 2);
  });

  testWidgets('必填及年龄校验失败时不调用保存接口', (tester) async {
    final repository = TestUserRepository();
    await showApp(tester, repository);
    await finishLoading(tester, repository);
    await tester.enterText(nameField, '  ');
    await tester.enterText(genderField, '');
    await tester.enterText(ageField, 'abc');
    await tester.tap(find.text('保存'));
    await tester.pumpAndSettle();
    expect(find.text('请输入姓名'), findsOneWidget);
    expect(find.text('请输入性别'), findsOneWidget);
    expect(find.text('请输入 0～150 的整数年龄'), findsOneWidget);

    await tester.enterText(nameField, '李四');
    await tester.enterText(genderField, '女');
    for (final age in ['', '-1', '151', '1.5']) {
      await tester.enterText(ageField, age);
      await tester.tap(find.text('保存'));
      await tester.pumpAndSettle();
      expect(find.text('请输入 0～150 的整数年龄'), findsOneWidget);
    }
    expect(repository.saveCalls, 0);

    for (final age in ['0', '150']) {
      await tester.enterText(ageField, age);
      await tester.tap(find.text('保存'));
      await tester.pumpAndSettle();
      expect(repository.profile.age, int.parse(age));
    }
    expect(repository.saveCalls, 2);
  });

  testWidgets('加载失败展示重试，重试成功回填', (tester) async {
    final repository = TestUserRepository()..failFetch = true;
    await showApp(tester, repository);
    await finishLoading(tester, repository);
    expect(find.text('加载失败，请检查后重试'), findsOneWidget);
    expect(repository.fetchCalls, 1);
    repository.failFetch = false;
    await tester.tap(find.text('重试'));
    await tester.pumpAndSettle();
    expect(find.text('张三'), findsOneWidget);
    expect(repository.fetchCalls, 2);
  });

  testWidgets('保存失败保留输入，支持再次保存', (tester) async {
    final repository = TestUserRepository()..failSave = true;
    await showApp(tester, repository);
    await finishLoading(tester, repository);
    await tester.enterText(nameField, '王五');
    await tester.tap(find.text('保存'));
    await tester.pumpAndSettle();
    expect(find.text('保存失败，请稍后重试'), findsOneWidget);
    expect(tester.widget<TextField>(nameField).controller!.text, '王五');
    expect(tester.widget<TextField>(nameField).enabled, isTrue);
    repository.failSave = false;
    await tester.tap(find.text('保存'));
    await tester.pumpAndSettle();
    expect(find.text('保存失败，请稍后重试'), findsNothing);
    expect(find.text('保存成功'), findsOneWidget);
    expect(repository.profile.name, '王五');
  });

  testWidgets('请求保存期间退出页面，完成后不会更新已销毁的状态', (tester) async {
    final repository = TestUserRepository()..saveGate = Completer<void>();
    await showApp(tester, repository);
    await finishLoading(tester, repository);
    await tester.tap(find.text('保存'));
    await tester.pump();
    await tester.pumpWidget(const SizedBox());
    await tester.pump();
    repository.saveGate!.complete();
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
}
