package com.example.frauddetction.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity 
@Table(name = "useraccount")
public class UseraccountEntity implements UserDetails {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(name="username", unique = true)
    private String username;

    @Column(name="email")
    private String email;

    @Column(name="phonenumber", unique = true)
    private String phoneNumber;

    @Column(name="password")
    private String password; 

    @Column(name="role")
    private String role; 

    @Column(name = "account_status")
    private String accountStatus = "ACTIVE"; 

    public UseraccountEntity() {
        // Set default role to USER
        this.role = "USER";
    }

    public UseraccountEntity(String username, String email, String phoneNumber, String password, String role) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public boolean isBlacklisted() {
        return "BLACKLISTED".equals(accountStatus);
    }

    public boolean isWhitelisted() {
        return "WHITELISTED".equals(accountStatus);
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isEmpty()) {
            // Remove ROLE_ prefix if it exists
            String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(normalizedRole.toUpperCase()));
        }
        // Add default user role if no role is set
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"BLACKLISTED".equals(accountStatus);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}