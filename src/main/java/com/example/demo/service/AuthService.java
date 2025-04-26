package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.constant.CommonMsg;
import com.example.demo.constant.OtpType;
import com.example.demo.dto.request.RegisterDto;
import com.example.demo.dto.request.ResetPasswordDto;
import com.example.demo.dto.request.VerifyOtpDto;
import com.example.demo.dto.request.ChangePasswordDto;
import com.example.demo.dto.request.ForgotPasswordDto;
import com.example.demo.dto.request.LoginDto;
import com.example.demo.dto.response.BaseResponse;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Slf4j
public class AuthService {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @Autowired
  private RedisService redisService;

  @Autowired
  private EmailService emailService;

  @Autowired
  private OtpService otpService;

  @Transactional(rollbackFor = Exception.class)
  public BaseResponse<?> register(RegisterDto registerDto) {
    if (userRepository.findByUsernameAndDeletedAtIsNull(registerDto.getUsername()) != null) {
      return BaseResponse.error(CommonMsg.DUPLICATE_USERNAME);
    }

    String otp = String.format("%06d", new Random().nextInt(999999));

    try {
      otpService.saveOtp(registerDto.getUsername(), otp, 300, OtpType.REGISTRATION);
      redisService.savePendingRegistration(registerDto, 300);

      try {
        emailService.sendOtpEmail(registerDto.getEmail(), otp, OtpType.REGISTRATION);
      } catch (Exception e) {
        throw new RuntimeException(CommonMsg.SEND_OTP_FAILED + ": " + e.getMessage());
      }

      return BaseResponse.success(CommonMsg.SEND_OTP_SUCCESS, null);
    } catch (Exception e) {
      // Rollback thủ công cho Redis
      otpService.deleteOtp(registerDto.getUsername());
      redisService.deletePendingRegistration(registerDto.getUsername());

      return BaseResponse.error(CommonMsg.SEND_OTP_FAILED);
    }
  }

  @Transactional(rollbackFor = Exception.class)
  public BaseResponse<?> verifyRegister(VerifyOtpDto verifyOtpDto) {
    RegisterDto registerDto = redisService.getPendingRegistration(verifyOtpDto.getUsername());
    if (registerDto == null) {
      return BaseResponse.error(CommonMsg.USERNAME_IS_NOT_CORRECT);
    }

    if (!otpService.validateOtp(verifyOtpDto.getUsername(), verifyOtpDto.getOtp())) {
      return BaseResponse.error(CommonMsg.OTP_NOT_CORRECT);
    }

    if (userRepository.findByUsernameAndDeletedAtIsNull(verifyOtpDto.getUsername()) != null) {
      return BaseResponse.error(CommonMsg.DUPLICATE_USERNAME);
    }

    User newUser = new User();
    newUser.setUsername(verifyOtpDto.getUsername());
    newUser.setPassword(passwordEncoder.encode(registerDto.getPassword()));
    newUser.setEmail(registerDto.getEmail());

    Set<Role> roles = new HashSet<>();
    Role userRole = roleRepository.findByName("MAM3");
    if (userRole == null) {
      userRole = new Role();
      userRole.setName("MAM3");
      roleRepository.save(userRole);
    }

    roles.add(userRole);
    newUser.setRoles(roles);
    userRepository.save(newUser);

    redisService.deletePendingRegistration(verifyOtpDto.getUsername());

    UserResponse userResponse = mapToUserResponse(newUser);
    return BaseResponse.success(CommonMsg.REGISTER_SUCCESS, userResponse);
  }

  public BaseResponse<?> login(LoginDto loginRequest) {
    User user = userRepository.findByUsernameAndDeletedAtIsNull(loginRequest.getUsername());
    if (user == null) {
      return BaseResponse.error(CommonMsg.USERNAME_IS_NOT_CORRECT);
    }
    if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      return BaseResponse.error(CommonMsg.LOGIN_FAILED);
    }

    String otp = String.format("%06d", new Random().nextInt(999999));
    try {
      otpService.saveOtp(loginRequest.getUsername(), otp, 300, OtpType.LOGIN);

      try {
        emailService.sendOtpEmail(user.getEmail(), otp, OtpType.LOGIN);
      } catch (Exception e) {
        throw new RuntimeException(CommonMsg.SEND_OTP_FAILED + ": " + e.getMessage());
      }
    } catch (Exception e) {
      // Rollback thủ công cho Redis
      otpService.deleteOtp(loginRequest.getUsername());
      return BaseResponse.error(CommonMsg.SEND_OTP_FAILED);
    }

