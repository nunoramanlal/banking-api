package com.eaglebank.banking_api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
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

    public User(
            String name,
            String email,
            String phoneNumber,
            String line1,
            String line2,
            String line3,
            String town,
            String county,
            String postcode
    ) {
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
}