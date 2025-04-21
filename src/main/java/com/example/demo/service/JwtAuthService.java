package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.constant.CommonMsg;
import com.example.demo.dto.request.UserDto;
import com.example.demo.dto.response.BaseResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class JwtAuthService {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public BaseResponse<?> register(UserDto userRequest) {
    if (userRepository.findByUsername(userRequest.getUsername()) != null) {
      return BaseResponse.error(CommonMsg.DUPLICATE_USERNAME);
    }

    User newUser = new User();
    newUser.setUsername(userRequest.getUsername());
    newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
    userRepository.save(newUser);

    UserResponse userResponse = new UserResponse();
    userResponse.setId(newUser.getId());
    userResponse.setUsername(newUser.getUsername());
    return BaseResponse.success(CommonMsg.SUCCESS, userResponse);
  }

  public BaseResponse<?> login(UserDto authRequest) {
    return BaseResponse.success(CommonMsg.SUCCESS, null);
  }
}
