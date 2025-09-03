package com.saipbuilds.kpaymentpipeline.Database.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    @Primary
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.postgresql.Driver");

        // --- This is the critical part ---
        // We are using the IP address 127.0.0.1 to be more explicit
        // and avoid potential conflicts with other services running on 'localhost'.
        dataSourceBuilder.url("jdbc:postgresql://127.0.0.1:5432/paymentdb??currentSchema=public&stringtype=unspecified&options=-c%20TimeZone=Asia/Kolkata");
//CHECK DURING DOCKERIZING
        dataSourceBuilder.username("postgres");
        dataSourceBuilder.password("postgres");
        return dataSourceBuilder.build();
    }
}
