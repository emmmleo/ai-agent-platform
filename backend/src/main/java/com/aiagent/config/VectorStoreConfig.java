package com.aiagent.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PGVector 数据源配置
 */
@Configuration
@EnableConfigurationProperties(VectorStoreProperties.class)
@ConditionalOnProperty(prefix = "vectorstore", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VectorStoreConfig {

    @Bean(name = "vectorDataSource")
    public DataSource vectorDataSource(VectorStoreProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setMaximumPoolSize(5);
        config.setPoolName("vectorstore-pool");
        return new HikariDataSource(config);
    }

    @Bean(name = "vectorJdbcTemplate")
    public JdbcTemplate vectorJdbcTemplate(@Qualifier("vectorDataSource") DataSource vectorDataSource) {
        return new JdbcTemplate(vectorDataSource);
    }
}
