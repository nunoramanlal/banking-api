package com.eaglebank.banking_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String line1;

    private String line2;

    private String line3;

    @Column(nullable = false)
    private String town;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private String postcode;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedTimestamp;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    public User(
            String name,
            String email,
            String phoneNumber,
            String line1,
            String line2,
            String line3,
            String town,
            String county,
            String postcode) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = "usr-" + UUID.randomUUID().toString().replace("-", "");
        }
    }
}
