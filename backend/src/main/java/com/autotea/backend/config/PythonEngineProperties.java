package com.autotea.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.python-engine")
public record PythonEngineProperties(String baseUrl, int timeoutSeconds) {
}
