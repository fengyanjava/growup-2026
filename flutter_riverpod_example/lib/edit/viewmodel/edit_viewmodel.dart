import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../model/user_profile.dart';
import '../repository/user_repository.dart';
import 'edit_state.dart';

// Repository 随 ProviderScope 保留，离开编辑页后仍能读取已保存的数据。
final userRepositoryProvider = Provider<UserRepository>(
  (ref) => MockUserRepository(),
);

final editViewModelProvider =
    AsyncNotifierProvider.autoDispose<EditViewModel, EditState>(
      EditViewModel.new,
      // 加载失败直接展示错误，由用户点击重试。
      retry: (retryCount, error) => null,
    );

class EditViewModel extends AsyncNotifier<EditState> {
  @override
  Future<EditState> build() async {
    final profile = await ref.watch(userRepositoryProvider).fetchProfile();
    return EditState.fromProfile(profile);
  }

  void updateName(String value) => _updateDraft(name: value);
  void updateGender(String value) => _updateDraft(gender: value);
  void updateAge(String value) => _updateDraft(age: value);

  void _updateDraft({String? name, String? gender, String? age}) {
    final current = state.asData?.value;
    if (current == null || current.isSaving) return;
    state = AsyncData(current.copyWith(name: name, gender: gender, age: age));
  }

  /// 返回是否保存成功。UI 提示由页面处理，不在 ViewModel 中持有 context。
  Future<bool> save() async {
    final current = state.asData?.value;
    if (current == null || current.isSaving) return false;

    final name = current.name.trim();
    final gender = current.gender.trim();
    final age = int.tryParse(current.age.trim());
    final validated = EditState(
      name: current.name,
      gender: current.gender,
      age: current.age,
      nameError: name.isEmpty ? '请输入姓名' : null,
      genderError: gender.isEmpty ? '请输入性别' : null,
      ageError: age == null || age < 0 || age > 150 ? '请输入 0～150 的整数年龄' : null,
    );
    if (validated.hasValidationError) {
      state = AsyncData(validated);
      return false;
    }

    state = AsyncData(current.copyWith(isSaving: true));
    try {
      final profile = await ref
          .read(userRepositoryProvider)
          .saveProfile(UserProfile(name: name, gender: gender, age: age!));
      // 请求完成时页面可能已退出，不能更新已销毁的 Provider。
      if (!ref.mounted) return false;
      state = AsyncData(EditState.fromProfile(profile));
      return true;
    } catch (_) {
      if (!ref.mounted) return false;
      // 保持 AsyncData，避免错误页替换表单而丢失输入。
      state = AsyncData(
        EditState(
          name: current.name,
          gender: current.gender,
          age: current.age,
          saveError: '保存失败，请稍后重试',
        ),
      );
      return false;
    }
  }
}
