package com.example.SinLauncher.SinLauncherEntites.POJOClasses;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CreateAccountRequest {

    @NotBlank (message = "Username must not be blank")
    @Size (min = 3, max = 20, message = "Username must be between 3 to 20 Characters")
    private String username;

    @NotBlank (message = "Password must not be blank")
    @Size (min = 3, max = 256, message = "Password must be between 6 and 256 Characters")
    private String password;

    @NotBlank (message = "Email must not be blank")
    @Email (message = "Email must be valid")
    private String email;
}
