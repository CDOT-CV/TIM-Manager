package com.trihydro.rsudatacontroller.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.trihydro.library.model.DbInteractionsProps;
import com.trihydro.library.model.EmailProps;

@ConfigurationProperties
public class BasicConfiguration implements DbInteractionsProps, EmailProps {
    private int snmpRetries;
    private int snmpTimeoutSeconds;
    private String snmpAuthProtocol;
    private String snmpSecurityLevel;

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

    public int getSnmpRetries() {
        return snmpRetries;
    }

    public void setSnmpRetries(int snmpRetries) {
        this.snmpRetries = snmpRetries;
    }

    public int getSnmpTimeoutSeconds() {
        return snmpTimeoutSeconds;
    }

    public void setSnmpTimeoutSeconds(int snmpTimeoutSeconds) {
        this.snmpTimeoutSeconds = snmpTimeoutSeconds;
    }

    public String getSnmpAuthProtocol() {
        return snmpAuthProtocol;
    }

    public void setSnmpAuthProtocol(String snmpAuthProtocol) {
        this.snmpAuthProtocol = snmpAuthProtocol;
    }

    public String getSnmpSecurityLevel() {
        return snmpSecurityLevel;
    }
    
    public void setSnmpSecurityLevel(String snmpSecurityLevel) {
        this.snmpSecurityLevel = snmpSecurityLevel;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String[] getAlertAddresses() {
        return alertAddresses;
    }

    public void setAlertAddresses(String[] alertAddresses) {
        this.alertAddresses = alertAddresses;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getMailHost() {
        return mailHost;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public int getMailPort() {
        return mailPort;
    }

    public void setMailPort(int mailPort) {
        this.mailPort = mailPort;
    }
}