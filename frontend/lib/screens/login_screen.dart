import 'package:flutter/material.dart';
import '../widgets/custom_button.dart';
import '../widgets/custom_text_field.dart';

class LoginScreen extends StatelessWidget {
  const LoginScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Minecraft Launcher',
                style: Theme.of(context).textTheme.displayMedium,
              ),
              const SizedBox(height: 20),
              const CustomTextField(hintText: 'Email', icon: Icons.email),
              const SizedBox(height: 10),
              const CustomTextField(hintText: 'Password', icon: Icons.lock, obscureText: true),
              const SizedBox(height: 20),
              CustomButton(
                text: 'Login',
                onPressed: () {
                  Navigator.pushReplacementNamed(context, '/home');
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}
