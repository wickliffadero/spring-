package com.example.frauddetction.model;

import jakarta.persistence.*;

//Marks this Entity to useraccount table
@Table(name = "useraccount")
@Entity // Marks this class as a JPA entity
public class UseraccountEntity {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment the ID
    private Long id;

     @Column(name="username")
    private String username;

    @Column(name="email")
    private String email;

    @Column(name="phonenumber")
    private String phoneNumber;

    @Column(name="password")
    private String password; // In a real application, you'd hash this!

    // Default constructor (required by JPA)
    public UseraccountEntity() {
    }

    // Constructor with fields
    public UseraccountEntity(String username, String email, String phoneNumber, String password) {
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.password = password;
    }

    // Getters and setters for all fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



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



    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}