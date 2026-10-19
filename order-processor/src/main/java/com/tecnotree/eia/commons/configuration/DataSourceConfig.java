package com.tecnotree.eia.commons.configuration;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Base64;

@Configuration
public class DataSourceConfig {
    @Value("${DB_URL}")
    private String dbUrl;

    @Value("${DB_USERNAME}")
    private String dbUsername;

    @Value("${DB_PASSWORD}")
    private String dbPasswordEncoded;

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        // Decode the Base64 password here
        String decodedPassword;
        try {
            decodedPassword = new String(Base64.getDecoder().decode(dbPasswordEncoded));
        } catch (IllegalArgumentException e) {
            // Log an error or throw a more specific exception if the password is not valid Base64
            System.err.println("ERROR: DB_PASSWORD is not a valid Base64 encoded string. " + e.getMessage());
            throw new RuntimeException("Failed to decode database password. Please check DB_PASSWORD environment variable.", e);
        }
        dataSource.setPassword(decodedPassword);

        // Set HikariCP specific properties
        dataSource.setMaximumPoolSize(30);
        dataSource.setConnectionTimeout(60000);

        return dataSource;
    }
}
