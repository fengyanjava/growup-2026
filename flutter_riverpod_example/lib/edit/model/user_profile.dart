/// 接口中的用户资料。年龄在模型中使用整数，输入草稿则保留字符串。
class UserProfile {
  const UserProfile({
    required this.name,
    required this.gender,
    required this.age,
  });

  final String name;
  final String gender;
  final int age;

  factory UserProfile.fromJson(Map<String, dynamic> json) => UserProfile(
    name: json['name'] as String,
    gender: json['gender'] as String,
    age: json['age'] as int,
  );

  Map<String, dynamic> toJson() => {'name': name, 'gender': gender, 'age': age};
}
