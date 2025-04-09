package com.example.frauddetction.Repository;

import com.example.frauddetction.model.UseraccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UseraccountEntity, Long> {

     UseraccountEntity findByUsername(String username);
     UseraccountEntity findByEmail(String email);
}