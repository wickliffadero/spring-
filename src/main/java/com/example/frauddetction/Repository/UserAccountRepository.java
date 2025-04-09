package com.example.frauddetction.Repository;

import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UseraccountEntity, Long> {
    UseraccountEntity findByUsername(String username);
    UseraccountEntity findByPhoneNumber(String phoneNumber);
    List<UseraccountEntity> findByAccountStatus(String accountStatus);
    boolean existsByUsername(String username);
}