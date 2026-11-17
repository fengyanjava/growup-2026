import '../model/user_profile.dart';

/// 不可变的页面数据。加载状态由外层 AsyncValue 表达。
class EditState {
  const EditState({
    required this.name,
    required this.gender,
    required this.age,
    this.isSaving = false,
    this.nameError,
    this.genderError,
    this.ageError,
    this.saveError,
  });

  factory EditState.fromProfile(UserProfile profile) => EditState(
    name: profile.name,
    gender: profile.gender,
    age: profile.age.toString(),
  );

  final String name;
  final String gender;

  /// 保留原始输入，校验通过后再转换为整数。
  final String age;
  final bool isSaving;
  final String? nameError;
  final String? genderError;
  final String? ageError;
  final String? saveError;

  bool get hasValidationError =>
      nameError != null || genderError != null || ageError != null;

  /// 修改草稿或开启新一轮保存时，清除上一轮的错误。
  EditState copyWith({
    String? name,
    String? gender,
    String? age,
    bool? isSaving,
  }) => EditState(
    name: name ?? this.name,
    gender: gender ?? this.gender,
    age: age ?? this.age,
    isSaving: isSaving ?? this.isSaving,
  );
}
