
package com.examplatform.user.entity;

import com.examplatform.user.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_subject", unique = true, length = 255)
    private String googleSubject;

    @Column(name = "phone_number", length = 25)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    protected User() {
    }

    public User(String name, String email, String passwordHash, UserRole role) {
        this(name, email, passwordHash, role, null, null);
    }

    public User(
            String name,
            String email,
            String passwordHash,
            UserRole role,
            String googleSubject) {
        this(name, email, passwordHash, role, googleSubject, null);
    }

    public User(
            String name,
            String email,
            String passwordHash,
            UserRole role,
            String googleSubject,
            String phoneNumber) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.googleSubject = googleSubject;
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber.trim();
    }
}
