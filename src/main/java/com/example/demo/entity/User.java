package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users_demo")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "password", nullable = false)
  private String password;
}
