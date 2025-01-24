package com.trihydro.rsudatacontroller;

import com.trihydro.library.helpers.Utility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.helpers.JavaMailSenderImplProvider;
import com.trihydro.library.helpers.SQLNullHandler;
import com.trihydro.rsudatacontroller.config.BasicConfiguration;

@SpringBootApplication
@Import({ Utility.class, DbInteractions.class, SQLNullHandler.class, EmailHelper.class, JavaMailSenderImplProvider.class, })
@EnableConfigurationProperties(BasicConfiguration.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}