package org.griddynamics.config;

import org.griddynamics.service.DatabaseService;
import org.griddynamics.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;

@Configuration
public class AppConfig {
    @Autowired
    private Connection connection;

    @Value("${storage.folder}")
    private String storageFolder;

    @Bean
    public DatabaseService databaseService() {
        return new DatabaseService(connection);
    }

    @Bean
    public StorageService storageService() {
        return new StorageService(storageFolder);
    }
}