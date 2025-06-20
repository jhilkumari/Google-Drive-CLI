package org.griddynamics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Configuration class responsible for loading database credentials and storage path from environment variables.
 * Uses the dotenv library to load variables from a `.env` file.
 */
@Configuration
public class DatabaseConfig {

    @Value("${db.name}")
    public String dbName;

    @Value("${db.user}")
    public String dbUser;

    @Value("${db.password}")
    public String dbPassword;

    @Value("${db.port}")
    public String dbPort;

    @Value("${db.url}")
    public String dbUrl;

    @Value("${storage.folder}")
    public String storageFolder;

}
