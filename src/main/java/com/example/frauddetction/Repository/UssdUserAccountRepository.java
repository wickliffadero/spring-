package com.example.frauddetction.Repository;


import com.example.frauddetction.model.UssdUserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UssdUserAccountRepository extends JpaRepository<UssdUserAccount, String> {
    Optional<UssdUserAccount> findByPhoneNumber(String phoneNumber);
}
