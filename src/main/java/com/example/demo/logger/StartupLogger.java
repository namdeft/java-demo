package com.example.demo.logger;

import org.springframework.core.env.Environment;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class StartupLogger {

  private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  private final Environment env;

  public StartupLogger(Environment env) {
    this.env = env;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logSwaggerUrl() {
    String host = env.getProperty("server.host");
    String port = System.getProperty("server.port");
    if (host == null || host.isEmpty()) {
      host = "localhost";
    }
    if (port == null || port.isEmpty()) {
      port = "8080";
    }
    String swaggerUrl = "http://" + host + ":" + port + "/api/v1/swagger-ui/index.html";
    logger.info("Swagger UI is available at: {}", swaggerUrl);
  }
}
