
package com.examplatform.subject.entity;

import com.examplatform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "subjects",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_subjects_admin_name",
            columnNames = {"admin_id", "name"}
        )
    }
)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Subject() {
    }

    public Subject(String name, String description, User admin) {
        this.name = name.trim();
        this.description = description;
        this.admin = admin;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public User getAdmin() {
        return admin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateDetails(String name, String description) {
        this.name = name.trim();
        this.description = description;
    }
}
