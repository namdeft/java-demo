package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_groups")
public class UserGroup {
  @Id
  private long id;

  @Column(name = "name")
  private String name;

  @Column(name = "description")
  private String description;
}
