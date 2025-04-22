package com.example.demo.dataInit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Component
public class DataInitializer {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PermissionRepository permissionRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @PostConstruct
  @Transactional
  // func sẽ được gọi khi ứng dụng khởi động
  public void initialize() {
    // Khởi tạo permissions
    Permission readPermission = createPermissionIfNotFound("READ");
    Permission writePermission = createPermissionIfNotFound("WRITE");
    Permission deletePermission = createPermissionIfNotFound("DELETE");
    Permission adminPermission = createPermissionIfNotFound("ADMIN");

    // Khởi tạo roles
    Role adminRole = createRoleIfNotFound("ADMIN", new HashSet<>(Arrays.asList(
        readPermission, writePermission, deletePermission, adminPermission)));

    Role moderatorRole = createRoleIfNotFound("MODERATOR", new HashSet<>(Arrays.asList(
        readPermission, writePermission)));

    Role userRole = createRoleIfNotFound("USER", new HashSet<>(Arrays.asList(
        readPermission)));

    // Khởi tạo admin user
    if (userRepository.findByUsername("admin") == null) {
      User admin = new User();
      admin.setUsername("admin");
      admin.setPassword(passwordEncoder.encode("admin123"));
      admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
      userRepository.save(admin);
    }

    // Khởi tạo moderator user
    if (userRepository.findByUsername("moderator") == null) {
      User moderator = new User();
      moderator.setUsername("moderator");
      moderator.setPassword(passwordEncoder.encode("mod123"));
      moderator.setRoles(new HashSet<>(Arrays.asList(moderatorRole)));
      userRepository.save(moderator);
    }

    // Khởi tạo normal user
    if (userRepository.findByUsername("user") == null) {
      User user = new User();
      user.setUsername("user");
      user.setPassword(passwordEncoder.encode("user123"));
      user.setRoles(new HashSet<>(Arrays.asList(userRole)));
      userRepository.save(user);
    }
  }

  @Transactional
  public Permission createPermissionIfNotFound(String name) {
    Permission permission = permissionRepository.findByName(name);
    if (permission == null) {
      permission = new Permission();
      permission.setName(name);
      permissionRepository.save(permission);
    }
    return permission;
  }

  @Transactional
  public Role createRoleIfNotFound(String name, Set<Permission> permissions) {
    Role role = roleRepository.findByName(name);
    if (role == null) {
      role = new Role();
      role.setName(name);
      role.setPermissions(permissions);
      roleRepository.save(role);
    }
    return role;
  }
}
