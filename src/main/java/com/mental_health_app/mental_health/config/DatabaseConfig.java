package com.mental_health_app.mental_health.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;

/**
 * PRODUCTION DATABASE CONFIGURATION (Render / PostgreSQL)
 * ────────────────────────────────────────────────────────
 * Automatically converts Render's native DATABASE_URL (postgres://user:password@host/dbname)
 * into a valid JDBC connection URL (jdbc:postgresql://host:port/dbname) with credentials.
 */
@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${DATABASE_URL:${spring.datasource.url:}}")
    private String databaseUrl;

    @Value("${spring.datasource.username:}")
    private String defaultUsername;

    @Value("${spring.datasource.password:}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        if (databaseUrl != null && !databaseUrl.isBlank()) {
            if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                try {
                    // Normalize protocol for URI parser
                    String cleanUrl = databaseUrl;
                    if (cleanUrl.startsWith("postgres://")) {
                        cleanUrl = "postgresql://" + cleanUrl.substring("postgres://".length());
                    }

                    URI uri = new URI(cleanUrl);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath(); // e.g. /mental_health_db_b8gn

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    config.setJdbcUrl(jdbcUrl);

                    // Extract userInfo (username:password)
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        config.setUsername(userInfo[0]);
                        if (userInfo.length > 1) {
                            config.setPassword(userInfo[1]);
                        }
                    }
                } catch (Exception e) {
                    // Fallback to direct jdbc prefixing
                    String fixedUrl = databaseUrl.startsWith("jdbc:") ? databaseUrl : "jdbc:" + databaseUrl;
                    config.setJdbcUrl(fixedUrl);
                    if (!defaultUsername.isBlank()) config.setUsername(defaultUsername);
                    if (!defaultPassword.isBlank()) config.setPassword(defaultPassword);
                }
            } else {
                config.setJdbcUrl(databaseUrl.startsWith("jdbc:") ? databaseUrl : "jdbc:" + databaseUrl);
                if (!defaultUsername.isBlank()) config.setUsername(defaultUsername);
                if (!defaultPassword.isBlank()) config.setPassword(defaultPassword);
            }
        } else {
            // Local dev fallback if no url provided in prod
            config.setJdbcUrl("jdbc:postgresql://localhost:5432/mental_health_db");
            config.setUsername(defaultUsername);
            config.setPassword(defaultPassword);
        }

        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(20000);

        return new HikariDataSource(config);
    }
}
