package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.request.RegisterDto;
import com.example.demo.dto.request.ResetPasswordDto;
import com.example.demo.dto.request.VerifyOtpDto;
import com.example.demo.dto.request.ChangePasswordDto;
import com.example.demo.dto.request.ForgotPasswordDto;
import com.example.demo.dto.request.LoginDto;
import com.example.demo.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Auth", description = "API Auth")
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {
  @Autowired
  private AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterDto request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/verify-register")
  public ResponseEntity<?> verifyRegister(@RequestBody VerifyOtpDto request) {
    return ResponseEntity.ok(authService.verifyRegister(request));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginDto request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/verify-login")
  public ResponseEntity<?> verifyLogin(@RequestBody VerifyOtpDto verifyOtpDto) {
    return ResponseEntity.ok(authService.verifyLogin(verifyOtpDto));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout(HttpServletRequest request) {
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    return ResponseEntity.ok(authService.logout(authHeader));
  }

  @PutMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestBody ChangePasswordDto request) {
    return ResponseEntity.ok(authService.changePassword(request));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDto request) {
    return ResponseEntity.ok(authService.forgotPassword(request));
  }

  @PutMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto request) {
    return ResponseEntity.ok(authService.resetPassword(request));
  }
}
