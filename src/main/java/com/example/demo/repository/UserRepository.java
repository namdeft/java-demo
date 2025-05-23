package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  User findByUsernameAndDeletedAtIsNull(String username);

  Optional<User> findByIdAndDeletedAtIsNull(Long id);

  List<User> findAllByDeletedAtIsNull();
}