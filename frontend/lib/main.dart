import 'package:flutter/material.dart';
import 'config/app_theme.dart';
import 'config/routes.dart';

void main() {
  runApp(const MinecraftLauncherApp());
}

class MinecraftLauncherApp extends StatelessWidget {
  const MinecraftLauncherApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Minecraft Launcher',
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      initialRoute: Routes.login,
      routes: Routes.all,
    );
  }
}
