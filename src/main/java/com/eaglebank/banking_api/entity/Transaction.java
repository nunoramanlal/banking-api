package com.eaglebank.banking_api.entity;

import com.eaglebank.banking_api.entity.enums.Currency;
import com.eaglebank.banking_api.entity.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number", nullable = false, updatable = false)
    private Account account;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 3)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private TransactionType type;

    @Column(updatable = false)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_timestamp", nullable = false, updatable = false)
    private LocalDateTime createdTimestamp;

    public Transaction(Account account, BigDecimal amount, TransactionType type, String reference) {
        this.account = account;
        this.amount = amount;
        this.type = type;
        this.reference = reference;
        this.currency = Currency.GBP;
    }

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = "tan-" + UUID.randomUUID().toString().replace("-", "");
        }
    }
}
