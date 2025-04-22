package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.constant.CommonMsg;
import com.example.demo.dto.response.BaseResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {
  @Autowired
  private UserRepository userRepository;

  public BaseResponse<List<UserResponse>> getAllUsers() {
    List<User> users = userRepository.findAll();
    List<UserResponse> userResponses = users.stream()
        .map(this::mapToUserResponse)
        .collect(Collectors.toList());

    return BaseResponse.success(CommonMsg.SUCCESS, userResponses);
  }

  private UserResponse mapToUserResponse(User user) {
    UserResponse userResponse = new UserResponse();
    userResponse.setId(user.getId());
    userResponse.setUsername(user.getUsername());

    // Thêm thông tin về roles
    List<String> roleNames = new ArrayList<>();
    Map<String, List<String>> rolePermissionsMap = new HashMap<>();

    for (Role role : user.getRoles()) {
      String roleName = role.getName();
      roleNames.add(roleName);

      // Thêm permissions của mỗi role
      List<String> permissionNames = role.getPermissions().stream()
          .map(Permission::getName)
          .collect(Collectors.toList());
      rolePermissionsMap.put(roleName, permissionNames);
    }

    userResponse.setRoles(roleNames);
    // userResponse.setRolePermissions(rolePermissionsMap);

    // Tổng hợp tất cả permissions của user (từ tất cả roles)
    Set<String> allPermissions = new HashSet<>();
    user.getRoles()
        .forEach(role -> role.getPermissions().forEach(permission -> allPermissions.add(permission.getName())));
    userResponse.setPermissions(new ArrayList<>(allPermissions));

    return userResponse;
  }
}
