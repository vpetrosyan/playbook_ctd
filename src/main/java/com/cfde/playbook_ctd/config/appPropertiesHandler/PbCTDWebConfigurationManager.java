package com.cfde.playbook_ctd.config.appPropertiesHandler;

public interface PbCTDWebConfigurationManager { ;
    String getPropertyByNameAsString(String propertyName);
    Integer getPropertyByNameAsInt(String propertyName);
    String[] getPropertyByNameAsArray(String propertyName);
}
