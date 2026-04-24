package com.eaglebank.banking_api.repository;

import com.eaglebank.banking_api.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByUserId(String userId);

    List<Account> findByUserId(String userId);

    Optional<Account> findByAccountNumber(Long accountNumber);
}
