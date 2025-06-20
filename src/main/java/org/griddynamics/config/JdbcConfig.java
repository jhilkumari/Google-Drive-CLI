package org.griddynamics.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.boot.jdbc.DataSourceBuilder;

@Configuration
public class JdbcConfig {
    @Autowired
    private DatabaseConfig databaseConfig;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(databaseConfig.dbUrl)
                .username(databaseConfig.dbUser)
                .password(databaseConfig.dbPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public Connection connection(DataSource dataSource) throws SQLException {
        return dataSource.getConnection();
    }
}