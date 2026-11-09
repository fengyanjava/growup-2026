import 'package:flutter/material.dart';

import 'catalog/example_catalog_page.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'Flutter 示例集',
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff3457d5)),
      useMaterial3: true,
      scaffoldBackgroundColor: const Color(0xfff5f6fa),
    ),
    home: const ExampleCatalogPage(),
  );
}
