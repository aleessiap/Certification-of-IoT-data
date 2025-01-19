package com.certifier.Auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String roles;

    public String getPassword() {
        return password;
    }

    public String getRoles() {
        return roles;
    }

    public String getUsername() {
        return username;
    }

    public String setPassword(String password) {
        return this.password = password;
    }

}
