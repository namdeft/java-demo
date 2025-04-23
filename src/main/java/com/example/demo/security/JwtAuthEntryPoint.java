package com.example.demo.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

import com.example.demo.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
  @Override
  public void commence(jakarta.servlet.http.HttpServletRequest request,
      jakarta.servlet.http.HttpServletResponse response,
      AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
    response.setContentType("application/json");
    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(
        new ErrorResponse(401, "Unauthorized", authException.getMessage()));
    response.getWriter().write(json);
  }
}
