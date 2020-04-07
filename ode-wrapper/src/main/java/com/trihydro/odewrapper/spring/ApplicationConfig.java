package com.trihydro.odewrapper.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = { "com.trihydro.odewrapper.controller", "com.trihydro.library.helpers",
        "com.trihydro.library.service", "com.trihydro.library.tables" })
public class ApplicationConfig implements WebMvcConfigurer {
    public ApplicationConfig() {
        super();
    }
}