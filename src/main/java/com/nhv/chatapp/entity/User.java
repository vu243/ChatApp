package com.nhv.chatapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "avatar")
    private String avatar;

    @ColumnDefault("0")
    @Column(name = "online")
    private Boolean online;

    @Column(name = "lastSeen")
    private Instant lastSeen;

    @ColumnDefault("0")
    @Column(name = "isVerified")
    private Boolean isVerified;

    @Column(name = "createAt", nullable = false)
    private Instant createAt;

    @Column(name = "updateAt")
    private Instant updateAt;

    @PrePersist
    public void prePersist() {
        if (this.createAt == null) {
            this.createAt = Instant.now();
        }
        this.updateAt = Instant.now();
        this.online = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateAt = Instant.now();
    }
}