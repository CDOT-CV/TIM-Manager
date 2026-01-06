package com.trihydro.cvdatacontroller.model;

import com.trihydro.library.helpers.GISConnector;
import com.trihydro.library.helpers.DbInteractions;
import com.trihydro.library.helpers.EmailHelper;
import com.trihydro.library.helpers.JavaMailSenderImplProvider;
import com.trihydro.library.helpers.SQLNullHandler;
import com.trihydro.library.helpers.Utility;
import com.trihydro.library.model.DbInteractionsProps;
import com.trihydro.library.model.EmailProps;
import com.trihydro.library.service.RestTemplateProvider;
import com.trihydro.library.tables.LoggingTables;
import com.trihydro.library.tables.TimDbTables;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("config")
@Data
@Getter
@Setter
@Import({ TimDbTables.class, SQLNullHandler.class, Utility.class, EmailHelper.class,
    JavaMailSenderImplProvider.class, LoggingTables.class, DbInteractions.class,
    GISConnector.class, RestTemplateProvider.class })
public class DataControllerConfigProperties implements DbInteractionsProps, EmailProps {
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;

    private int maximumPoolSize;
    private int connectionTimeout;

    private String[] alertAddresses;
    private String fromEmail;
    private String environmentName;
    private String mailHost;
    private int mailPort;

    private String milepostProvider;

}