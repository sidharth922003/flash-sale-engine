package com.flashsale.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.URI;

/**
 * Robust cloud PostgreSQL connection configuration (Render, Railway, Supabase).
 * Automatically intercepts cloud URIs ("postgresql://user:pass@host/db"), extracts
 * credentials, and constructs a clean PostgreSQL JDBC URL accepted by org.postgresql.Driver.
 * Automatically resolves cross-region Render hostnames and configures non-blocking pool initialization.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${SPRING_DATASOURCE_URL:${DATABASE_URL:jdbc:postgresql://localhost:5432/flashsaledb}}")
    private String rawUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:${DATABASE_USERNAME:postgres}}")
    private String rawUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:${DATABASE_PASSWORD:postgres}}")
    private String rawPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String cleanUrl = rawUrl != null ? rawUrl.trim() : "jdbc:postgresql://localhost:5432/flashsaledb";
        String username = rawUsername;
        String password = rawPassword;

        // Strip leading jdbc: if present to allow standard URI parsing
        if (cleanUrl.startsWith("jdbc:")) {
            cleanUrl = cleanUrl.substring(5);
        }

        String jdbcUrl;

        if (cleanUrl.startsWith("postgresql://") || cleanUrl.startsWith("postgres://")) {
            try {
                URI uri = URI.create(cleanUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] creds = userInfo.split(":", 2);
                    username = creds[0];
                    if (creds.length > 1) {
                        password = creds[1];
                    }
                }

                String host = uri.getHost();

                // If host is an internal Render shortname without a domain (e.g. "dpg-dbfb03qjnfac738nlrg0-a")
                // check if it can be resolved. If not resolvable from the web service's region, expand to Oregon public domain.
                if (host != null && !host.contains(".") && host.startsWith("dpg-")) {
                    try {
                        InetAddress.getByName(host);
                        log.info("Internal Render DB host resolved successfully: {}", host);
                    } catch (Exception e) {
                        String publicHost = host + ".oregon-postgres.render.com";
                        log.warn("Internal host '{}' not resolvable from current region. Expanding to public host: {}", host, publicHost);
                        host = publicHost;
                    }
                }

                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = (uri.getPath() != null && !uri.getPath().isEmpty()) ? uri.getPath() : "/flashsaledb";

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                    jdbcUrl += "?" + uri.getQuery();
                }
            } catch (Exception e) {
                log.warn("Failed to parse PostgreSQL URI '{}': {}. Falling back to default format.", cleanUrl, e.getMessage());
                jdbcUrl = "jdbc:" + cleanUrl;
            }
        } else {
            jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
        }

        // Ensure SSL is required for remote Render PostgreSQL connections
        if (jdbcUrl.contains(".render.com") && !jdbcUrl.contains("sslmode=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        log.info("Sanitized JDBC URL for PostgreSQL: {}", jdbcUrl);
        log.info("Configured Database Username: {}", username);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Conservative connection pooling for cloud free tiers (e.g. Render connection caps)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000); // 20s timeout
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1800000);
        config.setPoolName("FlashSaleHikariPool");

        // Crucial: Prevent initial pool failure from killing the Spring Boot application container
        // Allows Tomcat to bind port 10000 immediately, letting Render healthchecks pass with 200 OK
        config.setInitializationFailTimeout(-1);

        return new HikariDataSource(config);
    }
}
