package com.flashsale.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Normalizes cloud PostgreSQL connection strings (e.g. Render / Railway / Supabase).
 * Standardizes raw "postgresql://user:password@host:port/dbname" into:
 * - URL: "jdbc:postgresql://host:port/dbname"
 * - Username: "user"
 * - Password: "password"
 */
@Configuration
public class DatabaseConfig implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSourceProperties properties) {
            String url = properties.getUrl();
            if (url != null) {
                normalizePostgresUrl(properties, url);
            }
        }
        return bean;
    }

    private void normalizePostgresUrl(DataSourceProperties properties, String url) {
        try {
            String cleanUrl = url.trim();
            if (cleanUrl.startsWith("jdbc:")) {
                cleanUrl = cleanUrl.substring(5);
            }

            if (cleanUrl.startsWith("postgresql://") || cleanUrl.startsWith("postgres://")) {
                URI uri = URI.create(cleanUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isEmpty()) {
                    String[] parts = userInfo.split(":", 2);
                    if (properties.getUsername() == null || properties.getUsername().isEmpty() || "postgres".equals(properties.getUsername())) {
                        properties.setUsername(parts[0]);
                    }
                    if (parts.length > 1 && (properties.getPassword() == null || properties.getPassword().isEmpty() || "postgres".equals(properties.getPassword()))) {
                        properties.setPassword(parts[1]);
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath(); // includes leading '/'
                String query = uri.getQuery();

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(host);
                if (port > 0) {
                    jdbcUrl.append(":").append(port);
                } else {
                    jdbcUrl.append(":5432");
                }
                if (path != null && !path.isEmpty()) {
                    jdbcUrl.append(path);
                }
                if (query != null && !query.isEmpty()) {
                    jdbcUrl.append("?").append(query);
                }

                log.info("Transformed cloud PostgreSQL URL to valid JDBC URL: {}", jdbcUrl);
                properties.setUrl(jdbcUrl.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to parse PostgreSQL connection string '{}': {}", url, e.getMessage());
            if (!url.startsWith("jdbc:")) {
                properties.setUrl("jdbc:" + url);
            }
        }
    }
}
