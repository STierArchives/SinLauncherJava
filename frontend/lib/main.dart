import 'package:flutter/material.dart';
import 'ui/screens/home_screen.dart';

void main() => runApp(const SinLauncher());

class SinLauncher extends StatelessWidget {
  const SinLauncher({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      title: 'SinLauncher',
      home: HomeScreen(),
    );
  }
}
