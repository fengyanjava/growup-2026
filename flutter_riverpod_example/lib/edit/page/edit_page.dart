import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../viewmodel/edit_state.dart';
import '../viewmodel/edit_viewmodel.dart';

class EditPage extends ConsumerWidget {
  const EditPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(editViewModelProvider);
    profile.isLoading;
    return Scaffold(
      appBar: AppBar(title: const Text('编辑用户资料')),
      body: SafeArea(
        child: profile.when(
          skipLoadingOnRefresh: false,
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, stackTrace) => Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text('加载失败，请检查后重试'),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: () => ref.invalidate(editViewModelProvider),
                  child: const Text('重试'),
                ),
              ],
            ),
          ),
          data: (data) => _EditForm(data: data),
        ),
      ),
    );
  }
}

class _EditForm extends ConsumerStatefulWidget {
  const _EditForm({required this.data});
  final EditState data;

  @override
  ConsumerState<_EditForm> createState() => _EditFormState();
}

class _EditFormState extends ConsumerState<_EditForm> {
  late final TextEditingController _nameController;
  late final TextEditingController _genderController;
  late final TextEditingController _ageController;

  @override
  void initState() {
    super.initState();
    // 首次加载成功后才创建表单，避免在 build 中反复回填、移动光标。
    _nameController = TextEditingController(text: widget.data.name);
    _genderController = TextEditingController(text: widget.data.gender);
    _ageController = TextEditingController(text: widget.data.age);
  }

  @override
  void didUpdateWidget(covariant _EditForm oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 仅同步变化的数据，也支持保存后回填服务端规范化的结果。
    _syncText(_nameController, widget.data.name);
    _syncText(_genderController, widget.data.gender);
    _syncText(_ageController, widget.data.age);
  }

  void _syncText(TextEditingController controller, String text) {
    if (controller.text == text) return;
    controller.value = TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }

  @override
  void dispose() {
    _nameController.dispose();
    _genderController.dispose();
    _ageController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    FocusScope.of(context).unfocus();
    final success = await ref.read(editViewModelProvider.notifier).save();
    if (!mounted || !success) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('保存成功')));
  }

  @override
  Widget build(BuildContext context) {
    final data = widget.data;
    final viewModel = ref.read(editViewModelProvider.notifier);
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 480),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('用户资料', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 24),
              TextField(
                controller: _nameController,
                enabled: !data.isSaving,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: '姓名',
                  errorText: data.nameError,
                ),
                onChanged: viewModel.updateName,
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _genderController,
                enabled: !data.isSaving,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: '性别',
                  errorText: data.genderError,
                ),
                onChanged: viewModel.updateGender,
              ),
              const SizedBox(height: 20),
              TextField(
                controller: _ageController,
                enabled: !data.isSaving,
                keyboardType: TextInputType.number,
                textInputAction: TextInputAction.done,
                decoration: InputDecoration(
                  labelText: '年龄',
                  errorText: data.ageError,
                ),
                onChanged: viewModel.updateAge,
              ),
              if (data.saveError != null) ...[
                const SizedBox(height: 16),
                Text(
                  data.saveError!,
                  style: TextStyle(color: Theme.of(context).colorScheme.error),
                ),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: data.isSaving ? null : _save,
                child: data.isSaving
                    ? const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          ),
                          SizedBox(width: 8),
                          Text('保存中…'),
                        ],
                      )
                    : const Text('保存'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
