package com.autotea.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class StorageInitializer implements CommandLineRunner {

    private final StorageProperties storageProperties;

    @Override
    public void run(String... args) throws IOException {
        Files.createDirectories(Path.of(storageProperties.runDir()));
    }
}
