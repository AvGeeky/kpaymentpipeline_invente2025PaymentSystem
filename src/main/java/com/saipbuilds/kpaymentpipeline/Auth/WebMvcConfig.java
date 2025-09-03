package com.saipbuilds.kpaymentpipeline.Auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final HmacAuthInterceptor hmacAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply the HMAC interceptor only to the /api/payment path
        registry.addInterceptor(hmacAuthInterceptor)
                .addPathPatterns("/api/**");
    }
}