    return BaseResponse.success(CommonMsg.SEND_OTP_SUCCESS, null);
  }

  public BaseResponse<?> verifyLogin(VerifyOtpDto verifyOtpDto) {
    User user = userRepository.findByUsernameAndDeletedAtIsNull(verifyOtpDto.getUsername());
    if (!otpService.validateOtp(verifyOtpDto.getUsername(), verifyOtpDto.getOtp())) {
      return BaseResponse.error(CommonMsg.OTP_NOT_CORRECT);
    }

    String token = jwtTokenProvider.createToken(user);

    UserResponse userResponse = mapToUserResponse(user);
    redisService.saveUserInfo(user.getUsername(), userResponse);

    LoginResponse loginResponse = new LoginResponse();
    loginResponse.setUsername(user.getUsername());
    loginResponse.setToken(token);

    return BaseResponse.success(CommonMsg.LOGIN_SUCCESS, loginResponse);
  }

  public BaseResponse<?> logout(String token) {
    // Kiểm tra token hợp lệ
    token = token.replace("Bearer ", "");
    if (!jwtTokenProvider.validateToken(token)) {
      return BaseResponse.error(CommonMsg.INVALID_TOKEN);
    }

    // Lấy username từ token
    String username = jwtTokenProvider.getUsername(token);

    // Thêm token vào blacklist với TTL bằng thời gian còn lại của token
    try {
      Claims claims = Jwts.parser().setSigningKey(jwtTokenProvider.getSecretKey()).parseClaimsJws(token).getBody();
      long ttlSeconds = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
      if (ttlSeconds > 0) {
        redisService.blacklistToken(token, ttlSeconds);
      }
    } catch (JwtException e) {
      log.error("Error parsing token for logout: {}", token, e);
      return BaseResponse.error(CommonMsg.INVALID_TOKEN);
    }

    // Xóa token và thông tin người dùng khỏi Redis
    redisService.deleteToken(username);
    redisService.deleteUserInfo(username);

    return BaseResponse.success(CommonMsg.LOGOUT_SUCCESS, null);
  }

  public BaseResponse<?> changePassword(ChangePasswordDto changePasswordDto) {
    String oldPassword = changePasswordDto.getOldPassword();
    String newPassword = changePasswordDto.getNewPassword();
    if (oldPassword == null || oldPassword.trim().isEmpty()) {
      return BaseResponse.error(CommonMsg.PASSWORD_IS_NOT_BLANK);
    }
    if (newPassword == null || newPassword.trim().isEmpty()) {
      return BaseResponse.error(CommonMsg.PASSWORD_IS_NOT_BLANK);
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User user = userRepository.findByUsernameAndDeletedAtIsNull(authentication.getName());
    if (user == null) {
      return BaseResponse.error(CommonMsg.USER_NOT_FOUND);
    }
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
      return BaseResponse.error(CommonMsg.OLD_PASSWORD_IS_NOT_CORRECT);
    }
    if (oldPassword.equals(newPassword)) {
      return BaseResponse.error(CommonMsg.NEW_PASSWORD_IS_NOT_DIFFERENT);
    }

    redisService.deleteToken(user.getUsername());
    redisService.deleteUserInfo(user.getUsername());
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    return BaseResponse.success(CommonMsg.CHANGE_PASSWORD_SUCCESS, null);
  }

  public BaseResponse<?> forgotPassword(ForgotPasswordDto forgotPasswordDto) {
    User user = userRepository.findByUsernameAndDeletedAtIsNull(forgotPasswordDto.getUsername());
    if (user == null) {
      return BaseResponse.error(CommonMsg.USER_NOT_FOUND);
    }
    String otp = String.format("%06d", new Random().nextInt(999999));
    try {
      otpService.saveOtp(forgotPasswordDto.getUsername(), otp, 300, OtpType.RESET_PASSWORD);

      try {
        emailService.sendOtpEmail(user.getEmail(), otp, OtpType.RESET_PASSWORD);
      } catch (Exception e) {
        throw new RuntimeException(CommonMsg.SEND_OTP_FAILED + ": " + e.getMessage());
      }
    } catch (Exception e) {
      // Rollback thủ công cho Redis
      otpService.deleteOtp(forgotPasswordDto.getUsername());
      return BaseResponse.error(CommonMsg.SEND_OTP_FAILED);
    }

    return BaseResponse.success(CommonMsg.SEND_OTP_SUCCESS, null);
  }

  public BaseResponse<?> resetPassword(ResetPasswordDto resetPasswordDto) {
    User user = userRepository.findByUsernameAndDeletedAtIsNull(resetPasswordDto.getUsername());
    if (user == null) {
      return BaseResponse.error(CommonMsg.USER_NOT_FOUND);
    }
    if (!otpService.validateOtp(resetPasswordDto.getUsername(), resetPasswordDto.getOtp())) {
      return BaseResponse.error(CommonMsg.OTP_NOT_CORRECT);
    }
    redisService.deleteToken(resetPasswordDto.getUsername());
    redisService.deleteUserInfo(resetPasswordDto.getUsername());

    user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
    userRepository.save(user);

    return BaseResponse.success(CommonMsg.RESET_PASSWORD_SUCCESS, null);
  }

  public UserResponse mapToUserResponse(User user) {
    UserResponse userResponse = new UserResponse();
    userResponse.setId(user.getId());
    userResponse.setUsername(user.getUsername());
    Set<String> rolesSet = new HashSet<>();
    for (Role role : user.getRoles()) {
      rolesSet.add(role.getName());
    }
    userResponse.setRoles(new ArrayList<>(rolesSet));

    Set<String> permissionsSet = new HashSet<>();
    for (Role role : user.getRoles()) {
      role.getPermissions().forEach(permission -> permissionsSet.add(permission.getName()));
    }
    userResponse.setPermissions(new ArrayList<>(permissionsSet));

    return userResponse;
  }
}
