package com.example.demo.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.constant.CommonMsg;
import com.example.demo.dto.request.UserDto;
import com.example.demo.dto.response.BaseResponse;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class JwtAuthService {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  public BaseResponse<?> register(UserDto userRequest) {
    if (userRepository.findByUsername(userRequest.getUsername()) != null) {
      return BaseResponse.error(CommonMsg.DUPLICATE_USERNAME);
    }

    User newUser = new User();
    newUser.setUsername(userRequest.getUsername());
    newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
    Set<Role> roles = new HashSet<>();
    Role userRole = roleRepository.findByName("USER");
    if (userRole == null) {
      userRole = new Role();
      userRole.setName("USER");
      roleRepository.save(userRole);
    }

    roles.add(userRole);
    newUser.setRoles(roles);
    userRepository.save(newUser);

    UserResponse userResponse = mapToUserResponse(newUser);

    return BaseResponse.success(CommonMsg.REGISTER_SUCCESS, userResponse);
  }

  public BaseResponse<?> login(UserDto authRequest) {
    User user = userRepository.findByUsername(authRequest.getUsername());
    if (user == null || !passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
      return BaseResponse.error(CommonMsg.LOGIN_FAILED);
    }

    String token = jwtTokenProvider.createToken(user);

    LoginResponse loginResponse = new LoginResponse();
    loginResponse.setUsername(user.getUsername());
    loginResponse.setToken(token);

    return BaseResponse.success(CommonMsg.LOGIN_SUCCESS, loginResponse);
  }

  public UserResponse mapToUserResponse(User user) {
    UserResponse userResponse = new UserResponse();
    userResponse.setId(user.getId());
    userResponse.setUsername(user.getUsername());

    return userResponse;
  }
}
