package com.bootcamp.flowcheck.domain.auth.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PM")
@Getter
@NoArgsConstructor
public class Pm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String role = "PM";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Pm(String name, String email, String role, LocalDateTime createdAt) {
        this.name = name;
        this.email = email;
        this.role = role != null ? role : "PM";
        this.createdAt = createdAt;
    }
}
