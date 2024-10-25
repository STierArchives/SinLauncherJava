import 'package:flutter/material.dart';
import '../screens/login_screen.dart';
import '../screens/home_screen.dart';

class Routes {
  static const String login = '/login';
  static const String home = '/home';
  static const String settings = '/settings';
  static const String gameLauncher = '/gameLauncher';

  static final Map<String, WidgetBuilder> all = {
    login: (_) => const LoginScreen(),
    home: (_) => const HomeScreen()
  };
}
