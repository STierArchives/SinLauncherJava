package com.example.SinLauncher.SinLauncherEntites;

// import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor

@Entity
@Table(name = "users")
public class User {

    @Id
    private String uuid;

    @Getter
    private String username;
    private String password;
    private String email;
    private boolean isLoggedIn;
    private boolean cracked;

    // Pass in generatedUUID or legit UUID here in String format
    // UUID.randomUUID() generates a unique UUID with a very low chance for the same
    // UUID
    public User(String uuid, String username, String password, String email, boolean cracked, boolean isLoggedIn) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.email = email;
        this.cracked = cracked;
        this.isLoggedIn = isLoggedIn;
    }
}
