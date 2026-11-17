import '../model/user_profile.dart';

/// ViewModel 只依赖接口，接入真实 HTTP 请求时替换实现即可。
abstract class UserRepository {
  Future<UserProfile> fetchProfile();
  Future<UserProfile> saveProfile(UserProfile profile);
}

/// 模拟服务端：延时返回 JSON，并在当前应用运行期间保留保存结果。
class MockUserRepository implements UserRepository {
  Map<String, dynamic> _profile = {'name': '张三', 'gender': '男', 'age': 28};

  @override
  Future<UserProfile> fetchProfile() async {
    await Future<void>.delayed(const Duration(milliseconds: 800));
    return UserProfile.fromJson(_profile);
  }

  @override
  Future<UserProfile> saveProfile(UserProfile profile) async {
    await Future<void>.delayed(const Duration(milliseconds: 800));
    _profile = profile.toJson();
    return UserProfile.fromJson(_profile);
  }
}
