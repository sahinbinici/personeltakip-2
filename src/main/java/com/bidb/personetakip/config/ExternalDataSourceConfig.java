package com.bidb.personetakip.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for external read-only database connection.
 * This database contains personnel master data.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.bidb.personetakip.repository.external",
    entityManagerFactoryRef = "externalEntityManagerFactory",
    transactionManagerRef = "externalTransactionManager"
)
public class ExternalDataSourceConfig {
    
    @Value("${external.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;
    
    @Value("${external.datasource.url:#{null}}")
    private String jdbcUrl;
    
    @Value("${external.datasource.jdbc-url:#{null}}")
    private String jdbcUrlAlt;
    
    @Value("${external.datasource.username:#{null}}")
    private String username;
    
    @Value("${external.datasource.password:#{null}}")
    private String password;
    
    @Value("${external.datasource.driver-class-name:#{null}}")
    private String driverClassName;
    
    /**
     * External datasource bean (read-only)
     * This datasource is configured with read-only credentials and should only be used
     * for querying personnel master data from the external database.
     */
    @Bean(name = "externalDataSource")
    @ConfigurationProperties(prefix = "external.datasource")
    public DataSource externalDataSource() {
        // Use either url or jdbc-url property
        String url = jdbcUrl != null ? jdbcUrl : jdbcUrlAlt;
        
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("External database URL is not configured. Please set external.datasource.url property.");
        }
        
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        
        // Configure HikariCP settings
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setReadOnly(true);
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setLeakDetectionThreshold(60000);
        
        // Add connection validation
        dataSource.setInitializationFailTimeout(5000);
        
        return dataSource;
    }
    
    /**
     * Entity manager factory for external database
     */
    @Bean(name = "externalEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean externalEntityManagerFactory(
            @Qualifier("externalDataSource") DataSource dataSource) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.bidb.personetakip.model");
        em.setPersistenceUnitName("external");
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.MYSQL);
        vendorAdapter.setGenerateDdl(false);
        em.setJpaVendorAdapter(vendorAdapter);
        
        Map<String, Object> properties = new HashMap<>();
        // Use none for production
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.jdbc.time_zone", "UTC");
        // Read-only connection with READ_COMMITTED isolation
        properties.put("hibernate.connection.isolation", "2");
        // External DB is read-only, don't disable autocommit
        properties.put("hibernate.connection.provider_disables_autocommit", "false");
        
        em.setJpaPropertyMap(properties);
        
        return em;
    }
    
    /**
     * Transaction manager for external database (read-only operations)
     */
    @Bean(name = "externalTransactionManager")
    public PlatformTransactionManager externalTransactionManager(
            @Qualifier("externalEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        // Set timeout for transactions
        transactionManager.setDefaultTimeout(30);
        return transactionManager;
    }
}