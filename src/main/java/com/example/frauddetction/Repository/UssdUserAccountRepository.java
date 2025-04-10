package com.example.frauddetction.Repository;

import com.example.frauddetction.model.UssdUserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UssdUserAccountRepository extends JpaRepository<UssdUserAccount, Long> {
    Optional<UssdUserAccount> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    long countByIsActive(boolean isActive);
}
