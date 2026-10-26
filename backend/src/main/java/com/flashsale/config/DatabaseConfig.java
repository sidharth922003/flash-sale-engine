package com.flashsale.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Normalizes cloud PostgreSQL connection strings (e.g. Render / Railway).
 * Cloud platforms inject "postgresql://...", while JDBC requires "jdbc:postgresql://".
 */
@Configuration
public class DatabaseConfig implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSourceProperties properties) {
            String url = properties.getUrl();
            if (url != null && !url.startsWith("jdbc:")) {
                if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
                    properties.setUrl("jdbc:" + url);
                }
            }
        }
        return bean;
    }
}
