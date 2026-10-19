package com.tecnotree.eia.processed.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "stored-procedures")
public class StoredProcedureConfig {
    private String callableStatementString;
    private List<String> args;

    public void setCallableStatementString(String callableStatementString) {
        this.callableStatementString = callableStatementString;
    }

    public String getCallableStatementString() {
        return callableStatementString;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }
}
